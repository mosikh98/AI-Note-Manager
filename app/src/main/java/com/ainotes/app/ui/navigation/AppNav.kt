package com.ainotes.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ainotes.app.AppContainer
import com.ainotes.app.ui.editor.EditorScreen
import com.ainotes.app.ui.home.HomeScreen
import com.ainotes.app.ui.onboarding.OnboardingScreen
import com.ainotes.app.ui.settings.AiProvidersScreen
import com.ainotes.app.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PROVIDERS = "providers"
    const val EDITOR = "editor"
    fun editor(noteId: String) = "editor/$noteId"
}

@Composable
fun AppNav(container: AppContainer, onboardingDone: Boolean) {
    val nav = rememberNavController()
    val start = if (onboardingDone) Routes.HOME else Routes.ONBOARDING
    val easing = tween<androidx.compose.ui.unit.IntOffset>(420)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, easing)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, easing)
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, easing)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, easing)
        }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                container = container,
                onDone = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onOpenNote = { id -> nav.navigate(Routes.editor(id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = "${Routes.EDITOR}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("noteId") ?: return@composable
            EditorScreen(
                container = container,
                noteId = id,
                onBack = { nav.popBackStack() },
                onOpenProviders = { nav.navigate(Routes.PROVIDERS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onOpenProviders = { nav.navigate(Routes.PROVIDERS) }
            )
        }
        composable(Routes.PROVIDERS) {
            AiProvidersScreen(
                container = container,
                onBack = { nav.popBackStack() }
            )
        }
    }
    }
}
