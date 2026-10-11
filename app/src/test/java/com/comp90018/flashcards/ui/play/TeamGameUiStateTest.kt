package com.comp90018.flashcards.ui.play

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamGameUiStateTest {
    private fun state(
        mine: Int,
        theirs: Int?,
    ) = TeamGameUiState(round = DuoUiState(correctCount = mine), opponentCorrect = theirs)

    @Test
    fun `the headline waits while the other team has no score yet`() {
        assertEquals("Waiting for the other team to finish", state(5, null).headline)
    }

    @Test
    fun `a higher score wins`() {
        assertEquals("You win!", state(6, 5).headline)
    }

    @Test
    fun `a lower score loses`() {
        assertEquals("The other team wins", state(4, 5).headline)
    }

    @Test
    fun `equal scores are a tie, including nil nil`() {
        assertEquals("It's a tie", state(5, 5).headline)
        assertEquals("It's a tie", state(0, 0).headline)
    }
}
