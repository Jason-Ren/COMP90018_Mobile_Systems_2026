package com.comp90018.flashcards.data.account

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.comp90018.flashcards.data.local.DeckSyncDatabase
import com.comp90018.flashcards.data.local.FlashcardDatabase
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.local.entity.CardFsrsStateEntity
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.data.local.entity.ReviewLogEntity
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.domain.fsrs.FsrsCard
import com.comp90018.flashcards.domain.model.Rating
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AccountStoreTest {
    @Test
    fun deletionClearsOnlySelectedAccountAndPreservesRecoveryFlag() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = Room.inMemoryDatabaseBuilder(context, FlashcardDatabase::class.java).build()
            val syncDatabase = Room.inMemoryDatabaseBuilder(context, DeckSyncDatabase::class.java).build()
            val store = AccountStore(context, database, syncDatabase)
            val uid = "test_${UUID.randomUUID()}"
            val otherUid = "${uid}_other"
            val receipts = context.getSharedPreferences("pending_deck_copies", Context.MODE_PRIVATE)
            val now = Instant.now()
            try {
                for (owner in listOf(uid, otherUid)) {
                    database.deckDao().insertDeck(DeckEntity(owner, "Deck", ownerId = owner))
                    database.cardDao().insertCard(CardEntity(owner, owner, "Front", "Back"))
                    database.reviewDao().recordReview(
                        CardFsrsStateEntity.from(owner, owner, FsrsCard.new(now), now),
                        ReviewLogEntity(owner, owner, owner, Rating.GOOD, now),
                    )
                    syncDatabase.outboxDao().enqueue(owner, owner, "upsert", 1L)
                    store.save(owner, SocialProfile(owner, "username", "Display name"))
                }
                receipts
                    .edit()
                    .putString("${uid}_source", "request")
                    .putString("${otherUid}_source", "other request")
                    .commit()
                store.setDeleting(uid, true)

                store.eraseLocalData(uid)

                assertNull(database.deckDao().getDeckById(uid))
                assertTrue(database.cardDao().listCardsByDeckId(uid).isEmpty())
                assertNull(database.reviewDao().getState(uid, uid))
                assertTrue(database.reviewDao().getReviewLogs(uid, uid).isEmpty())
                assertTrue(syncDatabase.outboxDao().pendingForOwner(uid).isEmpty())
                assertNull(store.profile(uid))
                assertFalse(receipts.contains("${uid}_source"))
                assertEquals(otherUid, database.deckDao().getDeckById(otherUid)?.ownerId)
                assertEquals(1, database.cardDao().listCardsByDeckId(otherUid).size)
                assertEquals(otherUid, database.reviewDao().getState(otherUid, otherUid)?.userId)
                assertEquals(1, database.reviewDao().getReviewLogs(otherUid, otherUid).size)
                assertEquals(1, syncDatabase.outboxDao().pendingForOwner(otherUid).size)
                assertEquals(otherUid, store.profile(otherUid)?.id)
                assertTrue(receipts.contains("${otherUid}_source"))
                assertTrue(AccountStore(context, database, syncDatabase).isDeleting(uid))
            } finally {
                store.clearProfile(otherUid)
                store.setDeleting(uid, false)
                receipts
                    .edit()
                    .remove("${uid}_source")
                    .remove("${otherUid}_source")
                    .commit()
                database.close()
                syncDatabase.close()
            }
        }
}
