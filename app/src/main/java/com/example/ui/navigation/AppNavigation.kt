package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.JarvisViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiAgentsScreen
import com.example.ui.screens.AiProviderManagerScreen
import com.example.ui.screens.AiUsageDashboardScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DiagnosticScreen
import com.example.ui.screens.GitHubSkillsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.NotificationIntelligenceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PersonalizationStudioScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ResearchAndFactScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkillsMarketplaceScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SyncManagerScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.VoiceHudScreen
import com.example.ui.screens.WorkspacesScreen
import com.example.ui.theme.MyApplicationTheme

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: JarvisViewModel = viewModel()
) {
    val onboardingCompleted by viewModel.preferences.onboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = false)
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    MyApplicationTheme(themeConfig = currentTheme) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    if (onboardingCompleted) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            MainDashboardScreen(
                viewModel = viewModel,
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToVoiceHud = { navController.navigate(Screen.VoiceHud.route) },
                onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                onNavigateToPersonalization = { navController.navigate(Screen.Personalization.route) },
                onNavigateToAiProviders = { navController.navigate(Screen.AiProviders.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VoiceHud.route) {
            VoiceHudScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notes.route) {
            NotesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tasks.route) {
            TasksScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAiProviders = { navController.navigate(Screen.AiProviders.route) },
                onNavigateToPersonalization = { navController.navigate(Screen.Personalization.route) },
                onNavigateToAiAgents = { navController.navigate(Screen.AiAgents.route) },
                onNavigateToWorkspaces = { navController.navigate(Screen.Workspaces.route) },
                onNavigateToSkills = { navController.navigate(Screen.Skills.route) },
                onNavigateToRoutines = { navController.navigate(Screen.Routines.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToUsage = { navController.navigate(Screen.Usage.route) },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToResearch = { navController.navigate(Screen.Research.route) },
                onNavigateToGitHubSkills = { navController.navigate(Screen.GitHubSkills.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiProviders.route) {
            AiProviderManagerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Personalization.route) {
            PersonalizationStudioScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiAgents.route) {
            AiAgentsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Workspaces.route) {
            WorkspacesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Skills.route) {
            SkillsMarketplaceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Routines.route) {
            RoutinesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Usage.route) {
            AiUsageDashboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Sync.route) {
            SyncManagerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationIntelligenceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Research.route) {
            ResearchAndFactScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GitHubSkills.route) {
            GitHubSkillsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}
