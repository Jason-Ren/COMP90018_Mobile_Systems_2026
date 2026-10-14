package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.R
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.data.local.model.DeckWithCardCount
import com.comp90018.flashcards.debug.DEBUG_TOOLS_ENABLED
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.components.NavDestination

/**
 * Shows list of users decks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    onNavigateToDeck: (String) -> Unit,
    onCommunity: () -> Unit,
    onFriends: () -> Unit,
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
            )
        },
        bottomBar = {
            NavBar(NavDestination.DECKS, onCommunity, onHome, onJoinGame, onFriends)
        },
    ) { padding ->
        DeckListBody(
            decks = decks,
            isSyncing = isSyncing,
            onNavigateToDeck = onNavigateToDeck,
            onShowDialog = { showDialog = true },
            bottomInset = padding.calculateBottomPadding(),
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
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
    decks: List<DeckWithCardCount>,
    isSyncing: Boolean,
    onNavigateToDeck: (String) -> Unit,
    onShowDialog: () -> Unit,
    bottomInset: Dp,
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
        DeckListHeader(deckCount = decks.size, onShowDialog = onShowDialog)

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
                    contentPadding = PaddingValues(top = 16.dp, bottom = bottomInset + 16.dp),
                ) {
                    items(decks, key = { it.deck.deckId }) { item ->
                        DeckItem(
                            deck = item.deck,
                            cardCount = item.cardCount,
                            onOpenClick = { onNavigateToDeck(item.deck.deckId) },
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
) {
    var debugMenuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text("My Decks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        },
        actions = {
            IconButton(onClick = onSync, enabled = !isSyncing) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = if (isSyncing) "Syncing decks" else "Sync decks",
                )
            }
            // Keep debug controls out of the main toolbar when enabled for emulator testing.
            if (DEBUG_TOOLS_ENABLED) {
                Box {
                    IconButton(onClick = { debugMenuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Debug options")
                    }
                    DropdownMenu(expanded = debugMenuOpen, onDismissRequest = { debugMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Emulator mode") },
                            leadingIcon = { Checkbox(checked = emulatorMode, onCheckedChange = null) },
                            onClick = {
                                onEmulatorModeChange(!emulatorMode)
                                debugMenuOpen = false
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onAccount) {
                Icon(Icons.Default.Settings, contentDescription = "Account")
            }
        },
    )
}

@Composable
private fun DeckListHeader(
    deckCount: Int,
    onShowDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.deck_list_count, deckCount, deckCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = onShowDialog,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(8.dp))
            Text("Create Deck")
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
    cardCount: Int,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onOpenClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.deck_card_count, cardCount, cardCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
