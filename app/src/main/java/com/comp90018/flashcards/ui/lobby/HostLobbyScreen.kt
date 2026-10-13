package com.comp90018.flashcards.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.ui.connection.ConnectionOverlay
import com.comp90018.flashcards.ui.deck.DeckSummaryViewModel

/**
 * The host's team mode lobby: host on top, guest slot below, Start and Cancel at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreen(
    onNavigateBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    lobbyViewModel: HostLobbyViewModel = hiltViewModel(),
    summaryViewModel: DeckSummaryViewModel = hiltViewModel(),
) {
    val state by lobbyViewModel.state.collectAsState()
    val summary by summaryViewModel.uiState.collectAsState()

    // start advertising once, the permission gate has already made sure we are allowed to
    LaunchedEffect(Unit) { lobbyViewModel.begin() }

    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val endSession = {
        lobbyViewModel.cancel()
        onNavigateBack()
    }

    // Back, the top-bar arrow and Cancel all do the same thing.
    fun requestLeave() {
        if (state.guest is GuestSlot.Joined) showCancelConfirm = true else endSession()
    }
    BackHandler { requestLeave() }

    Scaffold(
        modifier = modifier,
        topBar = { LobbyTopBar(title = "Team Mode", onBack = { requestLeave() }) },
    ) { padding ->
        LobbyLayout(
            deckName = summary.deckName.ifBlank { "Deck" },
            topSlot = {
                TeamSlotCard(
                    label = "My Team",
                    teamName = state.teamName,
                    canReroll = state.canReroll,
                    onReroll = lobbyViewModel::rerollName,
                    tint = TeamTint.Blue,
                )
            },
            bottomSlot = {
                GuestSlotContent(
                    guest = state.guest,
                    showDebugButton = state.showDebugButton,
                    onSimulateRequest = lobbyViewModel::debugSimulateRequest,
                    onAccept = lobbyViewModel::accept,
                    onDecline = lobbyViewModel::decline,
                    onRemove = { showRemoveConfirm = true },
                )
            },
            bottomActions = {
                HostActions(canStart = state.canStart, onStart = onStart, onCancel = { requestLeave() })
            },
            modifier = Modifier.padding(padding),
        )
    }

    ConnectionOverlay(state.connection, onReconnect = lobbyViewModel::reconnect, onEnd = endSession)

    if (showRemoveConfirm) {
        RemoveGuestDialog(
            name = (state.guest as? GuestSlot.Joined)?.teamName ?: "this team",
            onConfirm = lobbyViewModel::removeGuest,
            onDismiss = { showRemoveConfirm = false },
        )
    }

    if (showCancelConfirm) {
        EndSessionDialog(
            onConfirm = endSession,
            onDismiss = { showCancelConfirm = false },
        )
    }
}

@Composable
private fun GuestSlotContent(
    guest: GuestSlot,
    showDebugButton: Boolean,
    onSimulateRequest: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onRemove: () -> Unit,
) {
    when (guest) {
        GuestSlot.Empty -> {
            Column {
                WaitingSlot()
                if (showDebugButton) {
                    OutlinedButton(
                        onClick = onSimulateRequest,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("DEBUG: simulate join request") }
                }
            }
        }
        is GuestSlot.Pending ->
            PendingSlot(teamName = guest.teamName, onAccept = onAccept, onDecline = onDecline)
        is GuestSlot.Joined ->
            JoinedSlot(teamName = guest.teamName, onRemove = onRemove)
    }
}

@Composable
private fun HostActions(
    canStart: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Start", style = MaterialTheme.typography.titleLarge)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Cancel",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun RemoveGuestDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove $name?") },
        text = { Text("They will be disconnected and would need to ask to join again.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onConfirm()
            }) { Text("Remove") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}

@Composable
private fun EndSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End this session?") },
        text = { Text("The other team will be disconnected.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onConfirm()
            }) { Text("End session") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } },
    )
}
