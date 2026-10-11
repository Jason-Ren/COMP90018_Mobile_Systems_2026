package com.comp90018.flashcards.ui.lobby

import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.debug.DebugSettings
import com.comp90018.flashcards.testutil.FakeConnectionController
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.keepCollecting
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LobbyViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val debug = DebugSettings()

    private fun TestScope.viewModel(): LobbyViewModel {
        val vm = LobbyViewModel(connection, debug)
        keepCollecting(vm.state)
        return vm
    }

    @Test
    fun `the lobby starts with a team name and an empty opponent slot`() =
        runTest {
            val state = viewModel().state.value

            assertTrue(state.teamName.startsWith("Team "))
            assertEquals(GuestSlot.Empty, state.guest)
            assertFalse(state.canStart)
            assertTrue(state.canReroll)
        }

    @Test
    fun `beginning starts hosting under the team name, only once`() =
        runTest {
            val vm = viewModel()

            vm.begin()
            vm.begin()

            assertEquals(listOf(vm.state.value.teamName), connection.hostedAs)
        }

    @Test
    fun `a request shows as pending and an accepted guest as joined`() =
        runTest {
            val vm = viewModel()
            vm.begin()

            connection.state.value = ConnectionState.Requested("Team B")
            assertEquals(GuestSlot.Pending("Team B"), vm.state.value.guest)
            assertFalse(vm.state.value.canStart)

            connection.state.value = ConnectionState.Connected("Team B")
            assertEquals(GuestSlot.Joined("Team B"), vm.state.value.guest)
            assertTrue(vm.state.value.canStart)
        }

    @Test
    fun `a guest stays in the slot while the link is being recovered`() =
        runTest {
            val vm = viewModel()
            vm.begin()

            connection.state.value = ConnectionState.Reconnecting("Team B")
            assertEquals(GuestSlot.Joined("Team B"), vm.state.value.guest)

            connection.state.value = ConnectionState.Lost("Team B")
            assertEquals(GuestSlot.Joined("Team B"), vm.state.value.guest)
            assertEquals(ConnectionState.Lost("Team B"), vm.state.value.connection)
        }

    @Test
    fun `a state left over from an earlier session is ignored until this lobby begins`() =
        runTest {
            connection.state.value = ConnectionState.PeerEnded
            val vm = viewModel()
            assertEquals(ConnectionState.Idle, vm.state.value.connection)

            // hosting resets the link, so the old state is gone for good
            vm.begin()
            assertEquals(ConnectionState.Advertising, vm.state.value.connection)
        }

    @Test
    fun `accept, decline, remove and reconnect are passed on to the connection`() =
        runTest {
            val vm = viewModel()

            vm.accept()
            vm.decline()
            vm.removeGuest()
            vm.reconnect()

            assertEquals(1, connection.accepts)
            assertEquals(1, connection.rejects)
            assertEquals(1, connection.removals)
            assertEquals(1, connection.reconnects)
        }

    @Test
    fun `cancelling ends the session`() =
        runTest {
            viewModel().cancel()

            assertEquals(1, connection.endSessions)
        }

    @Test
    fun `rerolling picks a new name and advertises it while waiting`() =
        runTest {
            val vm = viewModel()
            vm.begin()

            vm.rerollName()

            assertEquals(2, connection.hostedAs.size)
            assertEquals(vm.state.value.teamName, connection.hostedAs.last())
        }

    @Test
    fun `rerolling is locked once a guest has asked to join`() =
        runTest {
            val vm = viewModel()
            vm.begin()
            connection.state.value = ConnectionState.Requested("Team B")
            val before = vm.state.value.teamName

            vm.rerollName()

            assertFalse(vm.state.value.canReroll)
            assertEquals(before, vm.state.value.teamName)
            assertEquals(1, connection.hostedAs.size)
        }
}
