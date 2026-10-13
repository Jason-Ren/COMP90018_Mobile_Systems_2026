package com.comp90018.flashcards.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.ui.social.SocialStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    deleting: Boolean = false,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    var loggingOut by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text("Account settings") }, navigationIcon = {
            if (!deleting) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SocialStatus(task)
                if (deleting) {
                    Text("Account deletion is pending", style = MaterialTheme.typography.titleLarge)
                    Text("Your account is being removed. Confirm your sign-in again to finish. You can retry safely.")
                } else {
                    profile?.let { ProfileDetails(it, !task.busy, viewModel::updateName) }
                    if (profile == null) TextButton(onClick = viewModel::refresh, enabled = !task.busy) { Text("Retry") }
                }
                OutlinedButton(
                    onClick = { loggingOut = true },
                    enabled = !task.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Log out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            val errorColor = MaterialTheme.colorScheme.error
            OutlinedButton(
                onClick = { confirming = true },
                enabled = !task.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
                border = BorderStroke(1.dp, if (task.busy) errorColor.copy(alpha = 0.12f) else errorColor),
            ) {
                Text(
                    if (deleting) "Finish deleting account" else "Delete account",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
    if (loggingOut) {
        LogOutConfirmation(
            onConfirm = {
                loggingOut = false
                onSignOut()
            },
            onDismiss = { loggingOut = false },
        )
    }
    if (confirming) {
        DeleteAccountConfirmation(
            usesPassword = viewModel.usesPassword,
            enabled = !task.busy,
            onConfirm = {
                confirming = false
                viewModel.deleteAccount(context, it)
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun ProfileDetails(
    profile: SocialProfile,
    enabled: Boolean,
    onSave: (String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            FieldLabel("Username")
            Text("@${profile.username}", style = MaterialTheme.typography.bodyLarge)
        }
        Column {
            FieldLabel("Display name")
            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    name = profile.displayName
                                    editing = false
                                },
                                enabled = enabled,
                            ) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                            IconButton(
                                onClick = {
                                    editing = false
                                    onSave(name.trim())
                                },
                                enabled = enabled && name.trim().length in 1..80,
                            ) { Icon(Icons.Filled.Check, contentDescription = "Save display name") }
                        }
                    },
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { editing = true }, enabled = enabled) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit display name")
                    }
                }
            }
        }
    }
}

@Composable
internal fun LogOutConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out?") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Log out") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun DeleteAccountConfirmation(
    usesPassword: Boolean,
    enabled: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text("Delete your account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This permanently removes your account, decks, profile, friendships and local study history. " +
                        "Copies saved by other people remain theirs.",
                )
                Text(
                    if (usesPassword) {
                        "Enter your password to confirm."
                    } else {
                        "Confirm your Google account in the next step."
                    },
                )
                if (usesPassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = enabled,
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = enabled && (!usesPassword || password.isNotBlank()),
            ) {
                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text("Cancel") } },
    )
}
