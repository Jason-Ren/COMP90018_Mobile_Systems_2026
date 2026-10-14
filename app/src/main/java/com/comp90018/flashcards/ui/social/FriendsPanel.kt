package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialRequest

@Composable
internal fun FriendsPanel(
    bottomInset: Dp = 0.dp,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<SocialProfile?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.profile != null) {
            item { AddFriendForm(!task.busy, viewModel::addFriend) }
            item { Text("Friend requests", style = MaterialTheme.typography.titleMedium) }
            if (state.incoming.isEmpty() && state.outgoing.isEmpty()) item { Text("No pending requests.") }
            items(state.incoming, key = { "in_${it.id}" }) { request ->
                FriendRequestCard(request, true, !task.busy, viewModel::respond)
            }
            items(state.outgoing, key = { "out_${it.id}" }) { request ->
                FriendRequestCard(request, false, !task.busy, viewModel::respond)
            }
            item { Text("Friends · ${state.friends.size}", style = MaterialTheme.typography.titleMedium) }
            if (state.friends.isEmpty()) item { Text("Add a friend by username to start sharing decks.") }
            items(state.friends, key = { it.user.id }) { friend ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        ProfileLabel(friend.user)
                        TextButton(onClick = { removing = friend.user }, enabled = !task.busy) { Text("Remove friend") }
                    }
                }
            }
        }
    }
    removing?.let { friend ->
        SocialConfirmation(
            "Remove ${friend.displayName}?",
            "Private decks shared between you will no longer be accessible. Saved copies remain.",
            "Remove",
            onConfirm = {
                removing = null
                viewModel.remove(friend.id)
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
internal fun FriendOutcomeNotice(notice: String?) {
    notice ?: return
    Text(
        notice,
        modifier =
            Modifier
                .padding(vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun AddFriendForm(
    enabled: Boolean,
    onAdd: (String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Friend's username") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onAdd(username) },
            enabled = enabled && Regex("[A-Za-z0-9_]{3,24}").matches(username.trim()),
        ) {
            Text("Send friend request")
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: SocialRequest,
    incoming: Boolean,
    enabled: Boolean,
    onRespond: (String, String) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            ProfileLabel(if (incoming) request.sender else request.recipient)
            Text(if (incoming) "Wants to be your friend" else "Request sent")
            Row {
                if (incoming) {
                    TextButton(
                        onClick = { onRespond(request.id, "accept") },
                        enabled = enabled,
                    ) { Text("Accept") }
                }
                TextButton(
                    onClick = { onRespond(request.id, if (incoming) "decline" else "cancel") },
                    enabled = enabled,
                ) {
                    Text(if (incoming) "Decline" else "Cancel request")
                }
            }
        }
    }
}

@Composable
internal fun ProfileLabel(profile: SocialProfile) {
    Column {
        Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
        Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium)
    }
}
