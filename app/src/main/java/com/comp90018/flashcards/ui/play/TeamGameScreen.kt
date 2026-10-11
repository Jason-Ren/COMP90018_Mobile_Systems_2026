package com.comp90018.flashcards.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.ui.connection.ConnectionOverlay

/**
 * The team mode game screen, locked to landscape. It plays like Duo, then compares the two teams' scores.
 */
@Composable
fun TeamGameScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamGameViewModel = hiltViewModel(),
) {
    LockLandscape()
    val state by viewModel.uiState.collectAsState()
    var showLeaveConfirm by remember { mutableStateOf(false) }
    val leave = {
        viewModel.leave()
        onNavigateBack()
    }

    // the setup is gone if the app was restarted mid game, so there is nothing to play
    LaunchedEffect(Unit) { if (!viewModel.hasGame) leave() }
    BackHandler { showLeaveConfirm = true }

    Surface(modifier = modifier.fillMaxSize()) {
        val round = state.round
        when (round.phase) {
            DuoPhase.READY -> ReadyContent(round, viewModel::start)
            DuoPhase.FRONT -> FrontContent(round, viewModel::reveal)
            DuoPhase.BACK -> BackContent(round, viewModel::onGesture)
            DuoPhase.FINISHED -> ResultContent(state, onFinish = leave)
        }
    }

    ConnectionOverlay(state.connection, onReconnect = viewModel::reconnect, onEnd = leave)

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave the game?") },
            text = { Text("The other team will be disconnected") },
            confirmButton = { Button(onClick = leave) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Stay") } },
        )
    }
}

/** Both scores side by side, who won, and the cards this team missed. */
@Composable
private fun ResultContent(
    state: TeamGameUiState,
    onFinish: () -> Unit,
) {
    val round = state.round
    val opponent = state.opponentCorrect
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = headline(round.correctCount, opponent), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("You: ${round.correctCount} / ${round.totalCount}", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Other team: ${opponent ?: "…"} / ${round.totalCount}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (round.missed.isNotEmpty()) {
            MissedCards(round.missed, modifier = Modifier.weight(1f))
        } else {
            PerfectRound(hasCards = round.totalCount > 0, modifier = Modifier.weight(1f))
        }
        Button(onClick = onFinish) { Text("Done") }
    }
}

// the other team may still be playing, so say so until their score arrives
private fun headline(
    mine: Int,
    theirs: Int?,
): String =
    when {
        theirs == null -> "Waiting for the other team to finish"
        mine > theirs -> "You win!"
        mine < theirs -> "The other team wins"
        else -> "It's a tie"
    }
