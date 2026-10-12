package com.comp90018.flashcards.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.ui.connection.ConnectionOverlay
import com.comp90018.flashcards.ui.connection.DebugConnectionButtons

/**
 * The guest's side of team mode: list hosts nearby, ask to join, and once accepted display [GuestLobbyScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(
    onNavigateBack: () -> Unit,
    onGameStart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinGameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.begin() }

    val openGame by rememberUpdatedState(onGameStart)
    LaunchedEffect(state.hostStarted) { if (state.hostStarted) openGame() }

    // If already connected to host, show the GuestLobbyScreen
    val isJoined = state.connection is ConnectionState.Connected ||
        state.connection is ConnectionState.Reconnecting ||
        state.connection is ConnectionState.Lost

    if (isJoined) {
        GuestLobbyScreen(
            onNavigateBack = onNavigateBack,
            onGameStart = onGameStart,
            modifier = modifier,
            viewModel = viewModel,
        )
        return
    }

    // back, top bar arrow and Cancel all leave the same way
    val leave = {
        viewModel.leave()
        onNavigateBack()
    }
    BackHandler { leave() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Join game") },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        JoinBody(
            state = state,
            onReroll = viewModel::rerollName,
            onJoin = viewModel::join,
            onCancel = leave,
            modifier = Modifier.padding(padding),
        )
    }

    ConnectionOverlay(state = state.connection, onReconnect = viewModel::reconnect, onEnd = leave)
}

@Composable
private fun JoinBody(
    state: JoinUiState,
    onReroll: () -> Unit,
    onJoin: (DiscoveredHost) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Team",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = state.teamName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                IconButton(onClick = onReroll, enabled = state.canReroll) {
                    Icon(Icons.Default.Refresh, contentDescription = "Pick another team name")
                }
            }
        }

        when (val link = state.connection) {
            is ConnectionState.Requested ->
                WaitingCard(text = "Waiting for ${link.peerName} to accept…", onCancel = onCancel)
            else -> HostList(hosts = state.hosts, onJoin = onJoin, modifier = Modifier.weight(1f))
        }

        // DEBUG ONLY: fake the link going wrong, only shows in emulator mode
        DebugConnectionButtons()
    }
}

@Composable
private fun HostList(
    hosts: List<DiscoveredHost>,
    onJoin: (DiscoveredHost) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hosts.isEmpty()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Looking for games nearby…", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(hosts, key = { it.endpointId }) { host ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = host.teamName,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { onJoin(host) }) { Text("Join") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingCard(
    text: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(text = text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.error) }
    }
}
