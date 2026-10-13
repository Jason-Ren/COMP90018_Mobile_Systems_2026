package com.comp90018.flashcards.data.p2p

import com.comp90018.flashcards.testutil.FakeTransport
import com.comp90018.flashcards.testutil.found
import com.comp90018.flashcards.testutil.guestConnectedTo
import com.comp90018.flashcards.testutil.info
import com.comp90018.flashcards.testutil.result
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// the guest's half of the connection: finding hosts and asking to join one
class NearbyGuestTest {
    private val transport = FakeTransport()

    private fun TestScope.controller() = NearbyConnectionController(transport, backgroundScope)

    @Test
    fun `joining starts searching`() =
        runTest {
            val controller = controller()

            controller.startJoining("Team B")

            assertEquals(ConnectionState.Discovering, controller.state.value)
            assertEquals(1, transport.discoverCalls)
        }

    @Test
    fun `hosts that are found are listed once with their team names`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")

            transport.discoveryCallback.onEndpointFound("h1", found("x1|Team A"))
            transport.discoveryCallback.onEndpointFound("h1", found("x1|Team A"))
            transport.discoveryCallback.onEndpointFound("h2", found("x2|Team Z"))

            assertEquals(
                listOf(DiscoveredHost("h1", "Team A"), DiscoveredHost("h2", "Team Z")),
                controller.hosts.value,
            )
        }

    @Test
    fun `a host that goes away is removed from the list`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")
            transport.discoveryCallback.onEndpointFound("h1", found("x1|Team A"))

            transport.discoveryCallback.onEndpointLost("h1")

            assertTrue(controller.hosts.value.isEmpty())
        }

    @Test
    fun `asking to join sends a request and waits`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")

            controller.requestToJoin(DiscoveredHost("h1", "Team A"))

            assertEquals(ConnectionState.Requested("Team A"), controller.state.value)
            val (name, endpoint) = transport.requests.single()
            assertEquals("h1", endpoint)
            assertTrue(name.endsWith("|Team B"))
        }

    @Test
    fun `a host cannot ask to join`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            controller.requestToJoin(DiscoveredHost("h1", "Team Z"))

            assertTrue(transport.requests.isEmpty())
            assertEquals(ConnectionState.Advertising, controller.state.value)
        }

    @Test
    fun `the guest accepts its own side as soon as the handshake starts`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")
            controller.requestToJoin(DiscoveredHost("h1", "Team A"))

            transport.connectionCallback.onConnectionInitiated("h1", info("x1|Team A"))

            assertEquals(listOf("h1"), transport.accepted)
        }

    @Test
    fun `a finished handshake connects the guest to the host`() =
        runTest {
            val controller = controller()

            controller.guestConnectedTo(transport, "Team A")

            assertEquals(ConnectionState.Connected("Team A"), controller.state.value)
            assertTrue("searching should stop once connected", transport.stops > 0)
        }

    @Test
    fun `a host that declines sends the guest back to searching`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")
            controller.requestToJoin(DiscoveredHost("h1", "Team A"))

            transport.connectionCallback.onConnectionResult("h1", result(false))

            assertEquals(ConnectionState.Discovering, controller.state.value)
        }

    @Test
    fun `a request that could not be sent sends the guest back to searching`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")
            controller.requestToJoin(DiscoveredHost("h1", "Team A"))

            transport.requestFailure!!.invoke(IllegalStateException("boom"))

            assertEquals(ConnectionState.Discovering, controller.state.value)
        }
}
