package com.comp90018.flashcards.ui.lobby

import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.testutil.FakeCardRepository
import com.comp90018.flashcards.testutil.FakeConnectionController
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.testCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameSetupViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val session = GameSession()
    private val deck = DeckEntity(deckId = "deck", name = "Capitals", description = "", ownerId = "me")

    private fun viewModel(cards: Int = 6) =
        GameSetupViewModel(FakeCardRepository(deck, testCards(cards)), connection, session)

    private fun startedMessage() = connection.sent.single() as GameMessage.Start

    @Test
    fun `starting keeps the host's setup and opens the game`() {
        var opened = false

        viewModel().startGame("deck", 20) { opened = true }

        assertTrue(opened)
        assertEquals("Capitals", session.setup!!.deckName)
        assertEquals(20, session.setup!!.secondsPerCard)
        assertEquals(6, session.setup!!.cards.size)
    }

    @Test
    fun `starting sends the guest the same deck and countdown`() {
        viewModel().startGame("deck", 20) {}

        val start = startedMessage()
        assertEquals("Capitals", start.deckName)
        assertEquals(20, start.secondsPerCard)
        assertEquals(
            session.setup!!
                .cards
                .map { it.front }
                .sorted(),
            start.cards.map { it.front }.sorted(),
        )
    }

    @Test
    fun `the two teams are never on the same card at the same time`() {
        viewModel(cards = 8).startGame("deck", 20) {}

        val mine = session.setup!!.cards.map { it.front }
        val theirs = startedMessage().cards.map { it.front }
        mine.indices.forEach { assertNotEquals("position $it", mine[it], theirs[it]) }
    }

    @Test
    fun `an empty deck still opens the game`() {
        var opened = false

        viewModel(cards = 0).startGame("deck", 20) { opened = true }

        assertTrue(opened)
        assertTrue(startedMessage().cards.isEmpty())
    }
}
