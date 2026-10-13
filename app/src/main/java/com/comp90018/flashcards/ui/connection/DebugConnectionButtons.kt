package com.comp90018.flashcards.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * DEBUG ONLY: buttons that fake the link going wrong, for trying the reconnecting, lost connection
 * and other team left screens on an emulator. Shows nothing unless emulator mode is ticked.
 */
@Composable
fun DebugConnectionButtons(
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val emulatorMode by viewModel.emulatorMode.collectAsState()
    if (emulatorMode) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::dropConnection) {
                Text("DEBUG: drop connection", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = viewModel::peerLeft) {
                Text("DEBUG: other team left", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
