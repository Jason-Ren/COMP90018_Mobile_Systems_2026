package com.comp90018.flashcards.ui.deck

import com.comp90018.flashcards.data.remote.DeckVisibility
import com.comp90018.flashcards.data.remote.RemoteDeck
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PublicDeckSearchTest {
    private val decks =
        listOf(
            deck(id = "1", name = "Android quiz", author = "ada_lovelace"),
            deck(id = "2", name = "Capitals", author = "grace_hopper"),
        )

    @Test
    fun `blank search keeps every public deck`() {
        assertEquals(listOf("1", "2"), decks.matchingPublicSearch("  ").map { it.deckId })
    }

    @Test
    fun `search matches deck name or creator username`() {
        assertEquals(listOf("1"), decks.matchingPublicSearch("android").map { it.deckId })
        assertEquals(listOf("2"), decks.matchingPublicSearch("GRACE").map { it.deckId })
        assertEquals(listOf("1"), decks.matchingPublicSearch(" lovelace ").map { it.deckId })
        assertEquals(listOf("2"), decks.matchingPublicSearch(" @GRACE_HOPPER ").map { it.deckId })
        assertEquals(emptyList<String>(), decks.matchingPublicSearch("biology").map { it.deckId })
        assertEquals(emptyList<RemoteDeck>(), decks.matchingPublicSearch("@"))
    }

    @Test
    fun `creator search uses usernames instead of display names or legacy emails`() {
        val legacyDeck =
            decks.first().copy(authorEmail = "private@example.com", ownerDisplayName = "Jason", ownerUsername = null)
        assertEquals(emptyList<RemoteDeck>(), listOf(legacyDeck).matchingPublicSearch("private@example.com"))
        assertEquals(emptyList<RemoteDeck>(), listOf(legacyDeck).matchingPublicSearch("Jason"))
        assertEquals(listOf(legacyDeck), listOf(legacyDeck).matchingPublicSearch("android"))
    }

    private fun deck(
        id: String,
        name: String,
        author: String,
    ) = RemoteDeck(
        deckId = id,
        name = name,
        ownerId = "uid",
        visibility = DeckVisibility.PUBLIC,
        updatedAt = Instant.EPOCH,
        cardCount = 1,
        ownerUsername = author,
    )
}
