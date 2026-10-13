package com.comp90018.flashcards.ui.play

import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.testutil.FakeAnswerSounds
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

// the shared round, played through a tiny subclass that exposes what the real ones use
private class TestGameViewModel(
    seconds: Int,
    sounds: FakeAnswerSounds,
    vibration: FakeGameVibration,
) : GameViewModel(seconds, sounds, vibration) {
    val finishedScores = mutableListOf<Int>()

    fun load(cards: List<CardEntity>) = loadCards(cards, "Deck")

    fun linkDown(isDown: Boolean) = setLinkDown(isDown)

    override fun onFinished(correctCount: Int) {
        finishedScores += correctCount
    }
}

class GameViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    // records the sounds the round asks for, instead of playing them
    private val sounds = FakeAnswerSounds()

    // records the buzzes the round asks for, instead of vibrating
    private val vibration = FakeGameVibration()

    private fun game(
        seconds: Int = 3,
        cards: Int = 2,
    ) = TestGameViewModel(seconds, sounds, vibration).also { it.load(testCards(cards)) }

    @Test
    fun `loading cards shows the ready screen with the deck name and size`() {
        val vm = game(cards = 4)

        assertEquals(DuoPhase.READY, vm.round.value.phase)
        assertEquals("Deck", vm.round.value.deckName)
        assertEquals(4, vm.round.value.totalCount)
        assertFalse(vm.round.value.isLoading)
    }

    @Test
    fun `starting shows the first front with the full countdown running`() =
        runTest {
            val vm = game()

            vm.start()

            assertEquals(DuoPhase.FRONT, vm.round.value.phase)
            assertEquals(
                "front 1",
                vm.round.value.current
                    ?.front,
            )
            assertEquals(3, vm.round.value.secondsLeft)
        }

    @Test
    fun `the countdown ticks down and then shows the back`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()

            passSeconds(2)
            assertEquals(1, vm.round.value.secondsLeft)
            assertEquals(DuoPhase.FRONT, vm.round.value.phase)

            passSeconds(1)
            assertEquals(DuoPhase.BACK, vm.round.value.phase)
        }

    @Test
    fun `a tilt on the front shows the back early`() =
        runTest {
            val vm = game()
            vm.start()

            vm.onGesture(TiltGesture.UP)

            assertEquals(DuoPhase.BACK, vm.round.value.phase)
        }

    @Test
    fun `down marks right and up marks wrong, and the next card starts a fresh countdown`() =
        runTest {
            val vm = game(seconds = 5, cards = 3)
            vm.start()
            passSeconds(2)
            vm.reveal()

            vm.onGesture(TiltGesture.DOWN)
            assertEquals(1, vm.round.value.correctCount)
            assertEquals(5, vm.round.value.secondsLeft)
            assertEquals(
                "front 2",
                vm.round.value.current
                    ?.front,
            )

            vm.reveal()
            vm.onGesture(TiltGesture.UP)
            assertEquals(
                listOf("front 2"),
                vm.round.value.missed
                    .map { it.front },
            )
        }

    @Test
    fun `marking a card plays the right or wrong sound and buzz`() =
        runTest {
            val vm = game(cards = 2)
            vm.start()

            vm.reveal()
            vm.onGesture(TiltGesture.DOWN)
            vm.reveal()
            vm.onGesture(TiltGesture.UP)

            assertEquals(listOf(true, false), sounds.played)
            assertEquals(listOf(true, false), vibration.buzzed)
        }

    @Test
    fun `revealing a card buzzes once but plays no sound`() =
        runTest {
            val vm = game()
            vm.start()

            vm.reveal()

            assertEquals(1, vibration.reveals)
            assertTrue(sounds.played.isEmpty())
            assertTrue(vibration.buzzed.isEmpty())
        }

    @Test
    fun `the countdown running out buzzes once as time up, not as a reveal`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()

            passSeconds(3)

            assertEquals(DuoPhase.BACK, vm.round.value.phase)
            assertEquals(1, vibration.timeUps)
            assertEquals(0, vibration.reveals)
        }

    @Test
    fun `revealing early means no time up buzz later`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()

            vm.reveal()
            passSeconds(5)

            assertEquals(0, vibration.timeUps)
        }

    @Test
    fun `a paused countdown does not buzz`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()
            vm.pause()

            passSeconds(5)

            assertEquals(0, vibration.timeUps)
        }

    @Test
    fun `revealing while paused does not buzz`() =
        runTest {
            val vm = game()
            vm.start()
            vm.pause()

            vm.reveal()

            assertEquals(0, vibration.reveals)
        }

    @Test
    fun `finishing the last card reports the score once`() =
        runTest {
            val vm = game(cards = 2)
            vm.start()

            vm.reveal()
            vm.onGesture(TiltGesture.DOWN)
            vm.reveal()
            vm.onGesture(TiltGesture.UP)

            assertEquals(DuoPhase.FINISHED, vm.round.value.phase)
            assertEquals(listOf(1), vm.finishedScores)
        }

    @Test
    fun `pausing holds the countdown and resuming carries on from the same second`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()
            passSeconds(1)

            vm.pause()
            passSeconds(10)
            assertTrue(vm.round.value.isPaused)
            assertEquals(2, vm.round.value.secondsLeft)
            assertEquals(DuoPhase.FRONT, vm.round.value.phase)

            vm.resume()
            assertFalse(vm.round.value.isPaused)
            passSeconds(2)
            assertEquals(DuoPhase.BACK, vm.round.value.phase)
        }

    @Test
    fun `revealing is ignored while paused`() =
        runTest {
            val vm = game()
            vm.start()
            vm.pause()

            vm.reveal()
            vm.onGesture(TiltGesture.DOWN)

            assertEquals(DuoPhase.FRONT, vm.round.value.phase)
        }

    @Test
    fun `pausing only works while a front is showing`() =
        runTest {
            val vm = game()

            vm.pause()
            assertFalse(vm.round.value.isPaused)

            vm.start()
            vm.reveal()
            vm.pause()
            assertFalse(vm.round.value.isPaused)
        }

    @Test
    fun `a link that is down holds the countdown and bringing it back carries on`() =
        runTest {
            val vm = game(seconds = 3)
            vm.start()

            vm.linkDown(true)
            passSeconds(10)
            assertTrue(vm.round.value.isPaused)
            assertEquals(3, vm.round.value.secondsLeft)

            vm.linkDown(false)
            passSeconds(3)
            assertEquals(DuoPhase.BACK, vm.round.value.phase)
        }

    @Test
    fun `the countdown stays held until both the player and the link allow it`() =
        runTest {
            val vm = game()
            vm.start()
            vm.pause()
            vm.linkDown(true)

            vm.linkDown(false)
            assertTrue("the player has not resumed yet", vm.round.value.isPaused)

            vm.resume()
            assertFalse(vm.round.value.isPaused)
        }

    @Test
    fun `a card that comes up while the link is down starts held`() =
        runTest {
            val vm = game(seconds = 3, cards = 3)
            vm.start()
            vm.reveal()
            vm.linkDown(true)

            vm.onGesture(TiltGesture.DOWN)
            passSeconds(10)

            assertTrue(vm.round.value.isPaused)
            assertEquals(DuoPhase.FRONT, vm.round.value.phase)

            vm.linkDown(false)
            passSeconds(3)
            assertEquals(DuoPhase.BACK, vm.round.value.phase)
        }

    @Test
    fun `loading cards again starts a clean round`() =
        runTest {
            val vm = game()
            vm.start()
            vm.pause()

            vm.load(testCards(5))

            assertEquals(DuoPhase.READY, vm.round.value.phase)
            assertFalse(vm.round.value.isPaused)
            assertEquals(5, vm.round.value.totalCount)
        }

    @Test
    fun `starting with a countdown shows 3, 2, 1 and then the first card`() =
        runTest {
            val vm = game(seconds = 5, cards = 2)

            vm.startWithCountdown()
            assertEquals(3, vm.round.value.startCountdown)
            assertEquals(DuoPhase.READY, vm.round.value.phase)

            passSeconds(1)
            assertEquals(2, vm.round.value.startCountdown)
            passSeconds(1)
            assertEquals(1, vm.round.value.startCountdown)
            passSeconds(1)

            assertEquals(0, vm.round.value.startCountdown)
            assertEquals(DuoPhase.FRONT, vm.round.value.phase)
            assertEquals(5, vm.round.value.secondsLeft)
        }

    @Test
    fun `a deck with no cards is finished straight away`() {
        val vm = game(cards = 0)

        vm.start()

        assertEquals(DuoPhase.FINISHED, vm.round.value.phase)
    }
}
