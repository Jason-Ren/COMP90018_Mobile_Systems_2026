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
 * Makes Room mirror Firestore for the signed-in owner's decks.
 *
 * Firestore is the account source of truth. Pending outbox rows are left alone so an upload in
 * flight is not clobbered. Study progress is not downloaded.
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
            val ownedRemote = remoteDecks.filter { it.ownerId == ownerId }
            val remoteIds = ownedRemote.map { it.deckId }.toSet()
            for (remoteDeck in ownedRemote) {
                if (!shouldPullRemoteDeck(store.outboxDao.find(remoteDeck.deckId)?.operation)) {
                    continue
                }
                val cardsResult = remote.listCards(remoteDeck.deckId, fromServer = true)
                val cards = cardsResult.getOrElse { return syncFailureStep(it) }
                applyRemoteDeck(ownerId, remoteDeck, cards)
            }
            removeLocalDecksMissingFromCloud(ownerId, remoteIds)
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
                        createdAtEpochMillis =
                            existing?.createdAtEpochMillis?.takeIf { it > 0L }
                                ?: remoteDeck.createdAt?.toEpochMilli()
                                ?: remoteDeck.updatedAt.toEpochMilli(),
                    ),
                )
                val localCards = store.cardDao.listCardsByDeckId(remoteDeck.deckId)
                val remoteCardIds = cards.map { it.cardId }.toSet()
                localCards
                    .filter { it.cardId !in remoteCardIds }
                    .forEach { store.cardDao.deleteCardAndReviews(it) }
                cards.forEach { store.cardDao.insertCard(it.toLocalCard()) }
            }
        }

        /**
         * Drops cached decks that Firestore no longer has (deleted on another device), unless an
         * outbox row still needs to upload a local create/update/delete.
         */
        private suspend fun removeLocalDecksMissingFromCloud(
            ownerId: String,
            remoteIds: Set<String>,
        ) {
            store.deckDao.listDecksForOwner(ownerId).forEach { local ->
                if (local.deckId in remoteIds) {
                    return@forEach
                }
                if (!shouldPullRemoteDeck(store.outboxDao.find(local.deckId)?.operation)) {
                    return@forEach
                }
                database.withTransaction {
                    store.cardDao.listCardsByDeckId(local.deckId).forEach { store.cardDao.deleteCardAndReviews(it) }
                    store.deckDao.deleteDeck(local)
                }
            }
        }
    }

/** Skip cloud overwrite when local edits are still waiting to upload. */
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
