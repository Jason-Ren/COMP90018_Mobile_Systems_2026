package com.comp90018.flashcards.ui.social

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compatibility entry point until navigation uses [DiscoverScreen] and [FriendsScreen] directly. */
@Composable
fun CommunityScreen(
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    modifier: Modifier = Modifier,
    startWithFriends: Boolean = false,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit

) {
    if (startWithFriends) {
        FriendsScreen(onBack = onBack, modifier = modifier,onCommunity, onHome, onJoinGame, onFriends)
    } else {
        DiscoverScreen(onBack = onBack, onOpenDeck = onOpenDeck, modifier = modifier,onCommunity, onHome, onJoinGame, onFriends)
    }
    
}
