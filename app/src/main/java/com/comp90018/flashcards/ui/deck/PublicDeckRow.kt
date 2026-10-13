package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.ui.social.CatalogueCopies

// Adapted from Jason Ren's PublicDeckScreen in PR #64; actions use the social API workflow.
@Composable
internal fun PublicDeckRow(
    deck: RemoteDeck,
    ownedByMe: Boolean,
    copies: CatalogueCopies,
    enabled: Boolean,
    onPreview: (String) -> Unit,
    onSaveCopy: (String) -> Unit,
    onOpenLocal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(deck.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (ownedByMe) "${deck.cardCount} cards · Yours" else "${deck.cardCount} cards · Public",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            deck.authorEmail?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onPreview(deck.deckId) }) { Text("Preview deck") }
            val savedId = copies.saved[deck.deckId]
            FilledTonalButton(
                onClick = { if (savedId != null) onOpenLocal(savedId) else onSaveCopy(deck.deckId) },
                enabled = enabled,
            ) {
                Text(
                    when {
                        copies.saving == deck.deckId -> "Saving…"
                        savedId != null -> "Open my saved copy"
                        deck.deckId in copies.pending -> "Retry saving copy"
                        else -> "Save a copy"
                    },
                )
            }
        }
    }
}
