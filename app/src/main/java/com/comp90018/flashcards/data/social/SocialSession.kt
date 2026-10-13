package com.comp90018.flashcards.data.social

import com.comp90018.flashcards.data.auth.FirebaseAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface SocialSession {
    val uid: String?

    suspend fun token(
        ownerId: String,
        refresh: Boolean,
    ): String

    fun requireUser(ownerId: String) {
        check(uid == ownerId) { "Your account changed. Open this screen again." }
    }
}

class FirebaseSocialSession
    @Inject
    constructor(
        private val provider: FirebaseAuthProvider,
    ) : SocialSession {
        override val uid: String? get() = provider.auth?.currentUser?.uid

        override suspend fun token(
            ownerId: String,
            refresh: Boolean,
        ): String {
            requireUser(ownerId)
            val user = checkNotNull(provider.auth?.currentUser) { "Sign in to continue." }
            val token = checkNotNull(user.getIdToken(refresh).await().token) { "Please sign in again." }
            requireUser(ownerId)
            return token
        }
    }
