package com.comp90018.flashcards.data.p2p

import androidx.lifecycle.LifecycleOwner
import com.comp90018.flashcards.testutil.FakeTransport
import com.comp90018.flashcards.testutil.guestConnectedTo
import com.comp90018.flashcards.testutil.hostConnectedTo
import com.comp90018.flashcards.testutil.info
import com.comp90018.flashcards.testutil.passMillis
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

// going to the background frees the connection, coming back starts searching again
class NearbyBackgroundTest {
    private val transport = FakeTransport()
    private val owner: LifecycleOwner = mock()

    private fun TestScope.controller() = NearbyConnectionController(transport, backgroundScope)

    @Test
    fun `a waiting host stops advertising in the background and resumes in the foreground`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            controller.onStop(owner)
            assertTrue(transport.stops > 0)
            assertEquals(ConnectionState.Advertising, controller.state.value)

            controller.onStart(owner)
            assertEquals(2, transport.advertiseCalls)
        }

    @Test
    fun `a searching guest stops in the background and searches again in the foreground`() =
        runTest {
            val controller = controller()
            controller.startJoining("Team B")

            controller.onStop(owner)
            controller.onStart(owner)

            assertTrue(transport.stops > 0)
            assertEquals(2, transport.discoverCalls)
        }

    @Test
    fun `coming to the foreground without having left does nothing`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            controller.onStart(owner)

            assertEquals(1, transport.advertiseCalls)
        }

    @Test
    fun `an unanswered request is dropped in the background`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")
            transport.connectionCallback.onConnectionInitiated("e1", info("g1|Team B"))

            controller.onStop(owner)

            assertEquals(ConnectionState.Advertising, controller.state.value)
            assertTrue(transport.disconnectAlls > 0)

            controller.onStart(owner)
            assertEquals(2, transport.advertiseCalls)
        }

    @Test
    fun `a connected host loses the link in the background`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")

            controller.onStop(owner)

            assertEquals(ConnectionState.Lost("Team B"), controller.state.value)
            assertTrue(transport.disconnectAlls > 0)
        }

    @Test
    fun `a connected guest loses the link in the background`() =
        runTest {
            val controller = controller()
            controller.guestConnectedTo(transport, "Team A")

            controller.onStop(owner)

            assertEquals(ConnectionState.Lost("Team A"), controller.state.value)
        }

    @Test
    fun `going to the background while reconnecting gives up at once and stays lost`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")
            transport.connectionCallback.onDisconnected("e1")
            runCurrent()

            controller.onStop(owner)
            passMillis(20_000)

            assertEquals(ConnectionState.Lost("Team B"), controller.state.value)
        }

    @Test
    fun `the foreground does not restart a connection that was lost`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")
            controller.onStop(owner)
            val advertisesBefore = transport.advertiseCalls

            controller.onStart(owner)

            assertEquals(advertisesBefore, transport.advertiseCalls)
            assertEquals(ConnectionState.Lost("Team B"), controller.state.value)
        }

    @Test
    fun `an idle controller ignores the background`() =
        runTest {
            val controller = controller()

            controller.onStop(owner)
            controller.onStart(owner)

            assertEquals(ConnectionState.Idle, controller.state.value)
            assertEquals(0, transport.stops)
        }
}
