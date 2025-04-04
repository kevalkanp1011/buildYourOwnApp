package dev.kevalkanpariya.aurajournal.ui.home.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.ModalSheetProperties
import com.composables.core.Sheet
import com.composables.core.SheetDetent.Companion.Hidden
import com.composables.core.rememberModalBottomSheetState
import aurajournal.composeapp.generated.resources.Res
import aurajournal.composeapp.generated.resources.close_cd
import aurajournal.composeapp.generated.resources.play_cd
import aurajournal.composeapp.generated.resources.record_bottom_sheet_recording
import dev.kevalkanpariya.aurajournal.ui.home.components.fab.GradientFAB
import dev.kevalkanpariya.aurajournal.ui.home.state.HomeEvent
import dev.kevalkanpariya.aurajournal.ui.theme.spacing
import dev.kevalkanpariya.aurajournal.util.Peek
import dev.kevalkanpariya.aurajournal.util.formatDuration
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

@Composable
fun RecordBottomSheet(
    modifier: Modifier = Modifier,
    recordTime: Duration,
    fabState: HomeEvent.FabBottomSheet,
    audioEvent: HomeEvent.AudioRecorder,
    onAudioEvent: (HomeEvent.AudioRecorder) -> Unit,
) {

    val sheetState = rememberModalBottomSheetState(
        initialDetent = Hidden,
        detents = listOf(Hidden, Peek)
    )

    if (fabState == HomeEvent.FabBottomSheet.SheetShow) sheetState.currentDetent = Peek
    else sheetState.currentDetent = Hidden


    ModalBottomSheet(
        state = sheetState,
        properties = ModalSheetProperties().copy(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        onDismiss = { onAudioEvent(HomeEvent.AudioRecorder.Cancel) },
    ) {

        Sheet(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.large
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.spacing.bottomSheetMaxHeight)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    DragIndication(
                        modifier = Modifier
                            .padding(
                                top = MaterialTheme.spacing.medium,
                                bottom = MaterialTheme.spacing.large
                            )
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.large
                            )
                            .width(MaterialTheme.spacing.largeXL)
                            .height(MaterialTheme.spacing.extraSmall)
                    )

                    Text(
                        text = stringResource(Res.string.record_bottom_sheet_recording),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = recordTime.formatDuration(),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {

                        IconButton(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                )
                                .size(MaterialTheme.spacing.large2XL),
                            onClick = { onAudioEvent(HomeEvent.AudioRecorder.Cancel) },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.close_cd),
                                    tint = Color.Unspecified,
                                )
                            }
                        )

                        if (audioEvent == HomeEvent.AudioRecorder.Record) {
                            GradientFAB(
                                isPlaying = true,
                                onClick = { onAudioEvent(HomeEvent.AudioRecorder.Done) },
                            )
                        } else {
                            GradientFAB(
                                isPlaying = false,
                                onClick = { onAudioEvent(HomeEvent.AudioRecorder.Record) },
                            )
                        }

                        val pausePlayIcon =
                            if (audioEvent == HomeEvent.AudioRecorder.Record) Icons.Default.Pause else Icons.Default.Done
                        IconButton(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    shape = CircleShape
                                )
                                .size(MaterialTheme.spacing.large2XL),
                            onClick = {
                                if (audioEvent == HomeEvent.AudioRecorder.Pause)
                                    onAudioEvent(HomeEvent.AudioRecorder.Done)
                                else onAudioEvent(HomeEvent.AudioRecorder.Pause)
                            },
                            content = {
                                Icon(
                                    imageVector = pausePlayIcon,
                                    contentDescription = stringResource(Res.string.play_cd),
                                    tint = Color.Unspecified,
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
