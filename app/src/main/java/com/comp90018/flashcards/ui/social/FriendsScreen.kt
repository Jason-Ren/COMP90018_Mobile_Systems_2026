package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    pendingFriendRequestCount: Int = 0,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = {
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        bottomBar = {
            NavBar(
                NavDestination.FRIENDS,
                onCommunity,
                onHome,
                onJoinGame,
                onFriends,
                pendingFriendRequestCount = pendingFriendRequestCount,
            )
        },
    ) { padding ->
        Box(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            FriendsPanel(bottomInset = padding.calculateBottomPadding())
        }
    }
}
