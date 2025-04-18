package dev.kevalkanpariya.aurajournal.ui.home.state

import kotlin.time.Duration

data class RecordingState(
    val recordingTime: Duration = Duration.ZERO,
    val state: HomeEvent.AudioRecorder = HomeEvent.AudioRecorder.Idle
)
