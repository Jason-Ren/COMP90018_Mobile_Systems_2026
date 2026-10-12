package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A small "?" in a circle. Tapping it opens a large help panel covering most
 * of the screen. [content] can hold anything: steps, images, buttons, etc.
 *
 * Usage:
 *   HelpTip(title = "Setting up sync") {
 *       Text("1. Open Settings")
 *       Text("2. Tap Sync")
 *   }
 */
@Composable
fun HelpTip(
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    HelpIconButton(size = size, modifier = modifier, onClick = { open = true })

    if (open) {
        HelpDialog(title = title, onDismiss = { open = false }, content = content)
    }
}

/** Simple text-only version. */
@Composable
fun HelpTip(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) = HelpTip(title = title, modifier = modifier, size = size) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun HelpIconButton(
    size: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier =
            modifier
                .size(size)
                .semantics { contentDescription = "Help" }
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = (size.value * 0.6f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HelpDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Lets us size the dialog ourselves instead of the narrow platform default
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Scrollable body, takes all remaining space
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )

                Spacer(Modifier.size(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Got it")
                }
            }
        }
    }
}

/** Convenience: a label with a HelpTip beside it. */
@Composable
fun LabelWithHelp(
    label: String,
    helpTitle: String,
    modifier: Modifier = Modifier,
    helpContent: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        HelpTip(title = helpTitle, content = helpContent)
    }
}
