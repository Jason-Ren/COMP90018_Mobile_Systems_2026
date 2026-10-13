package com.comp90018.flashcards.ui.play

import androidx.lifecycle.SavedStateHandle
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.testutil.FakeAnswerSounds
import com.comp90018.flashcards.testutil.FakeCardRepository
import com.comp90018.flashcards.testutil.FakeGameVibration
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.testCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class DuoViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val deck = DeckEntity(deckId = "deck", name = "Capitals", description = "", ownerId = "me")

    private fun viewModel(cardCount: Int = 3) =
        DuoViewModel(
            repository = FakeCardRepository(deck, testCards(cardCount)),
            savedStateHandle = SavedStateHandle(mapOf("deckId" to "deck", "seconds" to 5)),
            // no real audio or vibrator in a unit test
            sounds = FakeAnswerSounds(),
            vibration = FakeGameVibration(),
        )

    // goes through the whole round, marking every card the same way, and returns the cards in the order shown
    private fun DuoViewModel.playAll(tilt: TiltGesture): List<String> {
        start()
        val seen = mutableListOf<String>()
        while (uiState.value.phase == DuoPhase.FRONT) {
            seen += uiState.value.current!!.cardId
            reveal()
            onGesture(tilt)
        }
        return seen
    }

    @Test
    fun `the deck is loaded with its name and size`() {
        val state = viewModel(cardCount = 3).uiState.value

        assertFalse(state.isLoading)
        assertEquals("Capitals", state.deckName)
        assertEquals(3, state.totalCount)
        assertEquals(5, state.secondsPerCard)
        assertEquals(DuoPhase.READY, state.phase)
    }

    @Test
    fun `every card comes up exactly once in a round`() {
        val seen = viewModel(cardCount = 6).playAll(TiltGesture.DOWN)

        assertEquals(setOf("c1", "c2", "c3", "c4", "c5", "c6"), seen.toSet())
        assertEquals(6, seen.size)
    }

    @Test
    fun `marking everything right scores the whole deck and misses nothing`() {
        val vm = viewModel()

        vm.playAll(TiltGesture.DOWN)

        assertEquals(3, vm.uiState.value.correctCount)
        assertEquals(
            emptyList<String>(),
            vm.uiState.value.missed
                .map { it.cardId },
        )
    }

    @Test
    fun `marking everything wrong lists every card as missed`() {
        val vm = viewModel()

        vm.playAll(TiltGesture.UP)

        assertEquals(0, vm.uiState.value.correctCount)
        assertEquals(3, vm.uiState.value.missed.size)
    }

    @Test
    fun `playing again starts a new round with the same cards`() {
        val vm = viewModel()
        vm.playAll(TiltGesture.DOWN)

        vm.playAgain()

        val state = vm.uiState.value
        assertEquals(DuoPhase.FRONT, state.phase)
        assertEquals(0, state.correctCount)
        assertEquals(3, state.totalCount)
        assertEquals(emptyList<String>(), state.missed.map { it.cardId })
    }

    @Test
    fun `an empty deck finishes straight away`() {
        val vm = viewModel(cardCount = 0)

        vm.start()

        assertEquals(DuoPhase.FINISHED, vm.uiState.value.phase)
    }
}
