package com.comp90018.flashcards.ui.social

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compatibility entry point until navigation uses [DiscoverScreen] and [FriendsScreen] directly. */
@Composable
fun CommunityScreen(
    onOpenDeck: (String) -> Unit,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
    modifier: Modifier = Modifier,
    startWithFriends: Boolean = false,
    pendingFriendRequestCount: Int = 0,
) {
    if (startWithFriends) {
        FriendsScreen(
            onCommunity = onCommunity,
            onHome = onHome,
            onJoinGame = onJoinGame,
            onFriends = onFriends,
            modifier = modifier,
            pendingFriendRequestCount = pendingFriendRequestCount,
        )
    } else {
        DiscoverScreen(
            onOpenDeck = onOpenDeck,
            onCommunity = onCommunity,
            onHome = onHome,
            onJoinGame = onJoinGame,
            onFriends = onFriends,
            modifier = modifier,
            pendingFriendRequestCount = pendingFriendRequestCount,
        )
    }
}
