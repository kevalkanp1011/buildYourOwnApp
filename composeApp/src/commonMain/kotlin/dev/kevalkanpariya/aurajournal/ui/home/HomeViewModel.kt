package dev.kevalkanpariya.aurajournal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import aurajournal.composeapp.generated.resources.Res
import aurajournal.composeapp.generated.resources.ic_excited
import aurajournal.composeapp.generated.resources.ic_hashtag
import aurajournal.composeapp.generated.resources.ic_neutral
import aurajournal.composeapp.generated.resources.ic_peaceful
import aurajournal.composeapp.generated.resources.ic_sad
import aurajournal.composeapp.generated.resources.ic_stressed
import dev.kevalkanpariya.aurajournal.data.model.Audio
import dev.kevalkanpariya.aurajournal.data.model.AudioPath
import dev.kevalkanpariya.aurajournal.data.repository.AudioRepository
import dev.kevalkanpariya.aurajournal.platform.audioplayer.AudioPlayer
import dev.kevalkanpariya.aurajournal.platform.audiorecord.AudioRecorder
import dev.kevalkanpariya.aurajournal.platform.permission.Permission
import dev.kevalkanpariya.aurajournal.platform.permission.PermissionState
import dev.kevalkanpariya.aurajournal.platform.permission.service.PermissionService
import dev.kevalkanpariya.aurajournal.ui.home.components.chips.FilterOption
import dev.kevalkanpariya.aurajournal.ui.home.state.AudioDragRecordState
import dev.kevalkanpariya.aurajournal.ui.home.state.HomeEvent
import dev.kevalkanpariya.aurajournal.ui.home.state.HomeState
import dev.kevalkanpariya.aurajournal.ui.home.state.PlaybackState
import dev.kevalkanpariya.aurajournal.ui.home.state.RecordingState
import dev.kevalkanpariya.aurajournal.ui.home.state.Recordings
import dev.kevalkanpariya.aurajournal.ui.home.state.Topic
import dev.kevalkanpariya.aurajournal.util.toEmotionType
import dev.kevalkanpariya.aurajournal.util.toRecordingList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class HomeViewModel(
    private val audioPlayer: AudioPlayer, // TODO Use case
    private val audioRecorder: AudioRecorder,
    private val permissionService: PermissionService,
    private val repository: AudioRepository,
) : ViewModel() {

    private val _homeState = MutableStateFlow(HomeState())
    val homeState = _homeState
        .onStart { initHomeState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeState()
        )

    private val _permissionStatus = MutableStateFlow(PermissionState.NOT_DETERMINED)
    val permissionStatus: StateFlow<PermissionState> = _permissionStatus
        .onStart {}
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PermissionState.NOT_DETERMINED
        )

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.NotPlaying)
    val playbackState = _playbackState.asStateFlow()

    private val _audioDragRecordState = MutableStateFlow(AudioDragRecordState())
    val audioDragRecordState = _audioDragRecordState.asStateFlow()

    private var hasHandledWidgetAction = false
    private var originalRecordings: List<Audio> = emptyList()

    private var playbackJob: Job? = null
    private var recordingJob: Job? = null

    init {
        audioPlayer.setOnPlaybackCompleteListener {
            onStop()
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Chip.MoodChip -> onMoodChipSelect(event.id)
            is HomeEvent.Chip.TopicChip -> onTopicChipSelect(event.id)
            is HomeEvent.Chip.TopicSelect -> onTopicChipListSelect(event.topic)
            HomeEvent.Chip.MoodReset -> onMoodChipReset()
            HomeEvent.Chip.TopicReset -> onTopicChipReset()
            HomeEvent.FabBottomSheet.FabClick -> viewModelScope.launch { checkPermission() }
            HomeEvent.FabBottomSheet.SheetShow -> onFabBottomSheetShow()
            HomeEvent.FabBottomSheet.SheetHide -> onFabBottomSheetHide()
            HomeEvent.Permission.Close -> _permissionStatus.update { PermissionState.NOT_DETERMINED }
            is HomeEvent.Permission.Settings -> openSettingsPage(event.type)
            HomeEvent.AudioRecorder.Idle -> onAudioRecordIdle()
            HomeEvent.AudioRecorder.Record -> onAudioRecordStart()
            HomeEvent.AudioRecorder.Pause -> onAudioRecordPause()
            HomeEvent.AudioRecorder.Cancel -> onAudioRecordCancel()
            HomeEvent.AudioRecorder.Done -> onAudioRecordDone()
            HomeEvent.AudioDragRecorder.Record -> onAudioDragRecordStart()
            is HomeEvent.AudioDragRecorder.Drag -> onAudioDragRecordDragUpdate(
                event.offsetX,
                event.isInCancelZone
            )

            HomeEvent.AudioDragRecorder.Cancel -> onAudioDragRecordCancel()
            HomeEvent.AudioDragRecorder.Done -> onAudioDragRecordDone()
            is HomeEvent.AudioPlayer.Play -> onPlay(event.id)
            is HomeEvent.AudioPlayer.Pause -> onPause(event.id)
            HomeEvent.Reload -> onReload()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        playbackJob?.cancel()
        hasHandledWidgetAction = false
    }

    private fun initHomeState() {
        val moodChip = FilterOption(
            title = "All Moods",
            options = listOf(
                FilterOption.Options(id = 1, text = "Excited", icon = Res.drawable.ic_excited),
                FilterOption.Options(id = 2, text = "Peaceful", icon = Res.drawable.ic_peaceful),
                FilterOption.Options(id = 3, text = "Neutral", icon = Res.drawable.ic_neutral),
                FilterOption.Options(id = 4, text = "Sad", icon = Res.drawable.ic_sad),
                FilterOption.Options(id = 5, text = "Stressed", icon = Res.drawable.ic_stressed),
            )
        )

        viewModelScope.launch {
            val allRecordings: List<Audio> = repository.getAllRecordings()
            originalRecordings = allRecordings

            // Extract unique topics and convert to options
            val uniqueTopics = allRecordings
                .flatMap { it.topics }
                .distinct()
                .map { topic ->
                    FilterOption.Options(
                        id = topic.value.hashCode(),
                        text = topic.value,
                        icon = Res.drawable.ic_hashtag,
                        isSelected = false
                    )
                }

            _homeState.update { state ->
                state.copy(
                    recordings = allRecordings.toRecordingList(),
                    moodChip = moodChip,
                    topicsChip = FilterOption(
                        title = "All Topics",
                        options = uniqueTopics
                    ),
                )
            }
        }
    }

    private fun onMoodChipSelect(id: Int) {
        val moodChip = _homeState.value.moodChip
        require(moodChip != null) { "Mood chip can not be null." }

        viewModelScope.launch {

            val options: List<FilterOption.Options> = moodChip.options.map {
                if (it.id == id) it.copy(isSelected = it.isSelected.not())
                else it
            }

            val selectedEmotions = options.filter { it.isSelected }.map { it.toEmotionType() }

            val filteredRecordings =
                if (selectedEmotions.isEmpty()) originalRecordings.toRecordingList()
                else originalRecordings.filter { it.emotionType in selectedEmotions }
                    .toRecordingList()

            _homeState.update { state ->
                state.copy(
                    moodChip = state.moodChip?.copy(options = options),
                    recordings = filteredRecordings
                )
            }
        }
    }

    private fun onTopicChipSelect(id: Int) {
        val topicsChip = _homeState.value.topicsChip
        require(topicsChip != null) { "Topic chip can not be null." }

        viewModelScope.launch {
            val options = topicsChip.options.map {
                if (it.id == id) it.copy(isSelected = it.isSelected.not())
                else it
            }.sortedBy { it.text }

            val selectedTopics = options
                .filter { it.isSelected }
                .map { Topic(it.text) }

            val filteredRecordings = if (selectedTopics.isEmpty()) {
                originalRecordings.toRecordingList()
            } else {
                originalRecordings
                    .filter { audio ->
                        audio.topics.any { it in selectedTopics }
                    }
                    .toRecordingList()
            }

            _homeState.update { state ->
                state.copy(
                    topicsChip = state.topicsChip?.copy(options = options),
                    recordings = filteredRecordings
                )
            }
        }
    }

    private fun onTopicChipListSelect(topic: Topic) {
        val id = topic.value.hashCode()
        onTopicChipSelect(id)
    }

    private fun onMoodChipReset() {
        val moodChip = _homeState.value.moodChip
        require(moodChip != null) { "Mood chip can not be null." }

        val options = moodChip.options.map { it.copy(isSelected = false) }
        _homeState.update { state ->
            state.copy(
                moodChip = state.moodChip?.copy(options = options),
                recordings = originalRecordings.toRecordingList(),
            )
        }
    }

    private fun onTopicChipReset() {
        val topicsChip = _homeState.value.topicsChip
        require(topicsChip != null) { "Topic chip can not be null." }

        val options = topicsChip.options.map { it.copy(isSelected = false) }
        _homeState.update { state ->
            state.copy(
                topicsChip = state.topicsChip?.copy(options = options),
                recordings = originalRecordings.toRecordingList()
            )
        }
    }

    private fun onFabBottomSheetShow() {
        _homeState.update { state -> state.copy(fabBottomSheet = HomeEvent.FabBottomSheet.SheetShow) }
    }

    private fun onFabBottomSheetHide() {
        _homeState.update { state -> state.copy(fabBottomSheet = HomeEvent.FabBottomSheet.SheetHide) }
        viewModelScope.launch {
            if (audioRecorder.isRecording()) audioRecorder.discardRecording()
        }
    }

    private fun onAudioRecordIdle() {
        _recordingState.update { it.copy(state = HomeEvent.AudioRecorder.Idle) }
        onFabBottomSheetHide()
    }

    private fun onAudioRecordStart() {
        viewModelScope.launch {
            _recordingState.update { it.copy(state = HomeEvent.AudioRecorder.Record) }
            if (audioRecorder.isPaused()) {
                updateRecordingState()
                audioRecorder.resumeRecording()
            } else {
                val fileName = "recording_${Clock.System.now().toEpochMilliseconds()}.m4a"
                audioRecorder.startRecording(fileName)
                updateRecordingState()
            }
        }
    }

    private fun onAudioDragRecordStart() {
        viewModelScope.launch {
            _audioDragRecordState.update { it.copy(isDragging = true) }
            onAudioRecordStart()
        }
    }

    private fun onAudioDragRecordDragUpdate(offsetX: Float, isInCancelZone: Boolean) {
        Logger.i { "onAudioDragRecordDragUpdate isInCancelZone: $isInCancelZone" }
        _audioDragRecordState.update { state ->
            state.copy(
                isDragging = true,
                dragOffset = offsetX,
                isInCancelZone = isInCancelZone
            )
        }
    }

    private fun updateRecordingState() {
        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1.seconds)
                _recordingState.update { it.copy(recordingTime = it.recordingTime.plus(1.seconds)) }
            }
        }
    }

    private fun cancelRecordingJob() {
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun onAudioRecordPause() {
        if (audioRecorder.isRecording().not()) return
        viewModelScope.launch {
            audioRecorder.pauseRecording()
            cancelRecordingJob()
            _recordingState.update { it.copy(state = HomeEvent.AudioRecorder.Pause) }
        }
    }

    private fun onAudioRecordCancel() {
        viewModelScope.launch {
            audioRecorder.discardRecording()
        }
        cancelRecordingJob()
        _recordingState.update {
            it.copy(
                recordingTime = Duration.ZERO,
                state = HomeEvent.AudioRecorder.Cancel
            )
        }
        onFabBottomSheetHide()
    }

    private fun onAudioDragRecordCancel() {
        viewModelScope.launch {
            _audioDragRecordState.update { AudioDragRecordState() }
            onAudioRecordCancel()
        }
    }

    private fun onAudioRecordDone() {
        viewModelScope.launch {
            val (filePath, amplitudePath) = audioRecorder.stopRecording()
            Logger.i { "Audio recording done: $filePath" }
            cancelRecordingJob()
            _recordingState.update {
                it.copy(
                    recordingTime = Duration.ZERO,
                    state = HomeEvent.AudioRecorder.Done,
                )
            }
            _homeState.update {
                it.copy(
                    recordingPath = AudioPath(filePath),
                    amplitudePath = amplitudePath
                )
            }
        }
    }

    private fun onAudioDragRecordDone() {
        if (_audioDragRecordState.value.isInCancelZone) {
            onAudioDragRecordCancel()
            return
        }
        viewModelScope.launch {
            _audioDragRecordState.update { AudioDragRecordState() }
            onAudioRecordDone()
        }
    }

    private fun onPlay(id: Long) {
        playbackJob?.cancel()
        var audioPath: String? = ""
        _homeState.value.recordings.map { recording ->
            when (recording) {
                is Recordings.Date -> recording
                is Recordings.Entry -> {
                    recording.recordings.forEach { audio ->
                        if (audio.id == id) audioPath = audio.path?.value
                    }
                }
            }
        }
        requireNotNull(audioPath) { "Audio path can not be null." }
        audioPlayer.play(audioPath!!)
        _playbackState.update { PlaybackState(playingId = id, position = Duration.ZERO) }
        startPositionUpdates(id)
    }

    private fun onPause(id: Long) {
        audioPlayer.stop()
        playbackJob?.cancel()
        _playbackState.update { PlaybackState.NotPlaying }
    }

    private fun onStop() {
        playbackJob?.cancel()
        _playbackState.update { PlaybackState.NotPlaying }
    }

    private fun startPositionUpdates(id: Long) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive) {
                if (_playbackState.value.playingId == id && !_playbackState.value.isSeeking) {
                    val currentPosition = audioPlayer.getCurrentPosition()
                    Logger.i { "VM Position : $currentPosition, $id" }
                    _playbackState.update { it.copy(position = currentPosition) }
                }
                delay(100) // Update every 100ms
            }
        }
    }

    private fun onSeek(id: Long, position: Float) {
        viewModelScope.launch {
            _playbackState.update { it.copy(isSeeking = true) }
            audioPlayer.seekTo(position.toLong().milliseconds)
            _playbackState.update {
                it.copy(
                    position = audioPlayer.getCurrentPosition(),
                    isSeeking = false
                )
            }
        }
    }

    private suspend fun checkPermission() {
        val permissionState = permissionService.checkPermission(Permission.MICROPHONE)
        Logger.i { "Permission state: $permissionState" }

        when (permissionState) {
            PermissionState.NOT_DETERMINED -> {
                observePermissionChanges()
                permissionService.providePermission(Permission.MICROPHONE)
            }

            PermissionState.DENIED -> _permissionStatus.update { PermissionState.DENIED }
            PermissionState.GRANTED -> {
                _permissionStatus.update { PermissionState.GRANTED }
                showSheetAndStartRecording()
            }
        }
    }

    private fun observePermissionChanges() {
        viewModelScope.launch {
            permissionService.checkPermissionFlow(Permission.MICROPHONE)
                .collect { state ->
                    Logger.i { "Permission state in flow: $state" }
                    _permissionStatus.update { state }
                    if (state == PermissionState.GRANTED) {
                        showSheetAndStartRecording()
                    }
                }
        }
    }

    private fun openSettingsPage(permission: Permission) {
        permissionService.openSettingsPage(permission)
    }

    fun handleWidgetAction() {
        if (!hasHandledWidgetAction) {
            onEvent(HomeEvent.FabBottomSheet.FabClick)
            hasHandledWidgetAction = true
        }
    }

    private fun onReload() {
        onFabBottomSheetHide()
        onAudioRecordIdle()
    }

    private fun showSheetAndStartRecording() {
        onFabBottomSheetShow()
        onAudioRecordStart()
    }
}
