package com.comp90018.flashcards.data.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class DealOrdersTest {
    @Test
    fun `both teams get every card exactly once`() {
        val cards = (1..10).toList()

        val (first, second) = dealOrders(cards, Random(1))

        assertEquals(cards, first.sorted())
        assertEquals(cards, second.sorted())
    }

    @Test
    fun `the two teams never hold the same card at the same position`() {
        for (size in 2..25) {
            val (first, second) = dealOrders((1..size).toList(), Random(size))

            first.indices.forEach { assertNotEquals("deck of $size, position $it", first[it], second[it]) }
        }
    }

    @Test
    fun `the order is shuffled and not just the deck order`() {
        val cards = (1..20).toList()

        val (first, _) = dealOrders(cards, Random(7))

        assertNotEquals(cards, first)
    }

    @Test
    fun `an empty or one card deck still deals`() {
        assertEquals(emptyList<Int>() to emptyList<Int>(), dealOrders(emptyList<Int>()))
        assertEquals(listOf(1) to listOf(1), dealOrders(listOf(1)))
    }
}
