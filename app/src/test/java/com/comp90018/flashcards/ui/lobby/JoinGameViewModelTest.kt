package com.comp90018.flashcards.ui.lobby

import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.data.p2p.WireCard
import com.comp90018.flashcards.debug.DebugSettings
import com.comp90018.flashcards.testutil.FakeConnectionController
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.testutil.keepCollecting
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class JoinGameViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val connection = FakeConnectionController()
    private val debug = DebugSettings()
    private val session = GameSession()
    private val host = DiscoveredHost("h1", "Team A")

    private fun TestScope.viewModel(): JoinGameViewModel {
        val vm = JoinGameViewModel(connection, debug, session)
        keepCollecting(vm.state)
        return vm
    }

    @Test
    fun `beginning starts searching under the team name, only once`() =
        runTest {
            val vm = viewModel()

            vm.begin()
            vm.begin()

            assertEquals(listOf(vm.state.value.teamName), connection.joinedAs)
        }

    @Test
    fun `hosts that are found are listed`() =
        runTest {
            val vm = viewModel()
            vm.begin()

            connection.hosts.value = listOf(host)

            assertEquals(listOf(host), vm.state.value.hosts)
        }

    @Test
    fun `joining a host asks the connection to join it`() =
        runTest {
            viewModel().join(host)

            assertEquals(listOf(host), connection.requested)
        }

    @Test
    fun `the connection state is shown once searching has begun`() =
        runTest {
            connection.state.value = ConnectionState.PeerEnded
            val vm = viewModel()
            assertEquals(ConnectionState.Idle, vm.state.value.connection)

            vm.begin()
            connection.state.value = ConnectionState.Requested("Team A")

            assertEquals(ConnectionState.Requested("Team A"), vm.state.value.connection)
        }

    @Test
    fun `the host starting the game stores the setup and tells the screen`() =
        runTest {
            val vm = viewModel()
            assertFalse(vm.state.value.hostStarted)

            connection.messages.tryEmit(GameMessage.Start("Capitals", 30, listOf(WireCard("France", "Paris"))))

            assertTrue(vm.state.value.hostStarted)
            val setup = session.setup!!
            assertEquals("Capitals", setup.deckName)
            assertEquals(30, setup.secondsPerCard)
            assertEquals(listOf("France"), setup.cards.map { it.front })
        }

    @Test
    fun `other messages do not start the game`() =
        runTest {
            val vm = viewModel()

            connection.messages.tryEmit(GameMessage.Finished(1))

            assertFalse(vm.state.value.hostStarted)
            assertNull(session.setup)
        }

    @Test
    fun `leaving ends the session and forgets that the host started`() =
        runTest {
            val vm = viewModel()
            connection.messages.tryEmit(GameMessage.Start("D", 5, emptyList()))

            vm.leave()

            assertEquals(1, connection.endSessions)
            assertFalse(vm.state.value.hostStarted)
        }

    @Test
    fun `reconnect is passed on to the connection`() =
        runTest {
            viewModel().reconnect()

            assertEquals(1, connection.reconnects)
        }

    @Test
    fun `rerolling is locked while requested, but allowed when connected`() =
        runTest {
            val vm = viewModel()
            vm.begin()
            connection.state.value = ConnectionState.Requested("Team Host")

            assertFalse(vm.state.value.canReroll)

            connection.state.value = ConnectionState.Connected("Team Host")
            assertTrue(vm.state.value.canReroll)

            vm.rerollName()
            assertEquals(GameMessage.UpdateTeamName(vm.state.value.teamName), connection.sent.last())
        }

    @Test
    fun `receiving update team name message updates host team name`() =
        runTest {
            val vm = viewModel()
            vm.begin()
            connection.state.value = ConnectionState.Connected("Team Host")

            connection.messages.tryEmit(GameMessage.UpdateTeamName("Team SuperHost"))

            assertEquals("Team SuperHost", vm.state.value.hostTeamName)
        }
}
