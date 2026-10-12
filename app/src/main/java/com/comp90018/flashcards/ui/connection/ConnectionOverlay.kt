package com.comp90018.flashcards.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.comp90018.flashcards.data.p2p.ConnectionState

/**
 * Full screen cover for when the link to the other phone is not fine: reconnecting, lost,
 * the other team left, or nearby could not start. Shows nothing for every other state.
 */
@Composable
fun ConnectionOverlay(
    state: ConnectionState,
    onReconnect: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        // back does nothing while we are still trying
        is ConnectionState.Reconnecting ->
            OverlayScreen(onDismiss = {}, modifier = modifier) {
                CircularProgressIndicator()
                Text("Reconnecting…", style = MaterialTheme.typography.headlineSmall)
                Text("Trying to reach ${state.peerName}", textAlign = TextAlign.Center)
            }
        is ConnectionState.Lost ->
            OverlayScreen(onDismiss = onEnd, modifier = modifier) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp))
                Text("Lost connection", style = MaterialTheme.typography.headlineSmall)
                Text("Couldn't reach ${state.peerName}", textAlign = TextAlign.Center)
                Button(onClick = onReconnect, modifier = Modifier.fillMaxWidth()) { Text("Reconnect") }
                TextButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                    Text("End session", color = MaterialTheme.colorScheme.error)
                }
            }
        is ConnectionState.PeerEnded ->
            OverlayScreen(onDismiss = onEnd, modifier = modifier) {
                Text("Session ended", style = MaterialTheme.typography.headlineSmall)
                Text("The other team left", textAlign = TextAlign.Center)
                Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) { Text("OK") }
            }
        is ConnectionState.Failed ->
            OverlayScreen(onDismiss = onEnd, modifier = modifier) {
                // a reason is "what went wrong" on the first line and "what to do" on the next
                Text(
                    text = state.reason.substringBefore('\n'),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                val advice = state.reason.substringAfter('\n', "")
                if (advice.isNotEmpty()) Text(advice, textAlign = TextAlign.Center)
                Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        else -> Unit
    }
}

// a dialog window so this covers the whole screen without restructuring the screen under it
@Composable
private fun OverlayScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
        }
    }
}
