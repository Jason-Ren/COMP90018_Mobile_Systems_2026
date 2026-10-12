package com.comp90018.flashcards.data.social

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckCopies
    @Inject
    constructor(
        private val api: SocialApi,
        private val store: CopyStore,
        private val session: SocialSession,
    ) {
        private val mutex = Mutex()

        suspend fun hasPending(
            uid: String,
            sourceId: String,
        ): Boolean = store.pendingRequestId(uid, sourceId) != null

        suspend fun save(
            uid: String,
            sourceId: String,
        ): String =
            mutex.withLock {
                session.requireUser(uid)
                val requestId = store.requestId(uid, sourceId)
                val copy =
                    try {
                        socialJson.decodeFromString<SocialDeck>(
                            api.request(
                                uid,
                                "POST",
                                "/v1/decks/$sourceId/copies",
                                buildJsonObject { put("request_id", requestId) },
                            ),
                        )
                    } catch (error: SocialApiException) {
                        if (error.code == "copy_deleted") store.complete(uid, sourceId)
                        throw error
                    }
                val cards = mutableListOf<SocialCard>()
                do {
                    val page =
                        socialJson.decodeFromString<List<SocialCard>>(
                            api.request(uid, "GET", "/v1/decks/${copy.deckId}/cards?limit=100&offset=${cards.size}"),
                        )
                    cards.addAll(page)
                } while (page.size == 100 && cards.size <= 10_000)
                session.requireUser(uid)
                store.importDeck(uid, copy, cards)
                store.complete(uid, sourceId)
                copy.deckId
            }
    }
