package com.comp90018.flashcards.data.social

import androidx.lifecycle.ViewModelStore
import com.comp90018.flashcards.testutil.MainDispatcherRule
import com.comp90018.flashcards.ui.social.FriendsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsRepositoryTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `missing username reports user not found and never sends a request`() =
        runTest {
            val fixture = Fixture(errorResponse(404, "user_not_found", "No profile has that username."))
            val error = runCatching { fixture.repository.addFriend("alice", "missing") }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("User not found", error?.message)
            assertEquals("user_not_found", (error?.cause as SocialApiException).code)
            assertFalse(fixture.transport.calls.any { it.method == "POST" })
        }

    @Test
    fun `other lookup errors keep their original code and message`() =
        runTest {
            val fixture = Fixture(errorResponse(404, "profile_required", "Create your profile first."))
            val error =
                runCatching {
                    fixture.repository.addFriend(
                        "alice",
                        "bob",
                    )
                }.exceptionOrNull() as SocialApiException

            assertEquals("profile_required", error.code)
            assertEquals("Create your profile first.", error.message)
            assertFalse(fixture.transport.calls.any { it.method == "POST" })
        }

    @Test
    fun `successful lookup trims the username and sends the resolved recipient`() =
        runTest {
            val fixture = Fixture(profileResponse("bob"))
            fixture.repository.addFriend("alice", " bob ")

            val lookup = fixture.transport.calls.single { it.method == "GET" }
            assertTrue(lookup.url.endsWith("/v1/users/lookup?username=bob"))
            val sent = fixture.transport.calls.single { it.method == "POST" }
            assertTrue(sent.url.endsWith("/v1/friend-requests"))
            assertEquals("""{"recipient_id":"bob"}""", sent.body)
        }

    @Test
    fun `missing user reaches the friend form task state and a retry clears it`() =
        runTest {
            val fixture = Fixture(errorResponse(404, "user_not_found", "No profile has that username."))
            val viewModel = FriendsViewModel(fixture.repository, fixture.session)
            val store = ViewModelStore().apply { put("friends", viewModel) }
            try {
                runCurrent()
                assertTrue(viewModel.uiState.value.loaded)

                viewModel.addFriend("missing")
                runCurrent()
                assertEquals("User not found", viewModel.taskState.value.error)
                assertFalse(viewModel.taskState.value.busy)
                assertFalse(fixture.transport.calls.any { it.method == "POST" })

                fixture.transport.lookup = profileResponse("bob")
                viewModel.addFriend("bob")
                runCurrent()
                assertEquals(null, viewModel.taskState.value.error)
                assertEquals("Friend request sent.", viewModel.taskState.value.message)
            } finally {
                store.clear()
            }
        }

    private class Fixture(
        lookup: SocialHttpResponse,
    ) {
        val session = FakeSocialSession()
        val transport = FriendTransport(lookup)
        val repository = FriendsRepository(SocialApi("https://example.test", session, transport))
    }

    private class FriendTransport(
        var lookup: SocialHttpResponse,
    ) : SocialTransport {
        val calls = mutableListOf<RecordedCall>()

        override suspend fun execute(
            url: String,
            method: String,
            token: String,
            body: String?,
        ): SocialHttpResponse {
            calls.add(RecordedCall(url, method, token, body))
            return when {
                "/v1/users/lookup?" in url -> lookup
                url.endsWith("/v1/me") -> profileResponse("alice")
                method == "POST" -> SocialHttpResponse(200, "{}")
                else -> SocialHttpResponse(200, "[]")
            }
        }
    }

    private fun errorResponse(
        status: Int,
        code: String,
        message: String,
    ) = SocialHttpResponse(status, """{"error":{"code":"$code","message":"$message"}}""")

    private companion object {
        fun profileResponse(uid: String) =
            SocialHttpResponse(200, socialJson.encodeToString(SocialProfile(uid, uid, uid)))
    }
}
