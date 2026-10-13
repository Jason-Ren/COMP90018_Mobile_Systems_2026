package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.ui.social.PublishConfirmation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckPageScreen(
    onNavigateBack: () -> Unit,
    onPlay: (String) -> Unit,
    onAddCard: (String) -> Unit,
    onEditCard: (String, String) -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenLocal: (String) -> Unit = {},
    onFriends: () -> Unit = {},
    viewModel: DeckSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskState by viewModel.taskState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPublishConfirmDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedReadOnlyCard by remember { mutableStateOf<CardUiModel?>(null) }

    LaunchedEffect(taskState.message) {
        taskState.message?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(taskState.error) {
        taskState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            DeckPageBody(
                uiState = uiState,
                taskBusy = taskState.busy,
                onOpenSource = onOpenSource,
                onOpenLocal = onOpenLocal,
                onSaveCopy = viewModel::saveCopy,
                onMoreCards = viewModel::moreCards,
                onAddCard = { onAddCard(viewModel.deckId) },
                onEditCard = { cardId -> onEditCard(viewModel.deckId, cardId) },
                onSelectReadOnlyCard = { card -> selectedReadOnlyCard = card },
                onShareClick = {
                    showShareSheet = true
                    viewModel.loadFriends()
                },
                onOptionsClick = { showOptionsSheet = true },
            )

            if (uiState.isOwned) {
                PlayBottomBar(
                    canPlay = uiState.cardCount > 0,
                    onPlay = { onPlay(viewModel.deckId) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showShareSheet) {
        UnifiedDeckShareSheet(
            deckId = viewModel.deckId,
            friends = uiState.friends,
            task = taskState,
            onDismiss = { showShareSheet = false },
            onFriends = {
                showShareSheet = false
                onFriends()
            },
            onShare = viewModel::share,
            onRefresh = viewModel::loadFriends,
        )
    }

    if (showOptionsSheet) {
        DeckOptionsBottomSheet(
            isPublic = uiState.visibility == "public",
            onDismiss = { showOptionsSheet = false },
            onEditDeckInfo = { showEditDialog = true },
            onEditVisibility = { showPublishConfirmDialog = true },
            onDeleteDeck = { showDeleteConfirmDialog = true },
        )
    }

    if (showEditDialog) {
        EditDeckDialog(
            currentName = uiState.deckName,
            currentDescription = uiState.deckDescription,
            onConfirm = { newName, newDescription ->
                viewModel.updateDeck(newName, newDescription)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showPublishConfirmDialog) {
        PublishConfirmation(
            public = uiState.visibility != "public",
            onConfirm = {
                showPublishConfirmDialog = false
                viewModel.publish(uiState.visibility != "public")
            },
            onDismiss = { showPublishConfirmDialog = false },
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteDeckDialog(
            deckName = uiState.deckName,
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteDeck(onDeleted = onNavigateBack)
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    selectedReadOnlyCard?.let { card ->
        ReadOnlyCardDialog(
            card = card,
            onDismiss = { selectedReadOnlyCard = null },
        )
    }
}

@Composable
private fun DeckPageBody(
    uiState: DeckSummaryUiState,
    taskBusy: Boolean,
    onOpenSource: (String) -> Unit,
    onOpenLocal: (String) -> Unit,
    onSaveCopy: () -> Unit,
    onMoreCards: () -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (String) -> Unit,
    onSelectReadOnlyCard: (CardUiModel) -> Unit,
    onShareClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckHeaderSection(
            uiState = uiState,
            onOpenSource = onOpenSource,
            onShareClick = onShareClick,
            onOptionsClick = onOptionsClick,
        )

        if (!uiState.isOwned) {
            CommunityCopyActionSection(
                uiState = uiState,
                taskBusy = taskBusy,
                onSaveCopy = onSaveCopy,
                onOpenLocal = onOpenLocal,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cards · ${uiState.cardCount}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (uiState.isOwned) {
                FilledTonalButton(
                    onClick = onAddCard,
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        CardsListSection(
            cards = uiState.cards,
            isOwned = uiState.isOwned,
            moreCards = uiState.moreCards,
            taskBusy = taskBusy,
            onMoreCards = onMoreCards,
            onEditCard = onEditCard,
            onSelectReadOnlyCard = onSelectReadOnlyCard,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CommunityCopyActionSection(
    uiState: DeckSummaryUiState,
    taskBusy: Boolean,
    onSaveCopy: () -> Unit,
    onOpenLocal: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uiState.savedCopyId != null) {
            Button(
                onClick = { onOpenLocal(uiState.savedCopyId) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("View my copy", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onSaveCopy,
                enabled = !taskBusy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(
                    if (uiState.pendingCopy) "Retry saving copy" else "Save a copy to My Decks",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = "Your copy is private and editable, with fresh study progress.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardsListSection(
    cards: List<CardUiModel>,
    isOwned: Boolean,
    moreCards: Boolean,
    taskBusy: Boolean,
    onMoreCards: () -> Unit,
    onEditCard: (String) -> Unit,
    onSelectReadOnlyCard: (CardUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    if (isOwned) {
                        "No cards in this deck yet.\nTap 'Add Card' above to create cards."
                    } else {
                        "This deck has no cards yet."
                    },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Box(modifier = modifier) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = if (isOwned) 120.dp else 24.dp),
            ) {
                items(cards, key = { it.cardId }) { card ->
                    CardItemView(
                        card = card,
                        onClick = {
                            if (isOwned) onEditCard(card.cardId) else onSelectReadOnlyCard(card)
                        },
                    )
                }
                if (moreCards && !isOwned) {
                    item {
                        TextButton(
                            onClick = onMoreCards,
                            enabled = !taskBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Load more cards")
                        }
                    }
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

@Composable
private fun PlayBottomBar(
    canPlay: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ).padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 16.dp),
    ) {
        Button(
            onClick = onPlay,
            enabled = canPlay,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeckHeaderSection(
    uiState: DeckSummaryUiState,
    onOpenSource: (String) -> Unit,
    onShareClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiState.deckName.ifBlank { "Deck" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShareClick) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                if (uiState.isOwned) {
                    IconButton(onClick = onOptionsClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (uiState.visibility == "public") "Public" else "Private",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "· ${uiState.cardCount} cards",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.authorEmail?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text = "· $email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        uiState.copiedFrom?.let { source ->
            TextButton(
                onClick = { onOpenSource(source.deckId) },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Saved copy · View source deck")
            }
        }

        if (uiState.deckDescription.isNotBlank()) {
            Text(
                text = uiState.deckDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardItemView(
    card: CardUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = card.front,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.back,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.frontImageUri != null || card.backImageUri != null) {
                Text(
                    text = "Contains image attachment",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EditDeckDialog(
    currentName: String,
    currentDescription: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Deck Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteDeckDialog(
    deckName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Deck") },
        text = { Text("Are you sure you want to delete '$deckName'? This action cannot be reversed.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        },
    )
}
