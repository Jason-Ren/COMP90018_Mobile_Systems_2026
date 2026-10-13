package com.comp90018.flashcards.data.remote

import java.time.Instant

/**
 * Cloud deck document at `decks/{deckId}`. See [docs/cloud-deck-schema.md].
 */
data class RemoteDeck(
    val deckId: String,
    val name: String,
    val ownerId: String,
    val visibility: DeckVisibility,
    val updatedAt: Instant,
    val cardCount: Int,
    /** Optional creator email for catalogue search. Not stored in Room. */
    val authorEmail: String? = null,
    /** When the deck was first created; used for stable newest-first My Decks order. */
    val createdAt: Instant? = null,
)
