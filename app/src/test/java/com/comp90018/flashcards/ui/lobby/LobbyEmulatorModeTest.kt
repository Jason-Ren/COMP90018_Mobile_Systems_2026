package com.comp90018.flashcards.ui.lobby

import com.comp90018.flashcards.data.p2p.ConnectionState
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

// the lobby with the debug emulator mode on, where nearby is never used
class LobbyEmulatorModeTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val debug = DebugSettings().also { it.setEmulatorMode(true) }

    private fun TestScope.viewModel(): LobbyViewModel {
        val vm = LobbyViewModel(connection, debug)
        keepCollecting(vm.state)
        return vm
    }

    @Test
    fun `the debug button only shows in emulator mode`() =
        runTest {
            assertTrue(viewModel().state.value.showDebugButton)
            debug.setEmulatorMode(false)

            assertFalse(LobbyViewModel(connection, debug).state.value.showDebugButton)
        }

    @Test
    fun `beginning does not start hosting`() =
        runTest {
            viewModel().begin()

            assertTrue(connection.hostedAs.isEmpty())
        }

    @Test
    fun `the fake request can be accepted into a joined guest without the connection`() =
        runTest {
            val vm = viewModel()

            vm.debugSimulateRequest()
            assertEquals(GuestSlot.Pending("Team Kiwi"), vm.state.value.guest)

            vm.accept()
            assertEquals(GuestSlot.Joined("Team Kiwi"), vm.state.value.guest)
            assertEquals(0, connection.accepts)
        }

    @Test
    fun `declining or removing the fake guest empties the slot without the connection`() =
        runTest {
            val vm = viewModel()
            vm.debugSimulateRequest()

            vm.decline()
            assertEquals(GuestSlot.Empty, vm.state.value.guest)

            vm.debugSimulateRequest()
            vm.accept()
            vm.removeGuest()
            assertEquals(GuestSlot.Empty, vm.state.value.guest)
            assertEquals(0, connection.rejects + connection.removals)
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
            assertEquals(ConnectionState.Reconnecting("Team Plum"), vm.state.value.connection)

            passMillis(1_500)
            assertEquals(ConnectionState.Idle, vm.state.value.connection)
            assertEquals(0, connection.reconnects)
        }

    @Test
    fun `cancelling clears the fake guest and any simulated link`() =
        runTest {
            val vm = viewModel()
            vm.debugSimulateRequest()
            debug.simulatePeerEnded()

            vm.cancel()

            assertEquals(GuestSlot.Empty, vm.state.value.guest)
            assertNull(debug.simulatedLink.value)
        }
}
