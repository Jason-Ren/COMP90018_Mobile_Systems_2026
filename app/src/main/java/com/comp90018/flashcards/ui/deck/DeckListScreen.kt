package com.comp90018.flashcards.ui.deck

import android.R.attr.label
import android.R.attr.onClick
import android.R.attr.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.debug.DEBUG_TOOLS_ENABLED

/**
 * Shows list of users decks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    onNavigateToDeck: (String) -> Unit,
    onCommunity: () -> Unit,
    onNavigateBack: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onAccount: () -> Unit,
    viewModel: DeckListViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsState()
    val emulatorMode by viewModel.emulatorMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }
    var newDeckDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            DeckListTopBar(
                isSyncing = isSyncing,
                onSync = viewModel::syncFromCloud,
                onAccount = onAccount,
                emulatorMode = emulatorMode,
                onEmulatorModeChange = viewModel::setEmulatorMode,
                onCommunity = onCommunity,
            )
        },
        bottomBar = {
            NavBar(onCommunity, onHome, onJoinGame)
        },

    ) { padding ->
        DeckListBody(
            decks = decks,
            isSyncing = isSyncing,
            onNavigateToDeck = onNavigateToDeck,
            onShowDialog = { showDialog = true },
            modifier = Modifier.padding(padding),
        )
    }

    if (showDialog) {
        CreateDeckDialog(
            name = newDeckName,
            description = newDeckDescription,
            onNameChange = { newDeckName = it },
            onDescriptionChange = { newDeckDescription = it },
            onConfirm = {
                if (newDeckName.isNotBlank()) {
                    viewModel.createDeck(newDeckName, newDeckDescription)
                    newDeckName = ""
                    newDeckDescription = ""
                    showDialog = false
                }
            },
            onDismiss = {
                newDeckName = ""
                newDeckDescription = ""
                showDialog = false
            },
        )
    }
}

@Composable
private fun DeckListBody(
    decks: List<DeckEntity>,
    isSyncing: Boolean,
    onNavigateToDeck: (String) -> Unit,
    onShowDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isSyncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        DeckListHeader(onShowDialog = onShowDialog)

        if (decks.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No decks yet.\nTap 'Create Deck' to make your first one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                ) {
                    items(decks, key = { it.deckId }) { deck ->
                        DeckItem(
                            deck = deck,
                            onOpenClick = { onNavigateToDeck(deck.deckId) },
                        )
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.background, Color.Transparent),
                                ),
                            ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckListTopBar(
    isSyncing: Boolean,
    onSync: () -> Unit,
    onAccount: () -> Unit,
    emulatorMode: Boolean,
    onEmulatorModeChange: (Boolean) -> Unit,
    onCommunity: () -> Unit,
) {
    TopAppBar(
        title = {},
        actions = {
            TextButton(onClick = onSync, enabled = !isSyncing) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSyncing) "Syncing…" else "Sync")
            }
            // DEBUG ONLY: lets clash mode be tried on an emulator, hide it with DEBUG_TOOLS_ENABLED
            if (DEBUG_TOOLS_ENABLED) {
                Checkbox(checked = emulatorMode, onCheckedChange = onEmulatorModeChange)
                Text("[DEBUG] Emulator mode", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onCommunity) { Text("Community") }
            IconButton(
                onClick = onAccount,
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Account",
                    modifier = Modifier.size(32.dp),
                )
            }
        },
    )
}

@Composable
private fun DeckListHeader(onShowDialog: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "My Decks",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        FilledTonalButton(
            onClick = onShowDialog,
            modifier = Modifier.height(48.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Deck", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CreateDeckDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DeckItem(
    deck: DeckEntity,
    onOpenClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
