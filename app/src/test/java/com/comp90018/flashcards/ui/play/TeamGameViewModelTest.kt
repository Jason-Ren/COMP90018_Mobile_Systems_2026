package com.comp90018.flashcards.ui.play

import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.data.p2p.GameSetup
import com.comp90018.flashcards.debug.DebugSettings
import com.comp90018.flashcards.testutil.FakeAnswerSounds
import com.comp90018.flashcards.testutil.FakeConnectionController
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.passMillis
import com.comp90018.flashcards.testutil.passSeconds
import com.comp90018.flashcards.testutil.testCards
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TeamGameViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val debug = DebugSettings()
    private val session = GameSession()

    private fun viewModel(
        cards: Int = 2,
        seconds: Int = 10,
    ): TeamGameViewModel {
        session.setup = GameSetup("Capitals", seconds, testCards(cards))
        return TeamGameViewModel(connection, session, debug, FakeAnswerSounds())
    }

    // plays every card, marking each one right
    private fun TeamGameViewModel.playAllRight() {
        start()
        while (uiState.value.round.phase == DuoPhase.FRONT) {
            reveal()
            onGesture(TiltGesture.DOWN)
        }
    }

    @Test
    fun `the game starts on the ready screen with the deck from the setup`() {
        val round = viewModel(cards = 4).uiState.value.round

        assertEquals(DuoPhase.READY, round.phase)
        assertEquals("Capitals", round.deckName)
        assertEquals(4, round.totalCount)
    }

    @Test
    fun `without a setup there is no game to play`() {
        session.setup = null

        val vm = TeamGameViewModel(connection, session, debug, FakeAnswerSounds())

        assertFalse(vm.hasGame)
    }

    @Test
    fun `with a setup there is a game`() {
        assertTrue(viewModel().hasGame)
    }

    @Test
    fun `finishing the deck sends the score to the other team`() {
        val vm = viewModel(cards = 2)

        vm.playAllRight()

        assertEquals(listOf<GameMessage>(GameMessage.Finished(2)), connection.sent)
    }

    @Test
    fun `nothing is sent before the deck is finished`() {
        val vm = viewModel(cards = 2)

        vm.start()
        vm.reveal()
        vm.onGesture(TiltGesture.DOWN)

        assertTrue(connection.sent.isEmpty())
    }

    @Test
    fun `the other teams score shows up when it arrives`() {
        val vm = viewModel()
        assertNull(vm.uiState.value.opponentCorrect)

        connection.messages.tryEmit(GameMessage.Finished(1))

        assertEquals(1, vm.uiState.value.opponentCorrect)
    }

    @Test
    fun `a dropped link holds the countdown and the connection state is passed on`() =
        runTest {
            val vm = viewModel(seconds = 5)
            vm.start()

            connection.state.value = ConnectionState.Reconnecting("Team B")
            passSeconds(10)

            assertTrue(vm.uiState.value.round.isPaused)
            assertEquals(5, vm.uiState.value.round.secondsLeft)
            assertEquals(ConnectionState.Reconnecting("Team B"), vm.uiState.value.connection)
        }

    @Test
    fun `the countdown carries on when the link is back`() =
        runTest {
            val vm = viewModel(seconds = 3)
            vm.start()
            connection.state.value = ConnectionState.Lost("Team B")

            connection.state.value = ConnectionState.Connected("Team B")
            passSeconds(3)

            assertEquals(DuoPhase.BACK, vm.uiState.value.round.phase)
        }

    @Test
    fun `the score is sent again when the link comes back after the deck was finished`() {
        val vm = viewModel(cards = 1)
        vm.playAllRight()
        connection.state.value = ConnectionState.Reconnecting("Team B")

        connection.state.value = ConnectionState.Connected("Team B")

        assertEquals(listOf<GameMessage>(GameMessage.Finished(1), GameMessage.Finished(1)), connection.sent.takeLast(2))
    }

    @Test
    fun `leaving ends the session`() {
        val vm = viewModel()

        vm.leave()

        assertEquals(1, connection.endSessions)
    }

    @Test
    fun `reconnect is passed on to the connection`() {
        val vm = viewModel()

        vm.reconnect()

        assertEquals(1, connection.reconnects)
    }

    @Test
    fun `in emulator mode the real link is ignored`() {
        debug.setEmulatorMode(true)
        val vm = viewModel()

        connection.state.value = ConnectionState.Lost("Team B")

        assertEquals(ConnectionState.Idle, vm.uiState.value.connection)
        assertFalse(vm.uiState.value.round.isPaused)
    }

    @Test
    fun `in emulator mode a simulated drop shows and holds the game`() {
        debug.setEmulatorMode(true)
        val vm = viewModel()
        vm.start()

        debug.simulatePeerEnded()

        assertEquals(ConnectionState.PeerEnded, vm.uiState.value.connection)
        assertTrue(vm.uiState.value.round.isPaused)
    }

    @Test
    fun `in emulator mode the other teams score is made up after finishing`() =
        runTest {
            debug.setEmulatorMode(true)
            val vm = viewModel(cards = 3)

            vm.playAllRight()
            assertNull(vm.uiState.value.opponentCorrect)
            passMillis(1_500)

            val score = vm.uiState.value.opponentCorrect
            assertNotNull(score)
            assertTrue(score!! in 0..3)
        }

    @Test
    fun `leaving clears any simulated link`() {
        debug.setEmulatorMode(true)
        debug.simulatePeerEnded()
        val vm = viewModel()

        vm.leave()

        assertNull(debug.simulatedLink.value)
    }
}
