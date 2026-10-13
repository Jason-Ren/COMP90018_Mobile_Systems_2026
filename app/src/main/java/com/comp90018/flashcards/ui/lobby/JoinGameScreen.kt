package com.comp90018.flashcards.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.connection.ConnectionOverlay
import com.comp90018.flashcards.ui.connection.DebugConnectionButtons
import com.comp90018.flashcards.ui.help.helpTip
import com.comp90018.flashcards.ui.help.HelpTopic


/**
 * The guest's side of clash mode: list hosts nearby, ask to join, and once accepted display [GuestLobbyScreen].
 */

private val JOIN_CARD_HEIGHT = 100.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(
    onNavigateBack: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinGameViewModel = hiltViewModel(),
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.begin() }

    // once the host accepts, move on to the lobby screen
    val openLobby by rememberUpdatedState(onJoin)
    val joined = state.connection is ConnectionState.Connected
    LaunchedEffect(joined) { if (joined) openLobby() }

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
                title = { Text("Join clash") },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    helpTip(
                        topic = HelpTopic.MultiSetupHelp,
                        modifier = Modifier.padding(end = 12.dp),
                        size = 36.dp,
                    )
                },
            )
        },
        bottomBar = {
            NavBar(onCommunity, onHome, onJoinGame, onFriends)
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
        Card(modifier = Modifier.fillMaxWidth().height(JOIN_CARD_HEIGHT)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
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

        HorizontalDivider()

        when (val link = state.connection) {
            is ConnectionState.Requested ->
                WaitingCard(text = "Waiting for ${link.peerName} to accept…", onCancel = onCancel)
            else ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Nearby Clashes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    HostList(hosts = state.hosts, onJoin = onJoin, modifier = Modifier.weight(1f))
                }
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
            Text("Looking for clashes nearby…", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(hosts, key = { it.endpointId }) { host ->
                Card(modifier = Modifier.fillMaxWidth().height(JOIN_CARD_HEIGHT)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
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
        TextButton(onClick = onCancel) {
            Text(
                "Cancel",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
