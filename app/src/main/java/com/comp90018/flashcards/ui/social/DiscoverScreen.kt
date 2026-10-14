package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.deck.PublicDeckRow
import com.comp90018.flashcards.ui.deck.matchingPublicSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDeck: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Discover") })
        },
        bottomBar = {
            NavBar(onCommunity, onHome, onJoinGame, onFriends)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            DiscoverPanel(onOpenDeck = onOpenDeck)
        }
    }
}

@Composable
private fun DiscoverPanel(
    onOpenDeck: (String) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Find your next study deck", style = MaterialTheme.typography.headlineSmall)
            Text("Browse public decks or open a deck a friend shared with you.")
        }
        item { SharedDeckCode(onOpenDeck = onOpenDeck) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Public decks", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = viewModel::refresh, enabled = !task.busy) { Text("Refresh") }
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
        label = { Text("Search by deck name or username") },
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
    OutlinedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Have a shared deck code?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    invalid = false
                },
                singleLine = true,
                label = { Text("Paste deck code") },
                isError = invalid,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (invalid) Text("Paste the complete deck code copied by your friend.") },
            )
            Button(onClick = {
                val id = sharedDeckId(code)
                if (id == null) invalid = true else onOpenDeck(id)
            }, enabled = code.isNotBlank()) { Text("Open shared deck") }
        }
    }
}
