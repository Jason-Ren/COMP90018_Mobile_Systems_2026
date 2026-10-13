package com.comp90018.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.comp90018.flashcards.ui.account.AccountScreen
import com.comp90018.flashcards.ui.account.OnboardingScreen
import com.comp90018.flashcards.ui.account.ProfileConnectionScreen
import com.comp90018.flashcards.ui.auth.LoginScreen
import com.comp90018.flashcards.ui.auth.SessionStage
import com.comp90018.flashcards.ui.auth.SessionViewModel
import com.comp90018.flashcards.ui.card.AddEditCardScreen
import com.comp90018.flashcards.ui.components.NavBar
import com.comp90018.flashcards.ui.connection.NearbyPermissionGate
import com.comp90018.flashcards.ui.deck.ChooseModeScreen
import com.comp90018.flashcards.ui.deck.DeckListScreen
import com.comp90018.flashcards.ui.deck.DeckPageScreen
import com.comp90018.flashcards.ui.lobby.GameSetupViewModel
import com.comp90018.flashcards.ui.lobby.GuestLobbyScreen
import com.comp90018.flashcards.ui.lobby.HostLobbyScreen
import com.comp90018.flashcards.ui.lobby.JoinGameScreen
import com.comp90018.flashcards.ui.play.ChooseTimerScreen
import com.comp90018.flashcards.ui.play.DuoScreen
import com.comp90018.flashcards.ui.play.TeamGameScreen
import com.comp90018.flashcards.ui.social.CommunityScreen
import com.comp90018.flashcards.ui.study.StudyScreen
import com.comp90018.flashcards.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FlashcardApp()
                }
            }
        }
    }
}

@Composable
private fun FlashcardApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.session.collectAsState()
    key(session.user?.uid, session.stage) {
        when (session.stage) {
            SessionStage.LOADING ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            SessionStage.SIGNED_OUT -> LoginScreen()
            SessionStage.ONBOARDING ->
                OnboardingScreen(
                    onSignOut = sessionViewModel::signOut,
                    viewModel = hiltViewModel(key = "onboarding_${session.user?.uid}"),
                )
            SessionStage.DELETING ->
                AccountScreen(
                    {},
                    sessionViewModel::signOut,
                    deleting = true,
                    viewModel = hiltViewModel(key = "deleting_${session.user?.uid}"),
                )
            SessionStage.UNAVAILABLE ->
                ProfileConnectionScreen(
                    session.error.orEmpty(),
                    sessionViewModel::retry,
                    sessionViewModel::signOut,
                )
            SessionStage.READY ->
                FlashcardNavHost(
                    navController = rememberNavController(),
                    displayName = session.profile?.displayName.orEmpty(),
                    onSignOut = sessionViewModel::signOut,
                )
        }
    }
}

@Composable
private fun FlashcardNavHost(
    navController: NavHostController,
    displayName: String,
    onSignOut: () -> Unit,
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("account") {
            AccountScreen(onBack = { navController.popBackStack() }, onSignOut = onSignOut)
        }
        composable("home") {
            DeckListScreen(
                onCommunity = { navController.navigate("community") },
                onAccount = { navController.navigate("account") },
                displayName = displayName,
                onNavigateToDeck = { deckId ->
                    navController.navigate("deck/$deckId")
                },
                onJoinGame = { navController.navigate("join") },
                onNavigateBack = {navController.popBackStack()},
                onHome = {navController.navigate("home")} ,

            )
        }
        deckPlayRoutes(navController)
        communityRoutes(navController)
        duoRoutes(navController)
        teamGameRoutes(navController)
        joinRoutes(navController)
        composable(
            route = "add_edit_card/{deckId}?cardId={cardId}",
            arguments =
                listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("cardId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) {
            AddEditCardScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "study/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) {
            StudyScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("NaviBar"){
            NavBar(
                onCommunity = { navController.navigate("community") },
                onHome = {navController.navigate("home")},
                onJoinGame = { navController.navigate("join") },

                )

        }
    }
}

/**
 * The deck page and the choose-mode page that follows it when the user taps Play.
 */
private fun NavGraphBuilder.deckPlayRoutes(navController: NavHostController) {
    composable(
        route = "deck/{deckId}",
        arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
    ) {
        DeckPageScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlay = { deckId ->
                navController.navigate("choose_mode/$deckId")
            },
            onAddCard = { deckId ->
                navController.navigate("add_edit_card/$deckId")
            },
            onEditCard = { deckId, cardId ->
                navController.navigate("add_edit_card/$deckId?cardId=$cardId")
            },
            onCommunity = { deckId -> navController.navigate("cloud_deck/$deckId") },

            onJoinGame = { navController.navigate("join") },

            onOpenSource = { deckId -> navController.navigate("deck/$deckId") },
            onOpenLocal = { deckId -> navController.navigate("deck/$deckId") },
            onFriends = { navController.navigate("community?friends=true") },
        )
    }
    composable(
        route = "choose_mode/{deckId}",
        arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
    ) {
        ChooseModeScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlaySolo = { deckId ->
                navController.navigate("study/$deckId")
            },
            onPlayDuo = { deckId ->
                navController.navigate("choose_timer/$deckId")
            },
            onPlayTeamMode = { deckId ->
                navController.navigate("host_lobby/$deckId")
            },
        )
    }
}

private fun NavGraphBuilder.communityRoutes(navController: NavHostController) {
    composable(
        "community?friends={friends}",
        arguments =
            listOf(
                navArgument("friends") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { entry ->
        CommunityScreen(
            onBack = { navController.popBackStack() },
            onOpenDeck = { navController.navigate("deck/$it") },
            startWithFriends = entry.arguments?.getBoolean("friends") == true,
            onNavigateBack = { navController.popBackStack() },
            onCommunity = { navController.navigate("community?friends=true") },
            onHome = { navController.navigate("home")} ,
            onJoinGame = { navController.navigate("join") },



        )
    }
    composable("cloud_deck/{deckId}", arguments = listOf(navArgument("deckId") { type = NavType.StringType })) {
        DeckPageScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlay = { deckId -> navController.navigate("choose_mode/$deckId") },
            onAddCard = { deckId -> navController.navigate("add_edit_card/$deckId") },
            onEditCard = { deckId, cardId -> navController.navigate("add_edit_card/$deckId?cardId=$cardId") },
            onOpenSource = { deckId -> navController.navigate("deck/$deckId") },
            onOpenLocal = { deckId -> navController.navigate("deck/$deckId") },
            onFriends = { navController.navigate("community?friends=true") },
        )
    }
}

/**
 * The 2-player flow: pick the countdown, then play. Finishing a round returns to choose mode.
 */
private fun NavGraphBuilder.duoRoutes(navController: NavHostController) {
    composable(
        route = "choose_timer/{deckId}",
        arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
    ) {
        ChooseTimerScreen(
            onNavigateBack = { navController.popBackStack() },
            onStart = { deckId, seconds ->
                navController.navigate("duo/$deckId/$seconds")
            },
        )
    }
    composable(
        route = "duo/{deckId}/{seconds}",
        arguments =
            listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("seconds") { type = NavType.IntType },
            ),
    ) {
        DuoScreen(
            onNavigateBack = { navController.popBackStack("choose_mode/{deckId}", inclusive = false) },
        )
    }
}

/**
 * The host's clash mode flow: lobby, pick the countdown, then play. The guest's flow is in [joinRoutes].
 * Finishing a game returns to the main screen.
 */
private fun NavGraphBuilder.teamGameRoutes(navController: NavHostController) {
    composable(
        route = "host_lobby/{deckId}",
        arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
    ) { entry ->
        val deckId = entry.arguments?.getString("deckId").orEmpty()
        NearbyPermissionGate(onNavigateBack = { navController.popBackStack() }) {
            HostLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onStart = { navController.navigate("choose_timer_team/$deckId") },
            )
        }
    }
    composable(
        route = "choose_timer_team/{deckId}",
        arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
    ) {
        val gameSetup: GameSetupViewModel = hiltViewModel()
        ChooseTimerScreen(
            onNavigateBack = { navController.popBackStack() },
            onStart = { deckId, seconds ->
                gameSetup.startGame(deckId, seconds) { navController.navigate("team_game") }
            },
        )
    }
    composable(route = "team_game") { TeamGameScreen(onNavigateBack = { navController.popBackStack("home", false) }) }
}

/** The guest's clash mode flow: find a host, wait for Start, then play. */
private fun NavGraphBuilder.joinRoutes(navController: NavHostController) {
    composable(route = "join") {
        NearbyPermissionGate(onNavigateBack = { navController.popBackStack() }) {
            JoinGameScreen(
                onNavigateBack = { navController.popBackStack() },
                // the join screen is dropped so back from the game cannot return to it
                onJoin = { navController.navigate("guest_lobby") { launchSingleTop = true } },
            )
        }
    }
    composable(route = "guest_lobby") { entry ->
        // share the join screen's view model, it holds the team names and the host's Start
        val joinEntry = remember(entry) { navController.getBackStackEntry("join") }
        GuestLobbyScreen(
            onNavigateBack = { navController.popBackStack("home", inclusive = false) },
            // the join and lobby screens are both dropped so back from the game cannot return to them
            onGameStart = {
                navController.navigate("team_game") { popUpTo("join") { inclusive = true } }
            },
            viewModel = hiltViewModel(joinEntry),
        )
    }
}
