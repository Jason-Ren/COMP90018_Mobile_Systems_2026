package com.comp90018.flashcards.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkThemeFromLuxTest {
    @Test
    fun `the first reading uses the midpoint so indoor light is not stuck on light theme`() {
        assertTrue(darkThemeForLux(lux = 10f, currentlyDark = false, firstReading = true, 20f, 60f))
        assertTrue(darkThemeForLux(lux = 40f, currentlyDark = false, firstReading = true, 20f, 60f))
        assertFalse(darkThemeForLux(lux = 41f, currentlyDark = true, firstReading = true, 20f, 60f))
    }

    @Test
    fun `later readings stay put inside the hysteresis band`() {
        assertTrue(darkThemeForLux(lux = 40f, currentlyDark = true, firstReading = false, 20f, 60f))
        assertFalse(darkThemeForLux(lux = 40f, currentlyDark = false, firstReading = false, 20f, 60f))
    }

    @Test
    fun `later readings switch only after crossing the dark or light threshold`() {
        assertTrue(darkThemeForLux(lux = 19f, currentlyDark = false, firstReading = false, 20f, 60f))
        assertFalse(darkThemeForLux(lux = 61f, currentlyDark = true, firstReading = false, 20f, 60f))
    }
}
