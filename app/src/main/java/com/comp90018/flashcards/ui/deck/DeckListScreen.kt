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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    pendingFriendRequestCount: Int = 0,
    viewModel: DeckListViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsState()
    val emulatorMode by viewModel.emulatorMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var fabHeight by remember { mutableStateOf(56.dp) }

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
            NavBar(
                NavDestination.DECKS,
                onCommunity,
                onHome,
                onJoinGame,
                onFriends,
                pendingFriendRequestCount = pendingFriendRequestCount,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            DeckListBody(
                decks = decks,
                isSyncing = isSyncing,
                onNavigateToDeck = onNavigateToDeck,
                bottomInset = padding.calculateBottomPadding() + fabHeight + 16.dp,
            )
            // Keep the FAB above the list for both touch and accessibility traversal.
            CreateDeckFab(
                onCreate = { android.util.Log.d("DeckFabCheck", "pressed"); showDialog = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f)
                        .padding(end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                        .onSizeChanged { fabHeight = with(density) { it.height.toDp() } },
            )
        }
    }

    if (showDialog) {
        NewDeckDialog(
            onCreate = viewModel::createDeck,
            onDismiss = { android.util.Log.d("DeckFabCheck", "dismissed"); showDialog = false },
        )
    }
}

@Composable
private fun NewDeckDialog(
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    CreateDeckDialog(
        name = name,
        description = description,
        onNameChange = { name = it },
        onDescriptionChange = { description = it },
        onConfirm = {
            if (name.isNotBlank()) {
                onCreate(name, description)
                onDismiss()
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun CreateDeckFab(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onCreate,
        modifier = modifier,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.create_deck)) },
    )
}

@Composable
private fun DeckListBody(
    decks: List<DeckWithCardCount>,
    isSyncing: Boolean,
    onNavigateToDeck: (String) -> Unit,
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
        if (decks.isEmpty()) {
            EmptyDecks(Modifier.weight(1f).padding(bottom = bottomInset + 16.dp))
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
                    item(key = "deck_count") { DeckCountFooter(decks.size) }
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

@Composable
private fun EmptyDecks(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.empty_decks),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeckCountFooter(0)
        }
    }
}

@Composable
private fun DeckCountFooter(deckCount: Int) {
    Text(
        text = pluralStringResource(R.plurals.deck_list_count, deckCount, deckCount),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
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
        modifier = Modifier.padding(top = 8.dp),
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
