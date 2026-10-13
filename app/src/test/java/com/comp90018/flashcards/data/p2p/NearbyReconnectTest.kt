package com.comp90018.flashcards.data.p2p

import com.comp90018.flashcards.testutil.FakeTransport
import com.comp90018.flashcards.testutil.found
import com.comp90018.flashcards.testutil.guestConnectedTo
import com.comp90018.flashcards.testutil.hostConnectedTo
import com.comp90018.flashcards.testutil.info
import com.comp90018.flashcards.testutil.passMillis
import com.comp90018.flashcards.testutil.result
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// what happens when the link drops: retry for a few seconds, then give up
class NearbyReconnectTest {
    private val transport = FakeTransport()

    private fun TestScope.controller() = NearbyConnectionController(transport, backgroundScope)

    @Test
    fun `a dropped link starts reconnecting and the host advertises again`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")

            transport.connectionCallback.onDisconnected("e1")

            assertEquals(ConnectionState.Reconnecting("Team B"), controller.state.value)
            assertEquals(2, transport.advertiseCalls)
        }

    @Test
    fun `a dropped link makes the guest search again`() =
        runTest {
            val controller = controller()
            controller.guestConnectedTo(transport, "Team A")

            transport.connectionCallback.onDisconnected("e1")

            assertEquals(ConnectionState.Reconnecting("Team A"), controller.state.value)
            assertEquals(2, transport.discoverCalls)
        }

    @Test
    fun `a disconnect from some other endpoint is ignored`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")

            transport.connectionCallback.onDisconnected("somebody else")

            assertEquals(ConnectionState.Connected("Team B"), controller.state.value)
        }

    @Test
    fun `the same guest coming back is accepted without asking the host`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B", guestId = "g1")
            transport.connectionCallback.onDisconnected("e1")

            transport.connectionCallback.onConnectionInitiated("e9", info("g1|Team B"))

            assertEquals(listOf("e1", "e9"), transport.accepted)
            assertEquals(ConnectionState.Reconnecting("Team B"), controller.state.value)
        }

    @Test
    fun `a different guest is rejected while the host waits for the old one`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B", guestId = "g1")
            transport.connectionCallback.onDisconnected("e1")

            transport.connectionCallback.onConnectionInitiated("e5", info("zz|Team C"))

            assertEquals(listOf("e5"), transport.rejected)
        }

    @Test
    fun `a guest that finds the lost host goes straight back to it`() =
        runTest {
            val controller = controller()
            controller.guestConnectedTo(transport, "Team A", hostId = "h0")
            transport.connectionCallback.onDisconnected("e1")

            transport.discoveryCallback.onEndpointFound("e7", found("h0|Team A"))

            assertEquals("e7", transport.requests.last().second)
            assertTrue("the lost host should not be listed", controller.hosts.value.isEmpty())
        }

    @Test
    fun `other hosts found while reconnecting are only listed`() =
        runTest {
            val controller = controller()
            controller.guestConnectedTo(transport, "Team A", hostId = "h0")
            transport.connectionCallback.onDisconnected("e1")
            val requestsBefore = transport.requests.size

            transport.discoveryCallback.onEndpointFound("e8", found("other|Team Z"))

            assertEquals(requestsBefore, transport.requests.size)
            assertEquals(listOf(DiscoveredHost("e8", "Team Z")), controller.hosts.value)
        }

    @Test
    fun `reconnecting gives up and shows lost after eight seconds`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")
            transport.connectionCallback.onDisconnected("e1")
            runCurrent()

            passMillis(7_999)
            assertEquals(ConnectionState.Reconnecting("Team B"), controller.state.value)

            passMillis(1)
            assertEquals(ConnectionState.Lost("Team B"), controller.state.value)
        }

    @Test
    fun `getting the link back in time cancels the countdown to lost`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B", guestId = "g1")
            transport.connectionCallback.onDisconnected("e1")
            runCurrent()
            transport.connectionCallback.onConnectionInitiated("e9", info("g1|Team B"))

            transport.connectionCallback.onConnectionResult("e9", result(true))
            passMillis(20_000)

            assertEquals(ConnectionState.Connected("Team B"), controller.state.value)
        }

    @Test
    fun `a failed attempt while reconnecting is left to the countdown`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")
            transport.connectionCallback.onDisconnected("e1")

            transport.connectionCallback.onConnectionResult("e9", result(false))

            assertEquals(ConnectionState.Reconnecting("Team B"), controller.state.value)
        }

    @Test
    fun `reconnect from the lost screen tries again`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")
            transport.connectionCallback.onDisconnected("e1")
            runCurrent()
            passMillis(8_000)
            assertEquals(ConnectionState.Lost("Team B"), controller.state.value)

            controller.reconnect()

            assertEquals(ConnectionState.Reconnecting("Team B"), controller.state.value)
            assertEquals(3, transport.advertiseCalls)
        }

    @Test
    fun `reconnect does nothing unless the link is lost`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")

            controller.reconnect()

            assertEquals(ConnectionState.Connected("Team B"), controller.state.value)
        }
}
