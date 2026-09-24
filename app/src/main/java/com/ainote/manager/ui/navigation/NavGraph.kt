package com.ainote.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ainote.manager.ui.noteedit.NoteEditScreen
import com.ainote.manager.ui.notes.NotesListScreen
import com.ainote.manager.ui.settings.SettingsHostScreen
import com.ainote.manager.ui.settings.SettingsTab

object Routes {
    const val NOTES_LIST = "notes_list"
    const val SETTINGS = "settings/{tab}"
    const val NOTE_EDIT = "note_edit/{noteId}"
    fun noteEdit(id: Long) = "note_edit/$id"
    fun settings(tab: SettingsTab) = "settings/${tab.name}"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.NOTES_LIST) {
        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                onOpenSettings = { navController.navigate(Routes.settings(SettingsTab.AI_PROVIDER)) },
            )
        }
        composable(
            route = Routes.SETTINGS,
            arguments = listOf(navArgument("tab") { type = NavType.StringType })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab")
                ?.let { runCatching { SettingsTab.valueOf(it) }.getOrNull() }
                ?: SettingsTab.AI_PROVIDER
            SettingsHostScreen(startTab = tab, onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditScreen(noteId = noteId, onBack = { navController.popBackStack() })
        }
    }
}
