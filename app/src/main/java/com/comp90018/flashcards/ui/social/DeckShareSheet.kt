package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckShareSheet(
    state: CloudDeckState,
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
                state.deck?.let { DeckCode(it.deckId) }
                TextButton(onClick = onRefresh, enabled = !task.busy) { Text("Refresh friends") }
                TextButton(onClick = onFriends, enabled = !task.busy) { Text("Manage friends and profile") }
            }
            if (state.friends?.isEmpty() ==
                true
            ) {
                item { Text("No friends yet. Add and accept a friend request first.") }
            }
            items(state.friends.orEmpty(), key = { it.user.id }) { friend ->
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
