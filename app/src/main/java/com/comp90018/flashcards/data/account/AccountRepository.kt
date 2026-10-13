package com.comp90018.flashcards.data.account

import android.content.Context
import com.comp90018.flashcards.data.auth.AuthRepository
import com.comp90018.flashcards.data.social.FriendsRepository
import com.comp90018.flashcards.data.social.SocialApi
import com.comp90018.flashcards.data.social.SocialApiException
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialSession
import com.comp90018.flashcards.data.sync.DeckSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository
    @Inject
    constructor(
        private val auth: AuthRepository,
        private val friends: FriendsRepository,
        private val api: SocialApi,
        private val store: AccountStore,
        private val sync: DeckSyncCoordinator,
        private val session: SocialSession,
    ) : AccountProfiles {
        private val changes = MutableStateFlow(0)
        override val profileChanges = changes.asStateFlow()
        val usesPassword get() = auth.usesPassword

        override suspend fun cachedProfile(uid: String): SocialProfile? = store.profile(uid)

        override suspend fun isDeleting(uid: String): Boolean = store.isDeleting(uid)

        override suspend fun loadProfile(uid: String): SocialProfile? {
            val profile = friends.profile(uid)
            if (profile != null) store.save(uid, profile) else store.clearProfile(uid)
            return profile
        }

        suspend fun saveProfile(
            uid: String,
            username: String,
            name: String,
        ): SocialProfile {
            val profile = friends.saveProfile(uid, username, name)
            store.save(uid, profile)
            // The community profile is authoritative; dashboard updates even if Firebase is temporarily unavailable.
            auth.updateDisplayName(uid, profile.displayName)
            changes.update { it + 1 }
            return profile
        }

        override suspend fun rememberDeletion(uid: String) {
            store.setDeleting(uid, true)
            sync.setAccountDeleting(uid, true)
        }

        suspend fun deleteAccount(
            context: Context,
            uid: String,
            password: String,
        ) {
            session.requireUser(uid)
            auth.reauthenticate(context, uid, password).getOrThrow()
            rememberDeletion(uid)
            try {
                deleteCloudData(uid)
                session.requireUser(uid)
                store.eraseLocalData(uid)
                auth.deleteCurrentUser(uid).getOrThrow()
            } finally {
                changes.update { it + 1 }
            }
        }

        private suspend fun deleteCloudData(uid: String) {
            try {
                api.request(uid, "DELETE", "/v1/me")
            } catch (error: SocialApiException) {
                val endpointUnavailable = error.status == 404 || error.status == 405
                if (endpointUnavailable || error.code == "recent_login_required") {
                    // These failures happen before the server creates its deletion barrier.
                    store.setDeleting(uid, false)
                    sync.setAccountDeleting(uid, false)
                }
                if (endpointUnavailable) {
                    error("Account deletion needs the updated backend. Please try again after it is deployed.")
                }
                throw error
            }
        }
    }

interface AccountProfiles {
    val profileChanges: StateFlow<Int>

    suspend fun cachedProfile(uid: String): SocialProfile?

    suspend fun loadProfile(uid: String): SocialProfile?

    suspend fun isDeleting(uid: String): Boolean

    suspend fun rememberDeletion(uid: String)
}
