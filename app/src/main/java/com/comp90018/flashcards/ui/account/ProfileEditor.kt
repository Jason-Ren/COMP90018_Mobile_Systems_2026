package com.comp90018.flashcards.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.data.social.SocialProfile

@Composable
internal fun ProfileEditor(
    profile: SocialProfile?,
    enabled: Boolean,
    onSave: (String, String) -> Unit,
) {
    var username by rememberSaveable(profile?.username) { mutableStateOf(profile?.username.orEmpty()) }
    var name by rememberSaveable(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    val valid = Regex("[A-Za-z0-9_]{3,24}").matches(username.trim()) && name.trim().length in 1..80
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Set up your profile",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Friends can find you by username.")
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                enabled = enabled,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("3–24 letters, numbers or underscores") },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                enabled = enabled,
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onSave(username, name) }, enabled = enabled && valid) {
                Text("Continue")
            }
        }
    }
}
