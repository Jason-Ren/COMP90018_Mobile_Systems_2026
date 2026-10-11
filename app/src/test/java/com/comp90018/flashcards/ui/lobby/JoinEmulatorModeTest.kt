package com.comp90018.flashcards.ui.lobby

import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.debug.DebugSettings
import com.comp90018.flashcards.testutil.FakeConnectionController
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.keepCollecting
import com.comp90018.flashcards.testutil.passMillis
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// the join screen with the debug emulator mode on, where a fake host stands in for a real phone
class JoinEmulatorModeTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val debug = DebugSettings().also { it.setEmulatorMode(true) }
    private val session = GameSession()
    private val fakeHost = DiscoveredHost("debug-host", "Team Plum")

    private fun TestScope.viewModel(): JoinGameViewModel {
        val vm = JoinGameViewModel(connection, debug, session)
        keepCollecting(vm.state)
        return vm
    }

    @Test
    fun `beginning does not start searching and shows the fake host`() =
        runTest {
            val vm = viewModel()

            vm.begin()

            assertTrue(connection.joinedAs.isEmpty())
            assertEquals(listOf(fakeHost), vm.state.value.hosts)
        }

    @Test
    fun `joining the fake host is accepted after a moment and then the game starts`() =
        runTest {
            val vm = viewModel()
            vm.begin()

            vm.join(fakeHost)
            assertEquals(ConnectionState.Requested("Team Plum"), vm.state.value.connection)
            assertTrue(connection.requested.isEmpty())

            passMillis(1_500)
            assertEquals(ConnectionState.Connected("Team Plum"), vm.state.value.connection)
            assertFalse(vm.state.value.hostStarted)

            passMillis(3_000)
            assertTrue(vm.state.value.hostStarted)
            assertEquals("Debug deck", session.setup!!.deckName)
            assertEquals(5, session.setup!!.cards.size)
        }

    @Test
    fun `the real link is ignored and a simulated one is shown`() =
        runTest {
            val vm = viewModel()
            connection.state.value = ConnectionState.Failed("nearby cannot run here")
            assertEquals(ConnectionState.Idle, vm.state.value.connection)

            debug.simulatePeerEnded()
            assertEquals(ConnectionState.PeerEnded, vm.state.value.connection)
        }

    @Test
    fun `reconnect is simulated instead of using the connection`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launch { debug.simulateDrop() }
            passMillis(4_000)

            vm.reconnect()
            passMillis(1_500)

            assertEquals(ConnectionState.Idle, vm.state.value.connection)
            assertEquals(0, connection.reconnects)
        }

    @Test
    fun `leaving clears the fake progress and any simulated link`() =
        runTest {
            val vm = viewModel()
            vm.begin()
            vm.join(fakeHost)
            passMillis(1_500)
            debug.simulatePeerEnded()

            vm.leave()

            assertNull(debug.simulatedLink.value)
            assertEquals(ConnectionState.Idle, vm.state.value.connection)
        }
}
