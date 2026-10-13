package com.comp90018.flashcards.data.social

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.comp90018.flashcards.data.local.FlashcardDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class LocalCopyStoreTest {
    private lateinit var database: FlashcardDatabase
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val uid = "test-${UUID.randomUUID()}"
    private val session =
        object : SocialSession {
            override val uid: String = this@LocalCopyStoreTest.uid

            override suspend fun token(
                ownerId: String,
                refresh: Boolean,
            ): String = error("No network in this test")
        }
    private val deck get() = SocialDeck("copy", "Vocabulary", uid, "private", 1)
    private val cards = listOf(SocialCard("new-card", "Hello", "Bonjour", 0))

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, FlashcardDatabase::class.java).build()
    }

    @After fun tearDown() {
        database.close()
    }

    @Test
    fun importIsCompleteAndRetryPreservesLocalEdits() =
        runBlocking {
            val store = LocalCopyStore(context, database, session)
            store.importDeck(uid, deck, cards)
            val local = checkNotNull(database.deckDao().getDeckById("copy"))
            assertEquals(uid, local.ownerId)
            assertEquals(1, database.cardDao().listCardsByDeckId("copy").size)
            assertNull(database.reviewDao().getState(uid, "new-card"))
            database.deckDao().updateDeck(local.copy(name = "Edited locally"))
            store.importDeck(uid, deck, cards)
            assertEquals("Edited locally", database.deckDao().getDeckById("copy")?.name)
        }

    @Test
    fun sessionChangeRollsBackDeckAndCardsTogether() =
        runBlocking {
            var checks = 0
            val changing =
                object : SocialSession {
                    override val uid = this@LocalCopyStoreTest.uid

                    override suspend fun token(
                        ownerId: String,
                        refresh: Boolean,
                    ): String = error("No network")

                    override fun requireUser(ownerId: String) {
                        check(++checks == 1) { "Account changed" }
                    }
                }
            val store = LocalCopyStore(context, database, changing)
            assertTrue(runCatching { store.importDeck(uid, deck, cards) }.isFailure)
            assertNull(database.deckDao().getDeckById("copy"))
            assertEquals(0, database.cardDao().getCardCount())
        }

    @Test
    fun retryReceiptSurvivesStoreRecreationAndIsUserScoped() =
        runBlocking {
            val store = LocalCopyStore(context, database, session)
            val id = store.requestId(uid, "source")
            assertEquals(id, LocalCopyStore(context, database, session).requestId(uid, "source"))
            val other = store.requestId("$uid-other", "source")
            assertTrue(other != id)
            store.complete(uid, "source")
            store.complete("$uid-other", "source")
        }
}
