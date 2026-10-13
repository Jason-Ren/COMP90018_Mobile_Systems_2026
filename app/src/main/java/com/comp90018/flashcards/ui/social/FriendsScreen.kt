package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.comp90018.flashcards.ui.components.NavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Friends") })
        },
        bottomBar = {
            NavBar(onCommunity, onHome, onJoinGame, onFriends)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            FriendsPanel()
        }
    }
}
