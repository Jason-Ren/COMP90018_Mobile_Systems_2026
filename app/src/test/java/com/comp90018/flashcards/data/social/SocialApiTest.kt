package com.comp90018.flashcards.data.social

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SocialApiTest {
    @Test
    fun `401 refreshes Firebase token once and preserves request body`() =
        runBlocking {
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    if (it ==
                        1
                    ) {
                        SocialHttpResponse(401, "")
                    } else {
                        SocialHttpResponse(200, "{}")
                    }
                }
            val api = SocialApi("https://example.test/", session, transport)
            val body = buildJsonObject { put("request_id", "stable-copy-request") }
            api.request("alice", "POST", "/v1/decks/source/copies", body)
            assertEquals(listOf(false, true), session.refreshes)
            assertEquals(listOf("cached-token", "fresh-token"), transport.calls.map { it.token })
            assertEquals(listOf(body.toString(), body.toString()), transport.calls.map { it.body })
            assertEquals("https://example.test/v1/decks/source/copies", transport.calls.first().url)
        }

    @Test
    fun `second unauthorized response stops retrying`() =
        runBlocking {
            val session = FakeSocialSession()
            val transport = RecordingTransport { SocialHttpResponse(401, "") }
            val error =
                runCatching { SocialApi("https://example.test", session, transport).request("alice", "GET", "/v1/me") }
            assertEquals(401, (error.exceptionOrNull() as SocialApiException).status)
            assertEquals(2, transport.calls.size)
        }

    @Test
    fun `account change cannot retry a write under another identity`() =
        runBlocking {
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    session.uid = "bob"
                    SocialHttpResponse(401, "")
                }
            val error =
                runCatching { SocialApi("https://example.test", session, transport).request("alice", "POST", "/v1/me") }
            assertTrue(error.exceptionOrNull() is IllegalStateException)
            assertEquals(1, transport.calls.size)
        }

    @Test
    fun `account change discards a successful response`() =
        runBlocking {
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    session.uid = "bob"
                    SocialHttpResponse(200, "private-data")
                }
            val response =
                runCatching { SocialApi("https://example.test", session, transport).request("alice", "GET", "/v1/me") }
            assertTrue(response.isFailure)
        }

    @Test
    fun `network failures do not silently resend mutations`() =
        runBlocking {
            val transport = RecordingTransport { throw IOException("Lost response") }
            val response =
                runCatching {
                    SocialApi("https://example.test", FakeSocialSession(), transport).request("alice", "POST", "/v1/me")
                }
            assertTrue(response.isFailure)
            assertEquals(1, transport.calls.size)
        }

    @Test
    fun `server errors retain actionable application codes without exposing HTML`() =
        runBlocking {
            val transport =
                RecordingTransport {
                    if (it ==
                        1
                    ) {
                        SocialHttpResponse(
                            404,
                            """{"error":{"code":"profile_required","message":"Create a profile."}}""",
                        )
                    } else {
                        SocialHttpResponse(503, "<html>internal proxy error</html>")
                    }
                }
            val api = SocialApi("https://example.test", FakeSocialSession(), transport)
            val first = runCatching { api.request("alice", "GET", "/v1/me") }.exceptionOrNull() as SocialApiException
            assertEquals("profile_required", first.code)
            val second = runCatching { api.request("alice", "GET", "/v1/me") }.exceptionOrNull() as SocialApiException
            assertFalse(second.message.orEmpty().contains("html"))
        }

    @Test(expected = IllegalArgumentException::class)
    fun `tokens cannot be sent to a plaintext endpoint`() {
        SocialApi("http://example.test", FakeSocialSession(), RecordingTransport { SocialHttpResponse(200, "") })
    }
}

internal class FakeSocialSession : SocialSession {
    override var uid: String? = "alice"
    val refreshes = mutableListOf<Boolean>()

    override suspend fun token(
        ownerId: String,
        refresh: Boolean,
    ): String {
        requireUser(ownerId)
        refreshes.add(refresh)
        return if (refresh) "fresh-token" else "cached-token"
    }
}

internal data class RecordedCall(
    val url: String,
    val method: String,
    val token: String,
    val body: String?,
)

internal class RecordingTransport(
    private val response: (Int) -> SocialHttpResponse,
) : SocialTransport {
    val calls = mutableListOf<RecordedCall>()

    override suspend fun execute(
        url: String,
        method: String,
        token: String,
        body: String?,
    ): SocialHttpResponse {
        calls.add(RecordedCall(url, method, token, body))
        return response(calls.size)
    }
}
