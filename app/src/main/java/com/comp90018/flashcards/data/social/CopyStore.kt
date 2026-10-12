package com.comp90018.flashcards.data.social

import android.content.Context
import androidx.room.withTransaction
import com.comp90018.flashcards.data.local.FlashcardDatabase
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.local.entity.DeckEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface CopyStore {
    suspend fun pendingRequestId(
        uid: String,
        sourceId: String,
    ): String?

    suspend fun requestId(
        uid: String,
        sourceId: String,
    ): String

    suspend fun complete(
        uid: String,
        sourceId: String,
    )

    suspend fun importDeck(
        uid: String,
        deck: SocialDeck,
        cards: List<SocialCard>,
    )
}

@Singleton
class LocalCopyStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val database: FlashcardDatabase,
        private val session: SocialSession,
    ) : CopyStore {
        private val preferences = context.getSharedPreferences("pending_deck_copies", Context.MODE_PRIVATE)

        override suspend fun pendingRequestId(
            uid: String,
            sourceId: String,
        ): String? = withContext(Dispatchers.IO) { preferences.getString("${uid}_$sourceId", null) }

        override suspend fun requestId(
            uid: String,
            sourceId: String,
        ): String =
            withContext(Dispatchers.IO) {
                val key = "${uid}_$sourceId"
                preferences.getString(key, null) ?: UUID.randomUUID().toString().also {
                    // Persist before sending the request, including across process death.
                    check(
                        preferences.edit().putString(key, it).commit(),
                    ) { "Could not save this action. Please retry." }
                }
            }

        override suspend fun complete(
            uid: String,
            sourceId: String,
        ) {
            withContext(Dispatchers.IO) {
                check(
                    preferences.edit().remove("${uid}_$sourceId").commit(),
                ) { "Could not finish saving. Please retry." }
            }
        }

        override suspend fun importDeck(
            uid: String,
            deck: SocialDeck,
            cards: List<SocialCard>,
        ) {
            check(deck.ownerId == uid) { "This copy belongs to another account." }
            check(cards.size == deck.cardCount && cards.map { it.cardId }.distinct().size == cards.size) {
                "The deck changed while downloading. Please retry."
            }
            database.withTransaction {
                session.requireUser(uid)
                val existing = database.deckDao().getDeckById(deck.deckId)
                if (existing == null) {
                    database.deckDao().insertDeck(DeckEntity(deck.deckId, deck.name, ownerId = uid))
                    cards.forEach { database.cardDao().insertCard(it.toLocalCard(deck.deckId)) }
                    // No review/state rows: the existing scheduler treats these new IDs as new cards.
                } else {
                    check(existing.ownerId == uid) { "This deck belongs to another account." }
                    // A retry after a successful import must not overwrite edits or study progress.
                }
                session.requireUser(uid)
            }
        }
    }

internal fun SocialCard.toLocalCard(deckId: String): CardEntity =
    CardEntity(
        cardId = cardId,
        deckId = deckId,
        front = front,
        back = back,
        position = position,
        // Device-local references from another installation cannot safely be imported.
        frontImageUri = frontImageUri?.takeIf { it.startsWith("https://") },
        backImageUri = backImageUri?.takeIf { it.startsWith("https://") },
    )
