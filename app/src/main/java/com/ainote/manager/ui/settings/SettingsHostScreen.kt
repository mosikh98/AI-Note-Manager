package com.ainote.manager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ainote.manager.ui.cloud.CloudSettingsScreen

enum class SettingsTab { AI_PROVIDER, CLOUD_BACKUP }

/**
 * Single entry point for all app settings — reached from the one gear icon on the notes
 * list. AI Provider and Cloud Backup live as tabs of the same screen instead of separate
 * disconnected screens, so nothing is hidden behind a button that "only opens one part".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHostScreen(
    startTab: SettingsTab = SettingsTab.AI_PROVIDER,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(startTab) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
                )
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == SettingsTab.AI_PROVIDER,
                        onClick = { selectedTab = SettingsTab.AI_PROVIDER },
                        text = { Text("AI Provider") }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.CLOUD_BACKUP,
                        onClick = { selectedTab = SettingsTab.CLOUD_BACKUP },
                        text = { Text("Cloud Backup") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                SettingsTab.AI_PROVIDER -> SettingsScreen()
                SettingsTab.CLOUD_BACKUP -> CloudSettingsScreen()
            }
        }
    }
}
