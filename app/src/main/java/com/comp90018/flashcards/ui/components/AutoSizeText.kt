package com.comp90018.flashcards.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text that shrinks until it fits the space it is given. Hidden while it is still measuring,
 * so the shrinking never flickers on screen.
 *
 * The text needs a bounded height to shrink against (a fixed height, a weight, or fillMaxSize
 * inside a bounded parent). In an unbounded container such as a scrolling column it never shrinks.
 *
 * [style] sets the starting (largest) size, and [minFontSize] is the floor.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    textAlign: TextAlign = TextAlign.Center,
    minFontSize: TextUnit = 14.sp,
) {
    var fontSize by remember(text, style.fontSize) { mutableStateOf(style.fontSize) }
    var readyToDraw by remember(text, style.fontSize) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f),
        textAlign = textAlign,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowHeight && fontSize > minFontSize) {
                fontSize *= 0.9f
            } else {
                readyToDraw = true
            }
        },
    )
}
