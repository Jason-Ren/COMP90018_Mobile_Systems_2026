package com.comp90018.flashcards.ui.card

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val frontFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.userMessageShown()
            frontFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            AddEditCardTopBar(
                isEditing = uiState.isEditing,
                onNavigateBack = onNavigateBack,
                onDeleteClick = { showDeleteDialog = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.front,
                onValueChange = { viewModel.onFrontChange(it) },
                minLines = 3,
                label = { Text("Front") },
                modifier = Modifier.fillMaxWidth().focusRequester(frontFocusRequester),
            )
            OutlinedTextField(
                value = uiState.back,
                onValueChange = { viewModel.onBackChange(it) },
                minLines = 3,
                label = { Text("Back") },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.saveCard()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.front.isNotBlank() && uiState.back.isNotBlank(),
                ) {
                    Text("Save Card", style = MaterialTheme.typography.titleMedium)
                }
                if (!uiState.isEditing) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveAndAddAnother()
                            frontFocusRequester.requestFocus()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.front.isNotBlank() && uiState.back.isNotBlank(),
                    ) {
                        Text("Save + Add Another Card", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteCardDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCard()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCardTopBar(
    isEditing: Boolean,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(if (isEditing) "Edit Card" else "Add Card") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (isEditing) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Card",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun DeleteCardDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Card") },
        text = { Text("Are you sure you want to delete this card? This action cannot be reversed.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
