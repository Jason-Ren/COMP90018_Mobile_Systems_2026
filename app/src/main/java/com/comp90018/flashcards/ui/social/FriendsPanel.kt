package com.comp90018.flashcards.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialRequest

private val AcceptGreen = Color(0xFF66BB6A)
private val CardRowPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)

@Composable
internal fun FriendsPanel(
    bottomInset: Dp = 0.dp,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<SocialProfile?>(null) }
    // The task error is shared by every action, so only show it under the field after an add attempt.
    var addedLast by remember { mutableStateOf(false) }
    val status = task.copy(error = task.error.takeUnless { addedLast })
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status.busy || status.error != null || status.message != null) {
            item {
                FriendsStatus(status, initialLoad = !state.loaded) {
                    addedLast = false
                    viewModel.refresh()
                }
            }
        }
        if (state.profile != null) {
            item {
                AddFriendForm(!task.busy, if (addedLast) task.error else null) {
                    addedLast = true
                    viewModel.addFriend(it)
                }
            }

            friendRequests(state, !task.busy) { id, action ->
                addedLast = false
                viewModel.respond(id, action)
            }
            friends(state, !task.busy) { removing = it }
        }
    }
    removing?.let { friend ->
        SocialConfirmation(
            "Remove ${friend.displayName}?",
            "Shared private decks will no longer be accessible. Saved copies remain.",
            "Remove",
            onConfirm = {
                removing = null
                addedLast = false
                viewModel.remove(friend.id)
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun FriendsStatus(
    task: SocialTaskState,
    initialLoad: Boolean,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SocialStatus(task)
        if (task.busy && initialLoad) {
            Text("Loading friends…", style = MaterialTheme.typography.bodyMedium)
        }
        if (task.error != null) {
            TextButton(onClick = onRetry, enabled = !task.busy) { Text("Retry") }
        }
    }
}

private fun LazyListScope.friendRequests(
    state: FriendsState,
    enabled: Boolean,
    onRespond: (String, String) -> Unit,
) {
    item { SectionHeader("Requests · ${state.incoming.size + state.outgoing.size}") }
    if (state.incoming.isEmpty() && state.outgoing.isEmpty()) item { Text("No pending requests.") }
    items(state.incoming, key = { "in_${it.id}" }) { request ->
        FriendRequestCard(request, true, enabled, onRespond)
    }
    items(state.outgoing, key = { "out_${it.id}" }) { request ->
        FriendRequestCard(request, false, enabled, onRespond)
    }
}

private fun LazyListScope.friends(
    state: FriendsState,
    enabled: Boolean,
    onRemove: (SocialProfile) -> Unit,
) {
    item { SectionHeader("Friends · ${state.friends.size}") }
    if (state.friends.isEmpty()) item { Text("Add a friend by username to start sharing decks.") }
    items(state.friends, key = { it.user.id }) { friend ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(CardRowPadding), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ProfileLabel(friend.user) }
                TextButton(
                    onClick = { onRemove(friend.user) },
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            }
        }
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
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AddFriendForm(
    enabled: Boolean,
    error: String?,
    onAdd: (String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var errorDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(error) { errorDismissed = false }
    val shownError = error.takeUnless { errorDismissed }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add a friend", style = MaterialTheme.typography.titleMedium)
            FriendUsernameField(
                username = username,
                onUsernameChange = {
                    username = it
                    errorDismissed = true
                },
                enabled = enabled,
                error = shownError,
                onSend = { onAdd(username) },
            )
        }
    }
}

@Composable
private fun FriendUsernameField(
    username: String,
    onUsernameChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    onSend: () -> Unit,
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        placeholder = { Text("Enter username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText =
            if (error != null) {
                { Text(error) }
            } else {
                null
            },
        trailingIcon = {
            IconButton(
                onClick = onSend,
                enabled = enabled && Regex("[A-Za-z0-9_]{3,24}").matches(username.trim()),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send friend request")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FriendRequestCard(
    request: SocialRequest,
    incoming: Boolean,
    enabled: Boolean,
    onRespond: (String, String) -> Unit,
) {
    val profile = if (incoming) request.sender else request.recipient
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(CardRowPadding), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ProfileLabel(profile)
                Text(
                    if (incoming) "Wants to be friends" else "Request sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (incoming) {
                RespondButton(Icons.Default.Check, "Accept", AcceptGreen, enabled) { onRespond(request.id, "accept") }
            }
            RespondButton(
                Icons.Default.Close,
                if (incoming) "Decline" else "Cancel request",
                MaterialTheme.colorScheme.error,
                enabled,
            ) { onRespond(request.id, if (incoming) "decline" else "cancel") }
        }
    }
}

@Composable
private fun RespondButton(
    icon: ImageVector,
    description: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = color),
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
internal fun ProfileLabel(profile: SocialProfile) {
    Column {
        Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium)
    }
}
