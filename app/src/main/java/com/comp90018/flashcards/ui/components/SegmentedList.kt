package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Gap between items in a segmented list group. */
val SegmentedListGap = 2.dp

/** Extra space around non-list content in a LazyColumn that uses [SegmentedListGap] spacing. */
val SegmentedListSectionGap = 12.dp

/** Extra vertical padding added on top of the ListItem's own padding to make rows taller. */
private val SegmentedListItemVerticalPadding = 8.dp

private val OuterCorner = 16.dp
private val InnerCorner = 4.dp

/** M3 segmented list shape: large corners on the group's outer edges, small corners between items. */
fun segmentedListShape(
    index: Int,
    count: Int,
): Shape {
    val top = if (index == 0) OuterCorner else InnerCorner
    val bottom = if (index == count - 1) OuterCorner else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** A [ListItem] rendered as one segment of a contained list group. */
@Composable
fun SegmentedListItem(
    index: Int,
    count: Int,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val shape = segmentedListShape(index, count)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        ListItem(
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(vertical = SegmentedListItemVerticalPadding),
        )
    }
}
