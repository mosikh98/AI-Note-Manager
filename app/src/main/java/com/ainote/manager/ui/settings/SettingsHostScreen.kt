package com.ainote.manager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ainote.manager.ui.cloud.CloudSettingsScreen
import com.ainote.manager.ui.theme.AppearancePreferences
import com.ainote.manager.ui.theme.AppearanceSettings
import com.ainote.manager.ui.theme.Violet40
import kotlinx.coroutines.launch

enum class SettingsTab { AI_PROVIDER, CLOUD_BACKUP, APPEARANCE }

/** Single entry point for AI, cloud, and app appearance settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHostScreen(
    startTab: SettingsTab = SettingsTab.AI_PROVIDER,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(startTab) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appearance by AppearancePreferences.observe(context).collectAsState(initial = AppearanceSettings())

    fun updateAppearance(updated: AppearanceSettings) {
        scope.launch { AppearancePreferences.update(context, updated) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
                )
                ScrollableTabRow(selectedTabIndex = selectedTab.ordinal) {
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
                    Tab(
                        selected = selectedTab == SettingsTab.APPEARANCE,
                        onClick = { selectedTab = SettingsTab.APPEARANCE },
                        text = { Text("Appearance") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                SettingsTab.AI_PROVIDER -> SettingsScreen()
                SettingsTab.CLOUD_BACKUP -> CloudSettingsScreen()
                SettingsTab.APPEARANCE -> AppearanceSettingsScreen(appearance, ::updateAppearance)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsScreen(
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
) {
    var fontScale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = settings.theme == value,
                        onClick = { onChange(settings.copy(theme = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(label) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Accent color", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    "violet" to Violet40,
                    "ocean" to Color(0xFF1769AA),
                    "forest" to Color(0xFF35734A),
                    "amber" to Color(0xFF8A5700),
                    "rose" to Color(0xFFB23A5B),
                ).forEach { (name, color) ->
                    FilterChip(
                        selected = settings.accent == name,
                        onClick = { onChange(settings.copy(accent = name)) },
                        label = { Text(name.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = {
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = CircleShape,
                                color = color,
                            ) {}
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Background", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("default" to "Default", "paper" to "Paper", "mist" to "Mist").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = settings.background == value,
                        onClick = { onChange(settings.copy(background = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(label) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Text size", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = fontScale,
                onValueChange = { fontScale = it },
                onValueChangeFinished = { onChange(settings.copy(fontScale = fontScale)) },
                valueRange = 0.85f..1.3f,
                steps = 8,
            )
            Text("Aa  Notes should be comfortable to read", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
