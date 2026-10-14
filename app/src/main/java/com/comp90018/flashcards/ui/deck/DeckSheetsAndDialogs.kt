package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.R
import com.comp90018.flashcards.data.social.SocialFriend
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
    sharedWith: Set<String> = emptySet(),
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Share with friends",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TextButton(onClick = onFriends, enabled = !task.busy) {
                        Text("Manage friends")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Give friends access below, then send them the deck code.\n" +
                        "They can access your deck by entering the code in the 'Discover' tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                DeckCodeCard(deckId = deckId)
                SocialStatus(task)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Friends Access",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (friends?.isEmpty() == true) {
                item {
                    EmptyFriendsCard(onFriends = onFriends, busy = task.busy)
                }
            } else {
                items(friends.orEmpty(), key = { it.user.id }) { friend ->
                    FriendAccessCard(
                        friend = friend,
                        busy = task.busy,
                        hasAccess = friend.user.id in sharedWith,
                        onAccessChange = { granted -> onShare(friend.user.id, !granted) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCodeCard(deckId: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(deckId) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DECK CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = deckId,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(deckId))
                    copied = true
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Copied" else "Copy")
            }
        }
    }
}

@Composable
private fun FriendAccessCard(
    friend: SocialFriend,
    busy: Boolean,
    hasAccess: Boolean,
    onAccessChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${friend.user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = hasAccess,
                onCheckedChange = onAccessChange,
                enabled = !busy,
            )
        }
    }
}

@Composable
private fun EmptyFriendsCard(
    onFriends: () -> Unit,
    busy: Boolean,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "No friends yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Add friends to share deck access with them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onFriends,
                enabled = !busy,
            ) {
                Text("Manage friends")
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
    val transparentItem = ListItemDefaults.colors(containerColor = Color.Transparent)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text("Edit Deck Info") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                colors = transparentItem,
                modifier = Modifier.fillMaxWidth().clickable {
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
                colors = transparentItem,
                modifier = Modifier.fillMaxWidth().clickable {
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
                colors = transparentItem,
                modifier = Modifier.fillMaxWidth().clickable {
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
