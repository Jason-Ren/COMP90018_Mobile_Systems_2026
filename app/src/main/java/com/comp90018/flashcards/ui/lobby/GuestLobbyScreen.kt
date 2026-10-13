package com.comp90018.flashcards.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.ui.connection.ConnectionOverlay

/**
 * The guest's team mode lobby: host on top, guest ("My Team") below, waiting indicator and Leave at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestLobbyScreen(
    onNavigateBack: () -> Unit,
    onGameStart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinGameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val openGame by rememberUpdatedState(onGameStart)
    LaunchedEffect(state.hostStarted) {
        if (state.hostStarted) openGame()
    }

    var showLeaveConfirm by remember { mutableStateOf(false) }

    val leaveSession = {
        viewModel.leave()
        onNavigateBack()
    }

    fun requestLeave() {
        if (state.connection is ConnectionState.Connected) {
            showLeaveConfirm = true
        } else {
            leaveSession()
        }
    }

    BackHandler { requestLeave() }

    Scaffold(
        modifier = modifier,
        topBar = { LobbyTopBar(title = "Team Mode", onBack = { requestLeave() }) },
    ) { padding ->
        LobbyLayout(
            topSlot = {
                TeamSlotCard(
                    label = "Host Team",
                    teamName = state.hostTeamName.ifBlank { "Host" },
                    canReroll = false,
                    tint = TeamTint.Blue
                )
            },
            bottomSlot = {
                TeamSlotCard(
                    label = "My Team",
                    teamName = state.teamName,
                    canReroll = state.canReroll,
                    onReroll = viewModel::rerollName,
                    tint = TeamTint.Red
                )
            },
            bottomActions = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Waiting for the host to start…", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    TextButton(onClick = { requestLeave() }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Leave Game",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            modifier = Modifier.padding(padding),
        )
    }

    ConnectionOverlay(state = state.connection, onReconnect = viewModel::reconnect, onEnd = leaveSession)

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave game?") },
            text = { Text("You will be disconnected from the host.") },
            confirmButton = {
                Button(onClick = {
                    showLeaveConfirm = false
                    leaveSession()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Stay") }
            },
        )
    }
}
