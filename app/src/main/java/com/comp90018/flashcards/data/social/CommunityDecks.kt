package com.comp90018.flashcards.data.social

import com.comp90018.flashcards.data.auth.AuthRepository
import com.comp90018.flashcards.data.local.dao.DeckDao
import com.comp90018.flashcards.data.remote.DeckRemoteDataSource
import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.data.sync.DeckSyncCoordinator
import com.comp90018.flashcards.data.sync.DeckSyncOutcome
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class CommunityDecks
    @Inject
    constructor(
        private val api: SocialApi,
        private val remote: DeckRemoteDataSource,
        private val local: DeckDao,
        private val sync: DeckSyncCoordinator,
        private val session: SocialSession,
        private val authRepository: AuthRepository,
    ) {
        suspend fun catalogue(uid: String): List<RemoteDeck> {
            session.requireUser(uid)
            val decks = remote.listPublicDecks().getOrThrow()
            session.requireUser(uid)
            return decks
        }

        suspend fun prepare(
            uid: String,
            deckId: String,
        ): SocialDeck {
            session.requireUser(uid)
            val deck = local.getDeckById(deckId)
            if (deck?.ownerId == uid) {
                sync.deckChanged(deckId, uid)
                check(sync.syncNow(uid) == DeckSyncOutcome.UP_TO_DATE) {
                    "Your deck is still syncing. Check your connection and try again."
                }
            }
            return get(uid, deckId)
        }

        suspend fun get(
            uid: String,
            deckId: String,
        ): SocialDeck = socialJson.decodeFromString(api.request(uid, "GET", "/v1/decks/$deckId"))

        suspend fun cards(
            uid: String,
            deckId: String,
            offset: Int,
        ): List<SocialCard> =
            socialJson.decodeFromString(api.request(uid, "GET", "/v1/decks/$deckId/cards?limit=50&offset=$offset"))

        suspend fun publish(
            uid: String,
            deckId: String,
            public: Boolean,
        ): SocialDeck {
            if (public) prepare(uid, deckId)
            val published: SocialDeck =
                socialJson.decodeFromString(
                    api.request(uid, if (public) "POST" else "DELETE", "/v1/decks/$deckId/publish"),
                )
            if (public) {
                stampAuthorEmail(deckId)
            }
            return published
        }

        /** Stores the publisher's email on the cloud deck so Discover can search by creator. */
        private suspend fun stampAuthorEmail(deckId: String) {
            val email = authRepository.currentEmail?.takeIf { it.isNotBlank() }
            val deck = email?.let { remote.getDeck(deckId).getOrNull() }
            if (email != null && deck != null && deck.authorEmail != email) {
                remote.upsertDeck(deck.copy(authorEmail = email)).getOrThrow()
            }
        }

        suspend fun share(
            uid: String,
            deckId: String,
            recipientId: String,
        ) {
            prepare(uid, deckId)
            api.request(uid, "POST", "/v1/decks/$deckId/shares", buildJsonObject { put("recipient_id", recipientId) })
        }

        suspend fun revoke(
            uid: String,
            deckId: String,
            recipientId: String,
        ) {
            api.request(uid, "DELETE", "/v1/decks/$deckId/shares/$recipientId")
        }
    }
