package com.comp90018.flashcards.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text("Account") }, navigationIcon = {
            if (!deleting) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SocialStatus(task)
            if (deleting) {
                Text("Account deletion is pending", style = MaterialTheme.typography.titleLarge)
                Text("Your account is being removed. Confirm your sign-in again to finish. You can retry safely.")
            } else {
                profile?.let { DisplayNameEditor(it, !task.busy, viewModel::updateName) }
                if (profile == null) TextButton(onClick = viewModel::refresh, enabled = !task.busy) { Text("Retry") }
            }
            OutlinedButton(onClick = onSignOut, enabled = !task.busy) { Text("Log out") }
            Button(
                onClick = { confirming = true },
                enabled = !task.busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(if (deleting) "Finish deleting account" else "Delete account") }
        }
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
internal fun DisplayNameEditor(
    profile: SocialProfile,
    enabled: Boolean,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("@${profile.username}", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            enabled = enabled,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSave(name.trim()) }, enabled = enabled && name.trim().length in 1..80) {
            Text("Save display name")
        }
    }
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
