package com.comp90018.flashcards.debug

import com.comp90018.flashcards.data.p2p.ConnectionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class DebugSettingsTest {
    private val peer = "Team Plum"

    @Test
    fun `emulator mode starts off and can be switched on and off`() {
        assumeTrue(DEBUG_TOOLS_ENABLED)
        val settings = DebugSettings()
        assertFalse(settings.emulatorMode.value)

        settings.setEmulatorMode(true)
        assertTrue(settings.emulatorMode.value)

        settings.setEmulatorMode(false)
        assertFalse(settings.emulatorMode.value)
    }

    @Test
    fun `no link is simulated at first`() {
        assertNull(DebugSettings().simulatedLink.value)
    }

    @Test
    fun `a drop shows reconnecting and then lost after the grace period`() =
        runTest {
            val settings = DebugSettings()

            backgroundScope.launch { settings.simulateDrop() }
            runCurrent()
            assertEquals(ConnectionState.Reconnecting(peer), settings.simulatedLink.value)

            advanceTimeBy(3_999)
            runCurrent()
            assertEquals(ConnectionState.Reconnecting(peer), settings.simulatedLink.value)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(ConnectionState.Lost(peer), settings.simulatedLink.value)
        }

    @Test
    fun `clearing during a drop stops it turning into lost`() =
        runTest {
            val settings = DebugSettings()
            backgroundScope.launch { settings.simulateDrop() }
            runCurrent()

            settings.clearSimulatedLink()
            advanceTimeBy(10_000)
            runCurrent()

            assertNull(settings.simulatedLink.value)
        }

    @Test
    fun `reconnect tries for a moment and then the link is back`() =
        runTest {
            val settings = DebugSettings()

            backgroundScope.launch { settings.simulateReconnect() }
            runCurrent()
            assertEquals(ConnectionState.Reconnecting(peer), settings.simulatedLink.value)

            advanceTimeBy(1_500)
            runCurrent()
            assertNull(settings.simulatedLink.value)
        }

    @Test
    fun `the other team leaving is simulated at once and can be cleared`() {
        val settings = DebugSettings()

        settings.simulatePeerEnded()
        assertEquals(ConnectionState.PeerEnded, settings.simulatedLink.value)

        settings.clearSimulatedLink()
        assertNull(settings.simulatedLink.value)
    }
}
