package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NumberedList(
    items: List<String>,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    numberWidth: Dp = 28.dp,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { i, text ->
            Row {
                Text("${i + 1}.", modifier = Modifier.width(numberWidth))
                Text(text, modifier = Modifier.weight(1f))
            }
        }
    }
}
