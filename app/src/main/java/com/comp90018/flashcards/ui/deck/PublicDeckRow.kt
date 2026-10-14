package com.comp90018.flashcards.ui.deck

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.ui.components.SegmentedListItem

// Adapted from Jason Ren's PublicDeckScreen in PR #64.
@Composable
internal fun PublicDeckRow(
    deck: RemoteDeck,
    ownedByMe: Boolean,
    index: Int,
    count: Int,
    onOpenDeck: (String) -> Unit, // opens the deck page
    modifier: Modifier = Modifier,
) {
    val owner =
        listOfNotNull(
            deck.ownerUsername?.takeIf { it.isNotBlank() }?.let { "@$it" },
            "Yours".takeIf { ownedByMe },
        ).joinToString(" · ").ifBlank { "Public" }

    SegmentedListItem(
        index = index,
        count = count,
        onClick = { onOpenDeck(deck.deckId) },
        modifier = modifier,
        headlineContent = { Text(deck.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${deck.cardCount} cards · $owner", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}
