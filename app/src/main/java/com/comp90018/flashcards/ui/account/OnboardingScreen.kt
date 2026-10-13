package com.comp90018.flashcards.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.flashcards.ui.social.SocialStatus

@Composable
fun OnboardingScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val task by viewModel.taskState.collectAsStateWithLifecycle()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome to Clashcards", style = MaterialTheme.typography.headlineLarge)
        Text("Choose how you appear to friends before you start studying.")
        SocialStatus(task)
        ProfileEditor(null, !task.busy, viewModel::createProfile)
        TextButton(onClick = onSignOut, enabled = !task.busy) { Text("Log out") }
    }
}

@Composable
fun ProfileConnectionScreen(
    error: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(error, textAlign = TextAlign.Center, modifier = Modifier.padding(all=24.dp) )
        Button(onClick = onRetry) { Text("Retry") }
        TextButton(onClick = onSignOut) { Text("Log out") }
    }
}
