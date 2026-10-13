package com.comp90018.flashcards.ui.play

import androidx.lifecycle.SavedStateHandle
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.testutil.FakeAnswerSounds
import com.comp90018.flashcards.testutil.FakeCardRepository
import com.comp90018.flashcards.testutil.FakeGameVibration
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.passSeconds
import com.comp90018.flashcards.testutil.testCards
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `playing again starts a new round with the same cards`() =
        runTest {
            val vm = viewModel()
            vm.playAll(TiltGesture.DOWN)

            vm.playAgain()
            // the 3, 2, 1 comes before the first card
            passSeconds(3)

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

    @Test
    fun `pose starts once and leaving it freezes the opening countdown`() =
        runTest {
            val vm = viewModel()
            vm.onForeheadPose(false)
            assertEquals(0, vm.uiState.value.startCountdown)
            vm.onForeheadPose(true)
            passSeconds(1)
            vm.onForeheadPose(true)
            assertEquals(2, vm.uiState.value.startCountdown)

            vm.onForeheadPose(false)
            passSeconds(5)
            assertTrue(vm.uiState.value.isPosePaused)
            assertTrue(vm.uiState.value.isPaused)
            assertEquals(2, vm.uiState.value.startCountdown)
            assertEquals(DuoPhase.READY, vm.uiState.value.phase)

            vm.onForeheadPose(true)
            passSeconds(2)
            assertEquals(DuoPhase.FRONT, vm.uiState.value.phase)
        }

    @Test
    fun `leaving pose holds time and returning resumes the same card`() =
        runTest {
            val vm = viewModel()
            vm.onForeheadPose(true)
            passSeconds(4)
            val card = vm.uiState.value.current
            assertEquals(4, vm.uiState.value.secondsLeft)

            vm.onForeheadPose(false)
            vm.reveal()
            vm.onGesture(TiltGesture.DOWN)
            passSeconds(10)
            assertEquals(card, vm.uiState.value.current)
            assertEquals(DuoPhase.FRONT, vm.uiState.value.phase)
            assertEquals(4, vm.uiState.value.secondsLeft)

            vm.onForeheadPose(true)
            passSeconds(4)
            assertEquals(DuoPhase.BACK, vm.uiState.value.phase)
        }

    @Test
    fun `leaving pose on the back prevents scoring until returning`() =
        runTest {
            val vm = viewModel(cardCount = 1)
            vm.onForeheadPose(true)
            passSeconds(3)
            vm.reveal()
            vm.onForeheadPose(false)
            vm.onGesture(TiltGesture.DOWN)
            assertEquals(DuoPhase.BACK, vm.uiState.value.phase)
            assertEquals(0, vm.uiState.value.correctCount)

            vm.onForeheadPose(true)
            vm.onGesture(TiltGesture.DOWN)
            assertEquals(DuoPhase.FINISHED, vm.uiState.value.phase)
            vm.onForeheadPose(true)
            vm.onForeheadPose(false)
            assertEquals(0, vm.uiState.value.startCountdown)
            assertFalse(vm.uiState.value.isPaused)
        }

    @Test
    fun `manual start keeps working without pose gating`() =
        runTest {
            val vm = viewModel()
            vm.startWithCountdown()
            vm.onForeheadPose(true)
            vm.onForeheadPose(false)
            passSeconds(4)
            assertFalse(vm.uiState.value.isPoseControlled)
            assertFalse(vm.uiState.value.isPaused)
            assertEquals(4, vm.uiState.value.secondsLeft)
        }

    @Test
    fun `pose resume cannot clear a background pause or be bypassed by Resume`() =
        runTest {
            val vm = viewModel()
            vm.onForeheadPose(true)
            passSeconds(3)
            vm.pause()
            vm.onForeheadPose(false)
            vm.resume()
            assertTrue(vm.uiState.value.isPaused)

            vm.pause()
            vm.onForeheadPose(true)
            passSeconds(5)
            assertTrue(vm.uiState.value.isPaused)
            assertEquals(5, vm.uiState.value.secondsLeft)
            vm.resume()
            assertFalse(vm.uiState.value.isPaused)
        }

    @Test
    fun `pose controlled replay waits for the next pose instead of starting manually`() =
        runTest {
            val vm = viewModel(cardCount = 1)
            vm.onForeheadPose(true)
            passSeconds(3)
            vm.reveal()
            vm.onGesture(TiltGesture.DOWN)
            vm.playAgain()
            passSeconds(5)
            assertEquals(DuoPhase.READY, vm.uiState.value.phase)
            assertEquals(0, vm.uiState.value.startCountdown)

            vm.onForeheadPose(true)
            passSeconds(3)
            assertEquals(DuoPhase.FRONT, vm.uiState.value.phase)
            assertTrue(vm.uiState.value.isPoseControlled)
        }

    @Test
    fun `backgrounding during the opening countdown holds it until Resume`() =
        runTest {
            val vm = viewModel()
            vm.startWithCountdown()
            passSeconds(1)
            vm.pause()
            passSeconds(5)
            assertEquals(2, vm.uiState.value.startCountdown)
            vm.resume()
            passSeconds(2)
            assertEquals(DuoPhase.FRONT, vm.uiState.value.phase)
        }
}
