package com.comp90018.flashcards.data.social

import com.comp90018.flashcards.ui.social.sharedDeckId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DeckCopiesTest {
    private val copyJson = """{"deckId":"copy","name":"Study","ownerId":"alice","visibility":"private","cardCount":1}"""
    private val cardsJson = """[{"cardId":"new-card","front":"Front","back":"Back","position":0}]"""

    @Test
    fun `retry after interrupted card download imports the same server copy`() =
        runBlocking {
            val store = FakeCopyStore()
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    when (it) {
                        1, 3 -> SocialHttpResponse(200, copyJson)
                        2 -> throw IOException("Connection lost after copy was created")
                        else -> SocialHttpResponse(200, cardsJson)
                    }
                }
            val api = SocialApi("https://example.test", session, transport)
            assertTrue(runCatching { DeckCopies(api, store, session).save("alice", "source") }.isFailure)
            assertFalse(store.completed)
            assertEquals(0, store.imports)
            assertEquals("copy", DeckCopies(api, store, session).save("alice", "source"))
            assertEquals(transport.calls[0].body, transport.calls[2].body)
            assertEquals(listOf("new-card"), store.cards.map { it.cardId })
            assertEquals(1, store.imports)
            assertTrue(store.completed)
        }

    @Test
    fun `failed local import retains the pending receipt`() =
        runBlocking {
            val store = FakeCopyStore(failImport = true)
            val transport = RecordingTransport { SocialHttpResponse(200, if (it == 1) copyJson else cardsJson) }
            val session = FakeSocialSession()
            val result =
                runCatching {
                    DeckCopies(
                        SocialApi("https://example.test", session, transport),
                        store,
                        session,
                    ).save("alice", "source")
                }
            assertTrue(result.isFailure)
            assertFalse(store.completed)
        }

    @Test
    fun `account switch never imports copy into the next account`() =
        runBlocking {
            val store = FakeCopyStore()
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    session.uid = "bob"
                    SocialHttpResponse(200, copyJson)
                }
            assertTrue(
                runCatching {
                    DeckCopies(
                        SocialApi("https://example.test", session, transport),
                        store,
                        session,
                    ).save("alice", "source")
                }.isFailure,
            )
            assertEquals(0, store.imports)
            assertFalse(store.completed)
        }

    @Test
    fun `deleted copy clears receipt only so the user can start a new action`() =
        runBlocking {
            val store = FakeCopyStore()
            val session = FakeSocialSession()
            val transport =
                RecordingTransport {
                    SocialHttpResponse(410, """{"error":{"code":"copy_deleted","message":"Start a new copy."}}""")
                }
            assertTrue(
                runCatching {
                    DeckCopies(
                        SocialApi("https://example.test", session, transport),
                        store,
                        session,
                    ).save("alice", "source")
                }.isFailure,
            )
            assertTrue(store.completed)
            assertEquals(1, transport.calls.size)
            assertEquals(0, store.imports)
        }

    @Test
    fun `import keeps new identifiers and HTTPS images but rejects device local references`() {
        val card = SocialCard("new-id", "front", "back", 4, "content://private/image", "https://example.test/image")
        val local = card.toLocalCard("new-deck")
        assertEquals("new-id", local.cardId)
        assertEquals("new-deck", local.deckId)
        assertEquals(4, local.position)
        assertNull(local.frontImageUri)
        assertEquals("https://example.test/image", local.backImageUri)
    }

    @Test
    fun `shared deck codes accept only complete UUIDs`() {
        val code = "123E4567-E89B-12D3-A456-426614174000"
        assertEquals(code.lowercase(), sharedDeckId(" $code "))
        assertNull(sharedDeckId("../../../v1/me"))
        assertNull(sharedDeckId("https://unknown.test/$code"))
        assertNull(sharedDeckId("short-code"))
    }
}

private class FakeCopyStore(
    private val failImport: Boolean = false,
) : CopyStore {
    override suspend fun pendingRequestId(
        uid: String,
        sourceId: String,
    ): String? = if (completed) null else "stable-request"

    var completed = false
    var imports = 0
    var cards = emptyList<SocialCard>()

    override suspend fun requestId(
        uid: String,
        sourceId: String,
    ): String = "stable-request"

    override suspend fun complete(
        uid: String,
        sourceId: String,
    ) {
        completed = true
    }

    override suspend fun importDeck(
        uid: String,
        deck: SocialDeck,
        cards: List<SocialCard>,
    ) {
        if (failImport) throw IOException("Disk full")
        imports++
        this.cards = cards
    }
}
