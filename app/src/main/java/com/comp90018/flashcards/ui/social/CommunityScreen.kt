package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.ui.deck.PublicDeckRow
import com.comp90018.flashcards.ui.deck.matchingPublicSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenLocal: (String) -> Unit,
    modifier: Modifier = Modifier,
    startWithFriends: Boolean = false,
) {
    var tab by rememberSaveable { mutableIntStateOf(if (startWithFriends) 1 else 0) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Community") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            })
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Discover") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Friends") })
            }
            if (tab == 0) DiscoverPanel(onOpenDeck = onOpenDeck, onOpenLocal = onOpenLocal) else FriendsPanel()
        }
    }
}

@Composable
private fun DiscoverPanel(
    onOpenDeck: (String) -> Unit,
    onOpenLocal: (String) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    val copies by viewModel.copyActions.collectAsStateWithLifecycle()
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
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                label = { Text("Search by name or creator") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
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
                copies = copies,
                enabled = !task.busy,
                onPreview = onOpenDeck,
                onSaveCopy = viewModel::saveCopy,
                onOpenLocal = onOpenLocal,
            )
        }
    }
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
