package com.comp90018.flashcards.data.p2p

import com.comp90018.flashcards.testutil.FakeTransport
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

// the host's half of the connection: advertising, answering requests, removing the guest
class NearbyHostTest {
    private val transport = FakeTransport()

    private fun TestScope.controller() = NearbyConnectionController(transport, backgroundScope)

    @Test
    fun `hosting advertises under the team name and waits`() =
        runTest {
            val controller = controller()

            controller.startHosting("Team A")

            assertEquals(ConnectionState.Advertising, controller.state.value)
            assertEquals(1, transport.advertiseCalls)
            assertTrue(transport.advertiseNames.single().endsWith("|Team A"))
        }

    @Test
    fun `a request waits for the host to answer`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))

            assertEquals(ConnectionState.Requested("Team B"), controller.state.value)
            assertTrue(transport.accepted.isEmpty())
        }

    @Test
    fun `accepting a request and nearby confirming it connects`() =
        runTest {
            val controller = controller()

            controller.hostConnectedTo(transport, "Team B")

            assertEquals(listOf("e1"), transport.accepted)
            assertEquals(ConnectionState.Connected("Team B"), controller.state.value)
            assertTrue("advertising should stop once connected", transport.stops > 0)
        }

    @Test
    fun `accept does nothing when nobody has asked`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            controller.accept()

            assertTrue(transport.accepted.isEmpty())
            assertEquals(ConnectionState.Advertising, controller.state.value)
        }

    @Test
    fun `a second request is rejected while one is waiting`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")
            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))

            transport.connectionCallback.onConnectionInitiated("e2", info("g2|Team C"))

            assertEquals(listOf("e2"), transport.rejected)
            assertEquals(ConnectionState.Requested("Team B"), controller.state.value)
        }

    @Test
    fun `declining rejects and goes back to waiting for someone else`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")
            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))

            controller.reject()
            transport.connectionCallback.onConnectionInitiated("e2", info("g2|Team C"))

            assertEquals(listOf("e1"), transport.rejected)
            assertEquals(ConnectionState.Requested("Team C"), controller.state.value)
        }

    @Test
    fun `the requester leaving before an answer puts the host back to waiting`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")
            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))

            transport.connectionCallback.onDisconnected("e1")

            assertEquals(ConnectionState.Advertising, controller.state.value)
        }

    @Test
    fun `a failed handshake puts the host back to waiting`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")
            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))
            controller.accept()

            transport.connectionCallback.onConnectionResult("e1", result(false))

            assertEquals(ConnectionState.Advertising, controller.state.value)
        }

    @Test
    fun `removing the guest tells them, disconnects and advertises again`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)

            controller.removePeer()
            assertEquals(ConnectionState.Advertising, controller.state.value)
            runCurrent()
            assertEquals(listOf("e1" to GameMessage.EndSession), transport.sent)
            assertTrue("the goodbye needs a moment to be sent first", transport.disconnected.isEmpty())

            passMillis(300)
            assertEquals(listOf("e1"), transport.disconnected)
            assertEquals(2, transport.advertiseCalls)
        }

    @Test
    fun `a failed start is reported with a friendly reason`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            transport.advertiseFailure!!.invoke(IllegalStateException("boom"))

            assertEquals(
                ConnectionState.Failed(
                    "Couldn't connect to nearby phones\n" +
                        "Check Bluetooth and Wi-Fi are on, then try again\n[IllegalStateException]",
                ),
                controller.state.value,
            )
        }
}
