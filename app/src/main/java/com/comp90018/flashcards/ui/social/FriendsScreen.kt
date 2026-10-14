package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.components.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Friends") })
        },
        bottomBar = {
            NavBar(NavDestination.FRIENDS, onCommunity, onHome, onJoinGame, onFriends)
        },
    ) { padding ->
        Box(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            FriendsPanel(bottomInset = padding.calculateBottomPadding())
        }
    }
}
