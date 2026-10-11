package com.comp90018.flashcards.data.p2p

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NearbyHelpersTest {
    private val generic = "Something went wrong starting nearby\nTry again"

    @Test
    fun `the endpoint name splits into id and team`() {
        assertEquals("a1b2c3d4", idOf("a1b2c3d4|Team Kiwi"))
        assertEquals("Team Kiwi", teamOf("a1b2c3d4|Team Kiwi"))
    }

    @Test
    fun `a team name that contains the separator is kept whole`() {
        assertEquals("id", idOf("id|Team|Two"))
        assertEquals("Team|Two", teamOf("id|Team|Two"))
    }

    @Test
    fun `a name without a separator is used as it is`() {
        assertEquals("plain", idOf("plain"))
        assertEquals("plain", teamOf("plain"))
    }

    // nearby reports errors as an ApiException carrying a status code, the status is a mock because it needs android
    private fun reasonFor(code: Int): String =
        friendlyReason(
            ApiException(
                mock<Status> {
                    on { statusCode } doReturn
                        code
                },
            ),
        )

    @Test
    fun `bluetooth errors say bluetooth is off`() {
        assertEquals("Bluetooth is off or not available\nTurn it on and try again", reasonFor(8007))
    }

    @Test
    fun `location off says location is off`() {
        assertEquals("Location is off\nTurn it on and try again", reasonFor(8025))
    }

    @Test
    fun `every missing permission code asks for permissions`() {
        val expected = "Some permissions are missing\nAllow them in the app settings and try again"

        listOf(8029, 8032, 8036, 8039).forEach { assertEquals("code $it", expected, reasonFor(it)) }
    }

    @Test
    fun `codes just outside the permission range are not treated as permissions`() {
        assertEquals(generic, reasonFor(8028))
        assertEquals(generic, reasonFor(8040))
    }

    @Test
    fun `anything else gets the general message`() {
        assertEquals(generic, reasonFor(8000))
        assertEquals(generic, friendlyReason(IllegalStateException("boom")))
    }
}
