package com.comp90018.flashcards.ui.social

import androidx.compose.runtime.Composable
import com.comp90018.flashcards.ui.deck.UnifiedDeckShareSheet

@Composable
internal fun DeckShareSheet(
    state: CloudDeckState,
    task: SocialTaskState,
    onDismiss: () -> Unit,
    onFriends: () -> Unit,
    onShare: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    UnifiedDeckShareSheet(
        deckId = state.deck?.deckId.orEmpty(),
        friends = state.friends,
        task = task,
        onDismiss = onDismiss,
        onFriends = onFriends,
        onShare = onShare,
        onRefresh = onRefresh,
        sharedWith = state.sharedWith,
    )
}
