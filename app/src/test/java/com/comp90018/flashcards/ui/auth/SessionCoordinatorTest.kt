package com.comp90018.flashcards.ui.auth

import com.comp90018.flashcards.data.account.AccountProfiles
import com.comp90018.flashcards.data.auth.AuthState
import com.comp90018.flashcards.data.auth.SignedInUser
import com.comp90018.flashcards.data.social.SocialApiException
import com.comp90018.flashcards.data.social.SocialProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCoordinatorTest {
    @Test
    fun `missing profile requires onboarding then profile save unlocks dashboard`() =
        runTest {
            val profiles = FakeAccountProfiles()
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.ONBOARDING, coordinator.session.value.stage)
            profiles.profiles["alice"] = profile("alice")
            profiles.profileChanges.value++
            runCurrent()
            assertEquals(SessionStage.READY, coordinator.session.value.stage)
            assertEquals(
                "Alice",
                coordinator.session.value.profile
                    ?.displayName,
            )
        }

    @Test
    fun `existing profile skips onboarding and reflects renamed display name`() =
        runTest {
            val profiles = FakeAccountProfiles().apply { this.profiles["alice"] = profile("alice") }
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.READY, coordinator.session.value.stage)
            profiles.profiles["alice"] = profile("alice").copy(displayName = "New name")
            profiles.profileChanges.value++
            runCurrent()
            assertEquals(
                "New name",
                coordinator.session.value.profile
                    ?.displayName,
            )
        }

    @Test
    fun `network failure does not skip onboarding for unknown accounts`() =
        runTest {
            val profiles = FakeAccountProfiles().apply { failure = IOException("offline") }
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.UNAVAILABLE, coordinator.session.value.stage)
            profiles.failure = null
            coordinator.retry()
            runCurrent()
            assertEquals(SessionStage.ONBOARDING, coordinator.session.value.stage)
        }

    @Test
    fun `cached profile keeps local decks available offline`() =
        runTest {
            val profiles =
                FakeAccountProfiles().apply {
                    cached["alice"] = profile("alice")
                    failure = IOException("offline")
                }
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.READY, coordinator.session.value.stage)
            assertEquals(
                "alice",
                coordinator.session.value.profile
                    ?.username,
            )
        }

    @Test
    fun `account switch cancels old profile request and clears previous user`() =
        runTest {
            val request = CompletableDeferred<Unit>()
            val profiles =
                FakeAccountProfiles().apply {
                    cached["alice"] = profile("alice")
                    waitForAlice = request
                }
            val auth = MutableStateFlow<AuthState>(signedIn("alice"))
            val coordinator = SessionCoordinator(auth, profiles, backgroundScope)
            runCurrent()
            assertEquals(
                "alice",
                coordinator.session.value.user
                    ?.uid,
            )
            auth.value = signedIn("bob")
            runCurrent()
            assertEquals(SessionStage.ONBOARDING, coordinator.session.value.stage)
            assertEquals(
                "bob",
                coordinator.session.value.user
                    ?.uid,
            )
            request.complete(Unit)
            runCurrent()
            assertEquals(
                "bob",
                coordinator.session.value.user
                    ?.uid,
            )
            auth.value = AuthState.SignedOut
            runCurrent()
            assertEquals(SessionStage.SIGNED_OUT, coordinator.session.value.stage)
            assertEquals(null, coordinator.session.value.profile)
        }

    @Test
    fun `pending deletion always routes to recovery instead of onboarding`() =
        runTest {
            val profiles = FakeAccountProfiles().apply { deleting.add("alice") }
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.DELETING, coordinator.session.value.stage)
            assertFalse(profiles.loaded)
        }

    @Test
    fun `server deletion barrier overrides cached profile and persists recovery`() =
        runTest {
            val profiles =
                FakeAccountProfiles().apply {
                    cached["alice"] = profile("alice")
                    failure = SocialApiException(409, "account_deleting", "Finish deletion")
                }
            val coordinator = SessionCoordinator(MutableStateFlow(signedIn("alice")), profiles, backgroundScope)
            runCurrent()
            assertEquals(SessionStage.DELETING, coordinator.session.value.stage)
            assertTrue("alice" in profiles.deleting)
        }

    private fun signedIn(uid: String): AuthState = AuthState.SignedIn(SignedInUser(uid, null, "$uid@example.test"))

    private fun profile(uid: String) = SocialProfile(uid, uid, "Alice")
}

private class FakeAccountProfiles : AccountProfiles {
    override val profileChanges = MutableStateFlow(0)
    val cached = mutableMapOf<String, SocialProfile>()
    val profiles = mutableMapOf<String, SocialProfile>()
    val deleting = mutableSetOf<String>()
    var failure: Exception? = null
    var waitForAlice: CompletableDeferred<Unit>? = null
    var loaded = false

    override suspend fun cachedProfile(uid: String) = cached[uid]

    override suspend fun isDeleting(uid: String) = uid in deleting

    override suspend fun rememberDeletion(uid: String) {
        deleting.add(uid)
    }

    override suspend fun loadProfile(uid: String): SocialProfile? {
        loaded = true
        if (uid == "alice") waitForAlice?.await()
        failure?.let { throw it }
        return profiles[uid]
    }
}
