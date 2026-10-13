package com.comp90018.flashcards.ui.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import com.comp90018.flashcards.R
import com.comp90018.flashcards.ui.components.NumberedList

@Composable
fun MultiGameInstructions(modifier: Modifier = Modifier) {
    NumberedList(
        items = stringArrayResource(R.array.multi_game_steps).toList(),
        modifier = modifier,
    )
}
