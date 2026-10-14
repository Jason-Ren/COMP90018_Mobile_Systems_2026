package com.comp90018.flashcards.ui.deck

import com.comp90018.flashcards.data.remote.RemoteDeck

/**
 * Public catalogue search. A blank query keeps every deck. Otherwise the deck name or creator
 * username must contain the text, ignoring case. Usernames also accept an @ prefix.
 */
internal fun List<RemoteDeck>.matchingPublicSearch(query: String): List<RemoteDeck> {
    val needle = query.trim()
    val username = needle.removePrefix("@")
    return if (needle.isEmpty()) {
        this
    } else {
        filter { deck ->
            deck.name.contains(needle, ignoreCase = true) ||
                (username.isNotEmpty() && deck.ownerUsername.orEmpty().contains(username, ignoreCase = true))
        }
    }
}
