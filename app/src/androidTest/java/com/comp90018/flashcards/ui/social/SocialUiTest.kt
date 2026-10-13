package com.comp90018.flashcards.ui.social

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.comp90018.flashcards.data.social.SocialDeck
import com.comp90018.flashcards.ui.account.ProfileEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SocialUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sharedCodeValidationAndNavigation() {
        var opened: String? = null
        compose.setContent { MaterialTheme { SharedDeckCode { opened = it } } }
        compose.onNodeWithText("Open shared deck").assertIsNotEnabled()
        compose.onNodeWithText("Paste deck code").performTextInput("bad-code")
        compose.onNodeWithText("Open shared deck").performClick()
        assertEquals(null, opened)
        val id = "123e4567-e89b-12d3-a456-426614174000"
        compose.onNodeWithText("Paste deck code").performTextReplacement(id)
        compose.onNodeWithText("Open shared deck").performClick()
        assertEquals(id, opened)
    }

    @Test
    fun profileRequiresValidUsernameAndDisplayName() {
        var saved: Pair<String, String>? = null
        compose.setContent {
            MaterialTheme {
                ProfileEditor(
                    null,
                    true,
                ) { username, name -> saved = username to name }
            }
        }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Username").performTextInput("al")
        compose.onNodeWithText("Display name").performTextInput("Alice")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Username").performTextReplacement("alice")
        compose.onNodeWithText("Continue").assertIsEnabled().performClick()
        assertEquals("alice" to "Alice", saved)
    }

    @Test
    fun busyCopyCannotBeSubmittedAgain() {
        compose.setContent { MaterialTheme { CopyDeckAction(null, false, {}, {}) } }
        compose.onNodeWithText("Save a copy to My Decks").assertIsNotEnabled()
    }

    @Test
    fun completedCopyOpensReturnedDeck() {
        var opened: String? = null
        compose.setContent { MaterialTheme { CopyDeckAction("saved-id", true, {}, { opened = it }) } }
        compose.onNodeWithText("Open my saved copy").performClick()
        assertEquals("saved-id", opened)
    }

    @Test
    fun interruptedCopyOffersAnExplicitRetry() {
        var retries = 0
        compose.setContent { MaterialTheme { CopyDeckAction(null, true, { retries++ }, {}, pending = true) } }
        compose.onNodeWithText("Retry saving copy").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun publicDeckOffersUnpublish() {
        var published: Boolean? = null
        val deck = SocialDeck("deck", "Vocabulary", "alice", "public", 12)
        compose.setContent { MaterialTheme { OwnerDeckActions(deck, true, { published = it }, {}) } }
        compose.onNodeWithText("Unpublish").performClick()
        assertEquals(false, published)
    }
}
