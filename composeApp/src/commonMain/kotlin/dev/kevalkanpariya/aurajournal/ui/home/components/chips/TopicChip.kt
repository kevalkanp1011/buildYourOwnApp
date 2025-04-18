package dev.kevalkanpariya.aurajournal.ui.home.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import aurajournal.composeapp.generated.resources.Res
import aurajournal.composeapp.generated.resources.common_cancel
import aurajournal.composeapp.generated.resources.hashtag_cd
import aurajournal.composeapp.generated.resources.ic_hashtag
import dev.kevalkanpariya.aurajournal.ui.theme.spacing
import dev.kevalkanpariya.aurajournal.util.noRippleClickable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopicChip(
    modifier: Modifier = Modifier,
    topic: String,
    shouldShowCancel: Boolean = false,
    onClick: () -> Unit = {},
    onCancel: () -> Unit = {},
) {

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.large,
            )
            .heightIn(min = FilterChipDefaults.Height)
            .padding(horizontal = MaterialTheme.spacing.small)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            painter = painterResource(Res.drawable.ic_hashtag),
            contentDescription = stringResource(Res.string.hashtag_cd),
            tint = Color.Unspecified,
        )

        Text(
            text = topic,
            modifier = Modifier.padding(end = MaterialTheme.spacing.extraSmall),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (shouldShowCancel.not()) return

        Icon(
            imageVector = Icons.Outlined.Close,
            modifier = Modifier
                .padding(end = MaterialTheme.spacing.extraSmall)
                .size(MaterialTheme.spacing.medium)
                .noRippleClickable { onCancel() },
            contentDescription = stringResource(Res.string.common_cancel),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )
    }
}