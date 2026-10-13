package com.comp90018.flashcards.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.comp90018.flashcards.R

@Composable
fun DuoPlayInstructions(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.duo_mode_1))
        Text(stringResource(R.string.duo_mode_2))
        Text(stringResource(R.string.duo_mode_3))
        Text(stringResource(R.string.duo_mode_4))
        Text(stringResource(R.string.duo_mode_5))
        Text(stringResource(R.string.duo_mode_6))
    }
}
