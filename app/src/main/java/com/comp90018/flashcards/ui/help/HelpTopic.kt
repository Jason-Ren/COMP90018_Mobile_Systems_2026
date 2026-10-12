package com.comp90018.flashcards.ui.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.R
import com.comp90018.flashcards.ui.components.HelpTip

enum class HelpTopic(
    @StringRes val title: Int,
    val content: @Composable ColumnScope.() -> Unit,
) {
    // Play Instructions
    SoloPlayHelp(R.string.solo_mode_title, { SoloPlayInstructions() }),
    DuoPlayHelp(R.string.duo_mode_title, { DuoPlayInstructions() }),
    MultiPlayHelp(R.string.multi_mode_title, { MultiPlayInstructions() }),
}

@Composable
fun helpTip(
    topic: HelpTopic,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) = HelpTip(title = stringResource(topic.title), modifier = modifier, size = size) {
    topic.content(this)
}
