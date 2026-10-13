package com.comp90018.flashcards.ui.social

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compatibility entry point until navigation uses [DiscoverScreen] and [FriendsScreen] directly. */
@Composable
fun CommunityScreen(
    onOpenDeck: (String) -> Unit,
    modifier: Modifier = Modifier,
    startWithFriends: Boolean = false,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit
) {
    if (startWithFriends) {
        FriendsScreen(modifier = modifier, onCommunity = onCommunity, onHome = onHome, onJoinGame = onJoinGame, onFriends = onFriends)
    } else {
        DiscoverScreen(onOpenDeck = onOpenDeck, modifier = modifier, onCommunity = onCommunity, onHome = onHome, onJoinGame = onJoinGame, onFriends = onFriends)
    }
}
