package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.R
import com.comp90018.flashcards.data.social.SocialFriend
import com.comp90018.flashcards.ui.social.DeckCode
import com.comp90018.flashcards.ui.social.ProfileLabel
import com.comp90018.flashcards.ui.social.SocialStatus
import com.comp90018.flashcards.ui.social.SocialTaskState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedDeckShareSheet(
    deckId: String,
    friends: List<SocialFriend>?,
    task: SocialTaskState,
    onDismiss: () -> Unit,
    onFriends: () -> Unit,
    onShare: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Share with friends", style = MaterialTheme.typography.headlineSmall)
                Text("Give a friend read access, then send them the deck code. They can open it in Community.")
                SocialStatus(task)
                DeckCode(deckId)
                TextButton(onClick = onRefresh, enabled = !task.busy) { Text("Refresh friends") }
                TextButton(onClick = onFriends, enabled = !task.busy) { Text("Manage friends and profile") }
            }
            if (friends?.isEmpty() == true) {
                item { Text("No friends yet. Add and accept a friend request first.") }
            }
            items(friends.orEmpty(), key = { it.user.id }) { friend ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        ProfileLabel(friend.user)
                        Row {
                            TextButton(
                                onClick = { onShare(friend.user.id, false) },
                                enabled = !task.busy,
                            ) { Text("Share") }
                            TextButton(
                                onClick = { onShare(friend.user.id, true) },
                                enabled = !task.busy,
                            ) { Text("Revoke access") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckOptionsBottomSheet(
    isPublic: Boolean,
    onDismiss: () -> Unit,
    onEditDeckInfo: () -> Unit,
    onEditVisibility: () -> Unit,
    onDeleteDeck: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            ListItem(
                headlineContent = { Text("Edit Deck Info") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onEditDeckInfo()
                    },
            )
            ListItem(
                headlineContent = {
                    Text(if (isPublic) "Make Deck Private" else "Make Deck Public")
                },
                leadingContent = {
                    if (isPublic) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    } else {
                        Icon(painterResource(R.drawable.ic_globe), contentDescription = null)
                    }
                },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onEditVisibility()
                    },
            )
            ListItem(
                headlineContent = {
                    Text("Delete Deck", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onDeleteDeck()
                    },
            )
        }
    }
}

@Composable
fun ReadOnlyCardDialog(
    card: CardUiModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = card.front, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider()
                Text(text = card.back, style = MaterialTheme.typography.bodyLarge)
                if (card.frontImageUri != null || card.backImageUri != null) {
                    Text(
                        text = "Contains image attachment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
