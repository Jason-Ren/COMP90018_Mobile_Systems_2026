package com.comp90018.flashcards.data.sync

import androidx.room.withTransaction
import com.comp90018.flashcards.data.local.FlashcardDatabase
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.data.remote.DeckRemoteDataSource
import com.comp90018.flashcards.data.remote.RemoteCard
import com.comp90018.flashcards.data.remote.RemoteDeck
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports the signed-in user's cloud decks into Room so My Decks matches across devices.
 *
 * Decks with a pending outbox row are left alone so local edits are not overwritten before push.
 * Study progress is not downloaded.
 */
@Singleton
class DeckCloudPull
    @Inject
    constructor(
        private val remote: DeckRemoteDataSource,
        private val database: FlashcardDatabase,
        private val store: DeckSyncStore,
    ) {
        suspend fun pullOwned(ownerId: String): DeckSyncStep =
            remote.listDecksForOwner(ownerId, fromServer = true).fold(
                onSuccess = { pullListed(ownerId, it) },
                onFailure = ::syncFailureStep,
            )

        private suspend fun pullListed(
            ownerId: String,
            remoteDecks: List<RemoteDeck>,
        ): DeckSyncStep {
            for (remoteDeck in remoteDecks.filter { it.ownerId == ownerId }) {
                if (!shouldPullRemoteDeck(store.outboxDao.find(remoteDeck.deckId)?.operation)) {
                    continue
                }
                val cardsResult = remote.listCards(remoteDeck.deckId, fromServer = true)
                val cards = cardsResult.getOrElse { return syncFailureStep(it) }
                applyRemoteDeck(ownerId, remoteDeck, cards)
            }
            return DeckSyncStep.Synced
        }

        private suspend fun applyRemoteDeck(
            ownerId: String,
            remoteDeck: RemoteDeck,
            cards: List<RemoteCard>,
        ) {
            database.withTransaction {
                val existing = store.deckDao.getDeckById(remoteDeck.deckId)
                if (existing != null && existing.ownerId != ownerId) {
                    return@withTransaction
                }
                store.deckDao.insertDeck(
                    DeckEntity(
                        deckId = remoteDeck.deckId,
                        name = remoteDeck.name,
                        description = existing?.description.orEmpty(),
                        ownerId = ownerId,
                    ),
                )
                val localCards = store.cardDao.listCardsByDeckId(remoteDeck.deckId)
                val remoteIds = cards.map { it.cardId }.toSet()
                localCards
                    .filter { it.cardId !in remoteIds }
                    .forEach { store.cardDao.deleteCardAndReviews(it) }
                cards.forEach { store.cardDao.insertCard(it.toLocalCard()) }
            }
        }
    }

/** Skip pull when local edits are waiting to upload. */
internal fun shouldPullRemoteDeck(pendingOperation: String?): Boolean = pendingOperation == null

internal fun RemoteCard.toLocalCard(): CardEntity =
    CardEntity(
        cardId = cardId,
        deckId = deckId,
        front = front,
        back = back,
        position = position,
        frontImageUri = frontImageUri?.takeIf { it.startsWith("https://") },
        backImageUri = backImageUri?.takeIf { it.startsWith("https://") },
    )
