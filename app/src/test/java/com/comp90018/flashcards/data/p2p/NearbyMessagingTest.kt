package com.comp90018.flashcards.data.p2p

import com.comp90018.flashcards.testutil.FakeTransport
import com.comp90018.flashcards.testutil.guestConnectedTo
import com.comp90018.flashcards.testutil.hostConnectedTo
import com.comp90018.flashcards.testutil.passMillis
import com.google.android.gms.nearby.connection.Payload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

// sending and receiving messages, and ending the session on purpose
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyMessagingTest {
    private val transport = FakeTransport()

    private fun TestScope.controller() = NearbyConnectionController(transport, backgroundScope)

    private fun TestScope.receive(message: GameMessage?) {
        transport.decoded = message
        transport.payloadCallback.onPayloadReceived("e1", mock<Payload>())
        // the payload is decoded in a launched coroutine, so let it run
        runCurrent()
    }

    @Test
    fun `a message goes to the connected phone`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)

            controller.send(GameMessage.Finished(2))

            assertEquals(listOf("e1" to GameMessage.Finished(2)), transport.sent)
        }

    @Test
    fun `nothing is sent when there is no connection`() =
        runTest {
            val controller = controller()

            controller.send(GameMessage.Finished(2))

            assertTrue(transport.sent.isEmpty())
        }

    @Test
    fun `a received message is passed on`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)
            val received = mutableListOf<GameMessage>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.messages.collect {
                    received +=
                        it
                }
            }

            receive(GameMessage.Finished(3))

            assertEquals(listOf<GameMessage>(GameMessage.Finished(3)), received)
        }

    @Test
    fun `a payload that cannot be read is ignored`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport, "Team B")

            receive(null)

            assertEquals(ConnectionState.Connected("Team B"), controller.state.value)
        }

    @Test
    fun `the other team ending the session skips reconnecting`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)

            receive(GameMessage.EndSession)
            passMillis(20_000)

            assertEquals(ConnectionState.PeerEnded, controller.state.value)
            assertTrue(transport.disconnectAlls > 0)
        }

    @Test
    fun `the end of session message is not passed on to the game`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)
            val received = mutableListOf<GameMessage>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.messages.collect {
                    received +=
                        it
                }
            }

            receive(GameMessage.EndSession)

            assertTrue(received.isEmpty())
        }

    @Test
    fun `ending the session says goodbye, then disconnects and goes idle`() =
        runTest {
            val controller = controller()
            controller.guestConnectedTo(transport)

            controller.endSession()
            runCurrent()
            assertEquals(listOf("e1" to GameMessage.EndSession), transport.sent)
            assertTrue(controller.state.value is ConnectionState.Connected)

            passMillis(300)
            assertEquals(ConnectionState.Idle, controller.state.value)
            assertTrue(transport.disconnectAlls > 0)
        }

    @Test
    fun `ending a session that never connected just goes idle`() =
        runTest {
            val controller = controller()
            controller.startHosting("Team A")

            controller.endSession()
            runCurrent()

            assertEquals(ConnectionState.Idle, controller.state.value)
            assertTrue(transport.sent.isEmpty())
        }

    @Test
    fun `nothing is sent after the session has ended`() =
        runTest {
            val controller = controller()
            controller.hostConnectedTo(transport)
            controller.endSession()
            passMillis(300)
            transport.sent.clear()

            controller.send(GameMessage.Finished(1))

            assertTrue(transport.sent.isEmpty())
        }
}
