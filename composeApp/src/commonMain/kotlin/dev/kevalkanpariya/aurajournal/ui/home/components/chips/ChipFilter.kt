package dev.kevalkanpariya.aurajournal.ui.home.components.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import aurajournal.composeapp.generated.resources.Res
import aurajournal.composeapp.generated.resources.chip_clear_selection_cd
import dev.kevalkanpariya.aurajournal.ui.theme.spacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChipFilter(
    modifier: Modifier = Modifier,
    filterOption: FilterOption,
    selectedOptions: List<FilterOption.Options>,
    shouldShowIcon: Boolean,
    onOptionSelected: (Int) -> Unit,
    onReset: () -> Unit,
) {

    var expanded by remember { mutableStateOf(false) }
    val chipBorderColor =
        if (selectedOptions.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor =
        if (selectedOptions.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else Color.Transparent

    Box(modifier = modifier) {

        FilterChip(
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(width = 1.dp, color = chipBorderColor),
            colors = FilterChipDefaults.filterChipColors().copy(
                containerColor = backgroundColor,
                selectedContainerColor = backgroundColor,
            ),
            selected = selectedOptions.isNotEmpty(),
            onClick = { expanded = true },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val displayText = when {
                        selectedOptions.isEmpty() -> filterOption.title
                        selectedOptions.size <= 2 -> selectedOptions.joinToString { it.text }
                        else -> {
                            val sortedOptions = selectedOptions.sortedBy { it.text }
                            "${sortedOptions[0].text}, ${sortedOptions[1].text} +${selectedOptions.size - 2}"
                        }
                    }

                    if (shouldShowIcon) {
                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            horizontalArrangement = Arrangement.spacedBy((-4).dp) // Negative spacing pulls icons together
                        ) {
                            selectedOptions.fastForEach { option ->
                                Image(
                                    painter = painterResource(option.icon),
                                    contentDescription = option.text,
                                )
                            }
                        }
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    if (selectedOptions.isNotEmpty()) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(MaterialTheme.spacing.medium)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(Res.string.chip_clear_selection_cd),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }
            },
        )

        if (expanded) {
            val density = LocalDensity.current
            val verticalOffset = remember {
                with(density) {
                    (FilterChipDefaults.Height + 16.dp).toPx().toInt()
                }
            }

            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                offset = IntOffset(0, verticalOffset)
            ) {
                Surface(
                    modifier = Modifier
                        .width(MaterialTheme.spacing.mobileMaxWidthSize)
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onPrimary)
                        .shadow(elevation = MaterialTheme.spacing.small),
                ) {

                    LazyColumn {
                        items(filterOption.options) { option ->
                            val isSelected = selectedOptions.contains(option)

                            DropdownMenuItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MaterialTheme.spacing.small,
                                        vertical = 1.dp
                                    )
                                    .background(
                                        color = if (isSelected) Color(0x475D92).copy(alpha = 0.05f)
                                        else Color.Transparent,
                                        shape = MaterialTheme.shapes.medium,
                                    ),
                                onClick = { onOptionSelected(option.id) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(option.icon),
                                        contentDescription = option.text,
                                        tint = Color.Unspecified,
                                    )
                                },
                                text = {
                                    Text(
                                        text = option.text,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}