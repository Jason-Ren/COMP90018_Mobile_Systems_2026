package com.comp90018.flashcards.ui.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTimerTest {
    @Test
    fun `timer starts with the full time`() {
        val timer = CardTimer(10)

        assertEquals(10, timer.secondsLeft)
        assertFalse(timer.isPaused)
        assertFalse(timer.isFinished)
    }

    @Test
    fun `each tick uses up one second`() {
        val timer = CardTimer(10)

        assertFalse(timer.tick())
        assertFalse(timer.tick())

        assertEquals(8, timer.secondsLeft)
    }

    @Test
    fun `the last tick reports that time is up, once`() {
        val timer = CardTimer(2)

        assertFalse(timer.tick())
        assertTrue(timer.tick())
        assertFalse(timer.tick())

        assertEquals(0, timer.secondsLeft)
        assertTrue(timer.isFinished)
    }

    @Test
    fun `pausing keeps the time left and ignores ticks`() {
        val timer = CardTimer(10)
        timer.tick()
        timer.tick()

        timer.pause()
        repeat(5) { timer.tick() }

        assertTrue(timer.isPaused)
        assertEquals(8, timer.secondsLeft)
    }

    @Test
    fun `resuming carries on from the time left`() {
        val timer = CardTimer(10)
        timer.tick()
        timer.pause()
        timer.tick()

        timer.resume()
        timer.tick()

        assertFalse(timer.isPaused)
        assertEquals(8, timer.secondsLeft)
    }

    @Test
    fun `pausing on the last second does not let time run out`() {
        val timer = CardTimer(1)
        timer.pause()

        assertFalse(timer.tick())
        assertFalse(timer.isFinished)

        timer.resume()
        assertTrue(timer.tick())
    }

    @Test
    fun `reset restores the full time and unpauses`() {
        val timer = CardTimer(10)
        timer.tick()
        timer.pause()

        timer.reset()

        assertEquals(10, timer.secondsLeft)
        assertFalse(timer.isPaused)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a countdown of zero is not allowed`() {
        CardTimer(0)
    }
}
