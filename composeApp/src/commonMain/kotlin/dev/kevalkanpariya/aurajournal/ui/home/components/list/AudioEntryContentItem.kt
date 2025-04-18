package dev.kevalkanpariya.aurajournal.ui.home.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.kevalkanpariya.aurajournal.data.model.Audio
import dev.kevalkanpariya.aurajournal.ui.components.player.AudioPlayer
import dev.kevalkanpariya.aurajournal.ui.home.components.chips.TopicChip
import dev.kevalkanpariya.aurajournal.ui.home.state.HomeEvent
import dev.kevalkanpariya.aurajournal.ui.theme.spacing
import dev.kevalkanpariya.aurajournal.util.toHourMinuteString
import kotlin.time.Duration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioEntryContentItem(
    modifier: Modifier = Modifier,
    recording: Audio,
    currentPosition: Duration,
    onAudioPlayerEvent: (HomeEvent.AudioPlayer) -> Unit,
    onTopicSelect: (HomeEvent.Chip.TopicSelect) -> Unit,
    isPlaying: Boolean,
) {

    val createdTime = remember(recording.createdDateInMillis) {
        recording.createdDateInMillis.toHourMinuteString()
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = MaterialTheme.spacing.extraSmall,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {

        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Text(
                    text = recording.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = createdTime,
                    modifier = Modifier.widthIn(min = MaterialTheme.spacing.medium),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AudioPlayer(
                modifier = Modifier.fillMaxWidth(),
                amplitudeData = recording.amplitudeData,
                isPlaying = isPlaying,
                emotionType = recording.emotionType,
                currentPosition = currentPosition,
                totalDuration = recording.duration,
                onPlayPauseClick = {
                    if (isPlaying) onAudioPlayerEvent(HomeEvent.AudioPlayer.Pause(recording.id))
                    else onAudioPlayerEvent(HomeEvent.AudioPlayer.Play(recording.id))
                },
                onSeek = {}
            )

            if (recording.description.isNotEmpty()) ExpandableText(text = recording.description)

            if (recording.topics.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recording.topics.forEach { topic ->
                        TopicChip(
                            topic = topic.value,
                            onClick = { onTopicSelect(HomeEvent.Chip.TopicSelect(topic)) }
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.small))
                    }
                }
            }
        }
    }
}