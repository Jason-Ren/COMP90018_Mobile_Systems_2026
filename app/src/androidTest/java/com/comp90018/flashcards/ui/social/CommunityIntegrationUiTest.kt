package com.comp90018.flashcards.ui.social

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.comp90018.flashcards.data.remote.DeckVisibility
import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.ui.deck.PublicDeckRow
import com.comp90018.flashcards.ui.deck.PublishMenuItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CommunityIntegrationUiTest {
    @get:Rule val compose = createComposeRule()
    private val deck = RemoteDeck("source", "Vocabulary", "alice", DeckVisibility.PUBLIC, Instant.EPOCH, 12)

    @Test
    fun catalogueDistinguishesPreviewSaveAndOpenCopy() {
        val copies = mutableStateOf(CatalogueCopies())
        var preview: String? = null
        var saved: String? = null
        var opened: String? = null
        compose.setContent {
            MaterialTheme {
                PublicDeckRow(deck, true, copies.value, true, { preview = it }, { saved = it }, { opened = it })
            }
        }
        compose.onNodeWithText("12 cards · Yours").assertIsDisplayed()
        compose.onNodeWithText("Preview deck").performClick()
        assertEquals("source", preview)
        compose.onNodeWithText("Save a copy").performClick()
        assertEquals("source", saved)
        compose.runOnIdle { copies.value = CatalogueCopies(saved = mapOf("source" to "server-copy")) }
        compose.onNodeWithText("Open my saved copy").performClick()
        assertEquals("server-copy", opened)
    }

    @Test
    fun catalogueDisablesOtherCopiesDuringAnActiveSave() {
        compose.setContent {
            MaterialTheme {
                PublicDeckRow(deck, false, CatalogueCopies(saving = "another-deck"), false, {}, {}, {})
            }
        }
        compose.onNodeWithText("Save a copy").assertIsNotEnabled()
    }

    @Test
    fun interruptedCatalogueCopyRetriesTheSource() {
        var retried: String? = null
        compose.setContent {
            MaterialTheme {
                PublicDeckRow(deck, false, CatalogueCopies(pending = setOf("source")), true, {}, { retried = it }, {})
            }
        }
        compose.onNodeWithText("Retry saving copy").performClick()
        assertEquals("source", retried)
    }

    @Test
    fun publishMenuRequiresConfirmationAndSupportsCancellation() {
        val confirming = mutableStateOf(false)
        var writes = 0
        compose.setContent {
            MaterialTheme {
                PublishMenuItem(true, true, { confirming.value = true })
                if (confirming.value) {
                    PublishConfirmation(false, {
                        writes++
                        confirming.value = false
                    }, { confirming.value = false })
                }
            }
        }
        compose.onNodeWithText("Unpublish").performClick()
        assertEquals(0, writes)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, writes)
        compose.onNodeWithText("Unpublish").performClick()
        compose.onNodeWithText("Make private").performClick()
        assertEquals(1, writes)
    }
}
