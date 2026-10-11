package com.comp90018.flashcards.data.p2p

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameMessageTest {
    // the same settings the real transport uses
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(message: GameMessage): GameMessage =
        json.decodeFromString(GameMessage.serializer(), json.encodeToString(GameMessage.serializer(), message))

    @Test
    fun `start keeps the deck name, countdown and every card in order`() {
        val start = GameMessage.Start("Capitals", 30, listOf(WireCard("France", "Paris"), WireCard("Peru", "Lima")))

        assertEquals(start, roundTrip(start))
    }

    @Test
    fun `start with no cards still round trips`() {
        val start = GameMessage.Start("Empty", 10, emptyList())

        assertEquals(start, roundTrip(start))
    }

    @Test
    fun `finished keeps the score`() {
        assertEquals(GameMessage.Finished(7), roundTrip(GameMessage.Finished(7)))
    }

    @Test
    fun `end session round trips`() {
        assertEquals(GameMessage.EndSession, roundTrip(GameMessage.EndSession))
    }

    @Test
    fun `text with symbols and non latin characters survives`() {
        val start = GameMessage.Start("日本語 \"deck\"", 5, listOf(WireCard("a|b\nc", "é ü ñ 😀")))

        assertEquals(start, roundTrip(start))
    }

    @Test
    fun `fields from a newer version are ignored`() {
        val encoded = json.encodeToString(GameMessage.serializer(), GameMessage.Finished(3))
        val withExtra = encoded.dropLast(1) + ",\"somethingNew\":1}"

        assertEquals(GameMessage.Finished(3), json.decodeFromString(GameMessage.serializer(), withExtra))
    }
}
