package com.comp90018.flashcards.data.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameSessionTest {
    @Test
    fun `a new session has no setup`() {
        assertNull(GameSession().setup)
    }

    @Test
    fun `the setup can be stored and read back`() {
        val session = GameSession()
        val setup = GameSetup("Deck", 10, emptyList())

        session.setup = setup

        assertEquals(setup, session.setup)
    }

    @Test
    fun `a start message becomes a setup with the cards in the same order`() {
        val start = GameMessage.Start("Capitals", 30, listOf(WireCard("France", "Paris"), WireCard("Peru", "Lima")))

        val setup = start.toSetup()

        assertEquals("Capitals", setup.deckName)
        assertEquals(30, setup.secondsPerCard)
        assertEquals(listOf("France", "Peru"), setup.cards.map { it.front })
        assertEquals(listOf("Paris", "Lima"), setup.cards.map { it.back })
    }

    @Test
    fun `cards built from a message get their own ids and no images`() {
        val setup = GameMessage.Start("D", 5, listOf(WireCard("a", "b"), WireCard("c", "d"))).toSetup()

        assertEquals(listOf("team-0", "team-1"), setup.cards.map { it.cardId })
        assertEquals(setOf(null), setup.cards.map { it.frontImageUri }.toSet())
        assertEquals(setOf(null), setup.cards.map { it.backImageUri }.toSet())
    }
}
