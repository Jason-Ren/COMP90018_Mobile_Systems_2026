package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.comp90018.flashcards.ui.components.SegmentedListGap
import com.comp90018.flashcards.ui.components.SegmentedListSectionGap
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
    pendingFriendRequestCount: Int = 0,
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
            NavBar(
                NavDestination.DISCOVER,
                onCommunity,
                onHome,
                onJoinGame,
                onFriends,
                pendingFriendRequestCount = pendingFriendRequestCount,
            )
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(SegmentedListGap),
    ) {
        item {
            Box(Modifier.padding(bottom = SegmentedListSectionGap)) { SharedDeckCode(onOpenDeck = onOpenDeck) }
        }
        item {
            DiscoverHeader(task = task, search = search, onSearch = { search = it }, onRefresh = viewModel::refresh)
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
        itemsIndexed(visible, key = { _, deck -> deck.deckId }) { index, deck ->
            PublicDeckRow(
                deck = deck,
                ownedByMe = deck.ownerId == viewModel.userId,
                index = index,
                count = visible.size,
                onOpenDeck = onOpenDeck,
            )
        }
    }
}

@Composable
private fun DiscoverHeader(
    task: SocialTaskState,
    search: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        Modifier.padding(bottom = SegmentedListSectionGap),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Public decks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = onRefresh,
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
        DiscoverSearchField(query = search, onQueryChange = onSearch)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Add a friend's deck",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Paste their shared code to add a copy to your decks.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    invalid = false
                },
                singleLine = true,
                placeholder = { Text("Paste deck code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = {
                    IconButton(
                        onClick = submit,
                        enabled = code.isNotBlank(),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open shared deck",
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
