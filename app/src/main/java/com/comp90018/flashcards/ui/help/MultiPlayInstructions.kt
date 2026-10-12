package com.comp90018.flashcards.ui.help

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.comp90018.flashcards.R

@Composable
fun MultiPlayInstructions() {
    Text(stringResource(R.string.multi_mode_1))
    Text(stringResource(R.string.multi_mode_2))
    Text(stringResource(R.string.multi_mode_3))
    Text(stringResource(R.string.multi_mode_4))
    Text(stringResource(R.string.multi_mode_5))
}
