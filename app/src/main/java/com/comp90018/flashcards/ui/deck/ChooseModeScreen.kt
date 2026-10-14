package com.comp90018.flashcards.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.comp90018.flashcards.R
import com.comp90018.flashcards.ui.help.HelpTopic
import com.comp90018.flashcards.ui.help.helpTip

/**
 * The ways a deck can be played.
 */
private enum class PlayMode(
    val title: String,
    val description: String,
    val isAvailable: Boolean,
    val helpTip: HelpTopic,
) {
    SOLO(
        "Solo",
        "Study the deck on your own, at your own pace." +
            "\nKeeps track of your progress for effective revision.",
        isAvailable = true,
        HelpTopic.SoloPlayHelp,
    ),
    DUO(
        "Duo",
        "Study together with a friend. One player describes the card, the other guesses the answer.",
        isAvailable = true,
        HelpTopic.DuoPlayHelp,
    ),
    TEAM_MODE(
        "Clash",
        "A competitive Duo match for 2 teams of two." +
            "\n Requires 2 devices with ClashCards installed.",
        isAvailable = true,
        HelpTopic.MultiSetupHelp,
    ),
    ;

    @Composable
    fun iconPainter(): Painter =
        when (this) {
            SOLO -> rememberVectorPainter(Icons.Default.Person)
            DUO -> painterResource(R.drawable.ic_communication)
            TEAM_MODE -> painterResource(R.drawable.ic_swords)
        }
}

/**
 * Choose-mode page: pick how to play the deck. Modes that are not built yet are shown disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseModeScreen(
    onNavigateBack: () -> Unit,
    onPlaySolo: (String) -> Unit,
    onPlayDuo: (String) -> Unit,
    onPlayTeamMode: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Choose mode")
                        Text(uiState.deckName.ifBlank { "Deck" }, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayMode.entries.forEach { mode ->
                PlayModeButton(
                    mode = mode,
                    // Each option may shrink, so all three stay visible on a short landscape screen.
                    modifier = Modifier.weight(1f, fill = false),
                    onClick = {
                        when (mode) {
                            PlayMode.SOLO -> onPlaySolo(viewModel.deckId)
                            PlayMode.DUO -> onPlayDuo(viewModel.deckId)
                            PlayMode.TEAM_MODE -> onPlayTeamMode(viewModel.deckId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayModeButton(
    mode: PlayMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // fillMaxWidth first, then the cap, so every row has exactly the same width.
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Button(
                onClick = onClick,
                enabled = mode.isAvailable,
                modifier = Modifier.weight(1f).height(72.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(mode.iconPainter(), contentDescription = null, modifier = Modifier.size(32.dp))
                    Text(mode.title, style = MaterialTheme.typography.titleLarge)
                }
            }
            helpTip(mode.helpTip, size = 48.dp)
        }
        Text(
            text = if (mode.isAvailable) mode.description else "${mode.description} (coming soon)",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
        )
    }
}
