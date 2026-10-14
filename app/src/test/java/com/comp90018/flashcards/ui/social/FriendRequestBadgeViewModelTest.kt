package com.comp90018.flashcards.ui.social

import com.comp90018.flashcards.data.auth.AuthRepository
import com.comp90018.flashcards.data.auth.AuthState
import com.comp90018.flashcards.data.auth.SignedInUser
import com.comp90018.flashcards.data.social.FakeSocialSession
import com.comp90018.flashcards.data.social.FriendsRepository
import com.comp90018.flashcards.data.social.RecordingTransport
import com.comp90018.flashcards.data.social.SocialApi
import com.comp90018.flashcards.data.social.SocialHttpResponse
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialRequest
import com.comp90018.flashcards.data.social.socialJson
import com.comp90018.flashcards.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestBadgeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `foreground polling finds new requests and stops without subscribers`() =
        runTest {
            val fixture = Fixture()
            val subscription = watch(fixture.viewModel)
            runCurrent()
            assertEquals(0, fixture.viewModel.incomingCount.value)

            fixture.requests = listOf(request("one"), request("two"))
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(2, fixture.viewModel.incomingCount.value)
            assertTrue(fixture.transport.calls.all { "direction=incoming&status=pending" in it.url })

            subscription.cancel()
            runCurrent()
            val calls = fixture.transport.calls.size
            advanceTimeBy(6_000)
            runCurrent()
            assertEquals(calls, fixture.transport.calls.size)
        }

    @Test
    fun `accept and decline clear the badge immediately while outgoing cancellation does not`() =
        runTest {
            val fixture = Fixture().apply { requests = listOf(request("one"), request("two")) }
            watch(fixture.viewModel)
            runCurrent()
            assertEquals(2, fixture.viewModel.incomingCount.value)

            fixture.repository.respond("alice", "outgoing", "cancel")
            runCurrent()
            assertEquals(2, fixture.viewModel.incomingCount.value)
            fixture.repository.respond("alice", "one", "accept")
            runCurrent()
            assertEquals(1, fixture.viewModel.incomingCount.value)
            fixture.repository.respond("alice", "two", "decline")
            runCurrent()
            assertEquals(0, fixture.viewModel.incomingCount.value)
        }

    @Test
    fun `network errors preserve the last count and a later poll recovers`() =
        runTest {
            val fixture = Fixture().apply { requests = listOf(request("one")) }
            watch(fixture.viewModel)
            runCurrent()
            fixture.offline = true
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(1, fixture.viewModel.incomingCount.value)

            fixture.offline = false
            fixture.requests = emptyList()
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(0, fixture.viewModel.incomingCount.value)
        }

    @Test
    fun `account switches and sign out cannot display another users badge`() =
        runTest {
            val fixture = Fixture().apply { requests = listOf(request("one"), request("two")) }
            watch(fixture.viewModel)
            runCurrent()
            assertEquals(2, fixture.viewModel.incomingCount.value)

            fixture.session.uid = "bob"
            fixture.requests = emptyList()
            fixture.auth.value = signedIn("bob")
            runCurrent()
            assertEquals(0, fixture.viewModel.incomingCount.value)

            fixture.requests = listOf(request("bob-request"))
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(1, fixture.viewModel.incomingCount.value)
            fixture.session.uid = null
            fixture.auth.value = AuthState.SignedOut
            runCurrent()
            assertEquals(0, fixture.viewModel.incomingCount.value)
            val calls = fixture.transport.calls.size
            advanceTimeBy(6_000)
            runCurrent()
            assertEquals(calls, fixture.transport.calls.size)
        }

    private fun TestScope.watch(viewModel: FriendRequestBadgeViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.incomingCount.collect {} }

    private class Fixture {
        val session = FakeSocialSession()
        val auth = MutableStateFlow<AuthState>(signedIn("alice"))
        var requests = emptyList<SocialRequest>()
        var offline = false
        val transport =
            RecordingTransport {
                if (offline) throw IOException("Offline")
                SocialHttpResponse(200, socialJson.encodeToString(requests))
            }
        val repository = FriendsRepository(SocialApi("https://example.test", session, transport))
        private val authRepository = mock<AuthRepository>()

        init {
            whenever(authRepository.authState).thenReturn(auth)
        }

        val viewModel = FriendRequestBadgeViewModel(repository, authRepository)
    }

    companion object {
        private fun signedIn(uid: String) = AuthState.SignedIn(SignedInUser(uid, uid, null))

        private fun request(id: String) =
            SocialRequest(
                id,
                SocialProfile("sender", "sender", "Sender"),
                SocialProfile("recipient", "recipient", "Recipient"),
                "pending",
            )
    }
}
