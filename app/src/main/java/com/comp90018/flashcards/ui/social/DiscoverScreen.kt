package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.components.NavDestination
import com.comp90018.flashcards.ui.deck.PublicDeckRow
import com.comp90018.flashcards.ui.deck.matchingPublicSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDeck: (String) -> Unit,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = {
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        bottomBar = {
            NavBar(NavDestination.DISCOVER, onCommunity, onHome, onJoinGame, onFriends)
        },
    ) { padding ->
        Box(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            DiscoverPanel(onOpenDeck = onOpenDeck, bottomInset = padding.calculateBottomPadding())
        }
    }
}

@Composable
private fun DiscoverPanel(
    onOpenDeck: (String) -> Unit,
    bottomInset: Dp,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SharedDeckCode(onOpenDeck = onOpenDeck) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Public decks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = viewModel::refresh,
                    enabled = !task.busy,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            SocialStatus(task)
            DiscoverSearchField(query = search, onQueryChange = { search = it })
        }
        val visible = decks.matchingPublicSearch(search)
        if (visible.isEmpty() && !task.busy && task.error == null) {
            item {
                Text(
                    if (search.isBlank()) {
                        "No public decks yet. Publish one of your own to get started."
                    } else {
                        "No public decks match that search."
                    },
                )
            }
        }
        items(visible, key = { it.deckId }) { deck ->
            PublicDeckRow(
                deck = deck,
                ownedByMe = deck.ownerId == viewModel.userId,
                onOpenDeck = onOpenDeck,
            )
        }
    }
}

@Composable
private fun DiscoverSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                "Search by deck name or username",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun SharedDeckCode(onOpenDeck: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val submit = {
        val id = sharedDeckId(code)
        if (id == null) invalid = true else onOpenDeck(id)
    }
    OutlinedCard(
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Have a shared deck code from a friend?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    invalid = false
                },
                singleLine = true,
                placeholder = { Text("Paste deck code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = {
                    IconButton(onClick = submit, enabled = code.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open shared deck",
                            tint =
                                if (code.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                        )
                    }
                },
                isError = invalid,
                // Passing null (not an empty lambda) stops Compose reserving blank space below the field.
                supportingText =
                    if (invalid) {
                        { Text("Paste the complete deck code.") }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
