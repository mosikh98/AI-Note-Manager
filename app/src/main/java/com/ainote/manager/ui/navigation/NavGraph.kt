package com.ainote.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ainote.manager.ui.cloud.CloudSettingsScreen
import com.ainote.manager.ui.noteedit.NoteEditScreen
import com.ainote.manager.ui.notes.NotesListScreen
import com.ainote.manager.ui.settings.SettingsScreen

object Routes {
    const val NOTES_LIST = "notes_list"
    const val SETTINGS = "settings"
    const val CLOUD_SETTINGS = "cloud_settings"
    const val NOTE_EDIT = "note_edit/{noteId}"
    fun noteEdit(id: Long) = "note_edit/$id"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.NOTES_LIST) {
        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCloudSettings = { navController.navigate(Routes.CLOUD_SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CLOUD_SETTINGS) {
            CloudSettingsScreen(onBack = { navController.popBackStack() })
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
