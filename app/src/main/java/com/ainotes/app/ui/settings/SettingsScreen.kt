package com.ainotes.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.ExportFormat
import com.ainotes.app.settings.ThemeMode
import com.ainotes.app.ui.components.SectionLabel
import com.ainotes.app.ui.theme.hexToColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ainotes.app.sync.CustomCloudConfig
import com.ainotes.app.sync.CustomCloudStorage

private val ACCENTS = listOf("#7C4DFF", "#00A0A8", "#E91E63", "#FF6D00", "#2E7D32", "#3D5AFE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit
) {
    val themeMode by container.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val accent by container.settings.accent.collectAsState(initial = "#7C4DFF")
    val defaultExport by container.settings.defaultExport.collectAsState(initial = "PDF")
    val customPrompt by container.settings.customPrompt.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    var showPrompt by remember { mutableStateOf(false) }
    var promptDraft by remember { mutableStateOf(customPrompt) }
    var showCloud by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var backingUp by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var cloudUrl by remember {
        mutableStateOf(container.secureStore.get("cloud_url") ?: "")
    }
    var cloudKey by remember {
        mutableStateOf(if (container.secureStore.get("cloud_key") == null) "" else "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Appearance")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { container.settings.setTheme(mode) } },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ACCENTS.forEach { hex ->
                    Surface(
                        shape = CircleShape,
                        color = hexToColor(hex),
                        border = if (accent == hex) {
                            androidx.compose.foundation.BorderStroke(
                                3.dp,
                                MaterialTheme.colorScheme.onBackground
                            )
                        } else null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                            .clickable { scope.launch { container.settings.setAccent(hex) } }
                    ) {
                        Box(Modifier.size(26.dp))
                    }
                }
            }

            SectionLabel("AI")
            SettingRow("AI Providers", "Configure your own API endpoint") { onOpenProviders() }
            SettingRow("AI Instructions", "Customize the organization prompt") { showPrompt = true }
            SettingRow("Privacy", "Note content is sent to your configured AI server only when you use AI actions") { }

            SectionLabel("Export")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = defaultExport == format.name,
                        onClick = { scope.launch { container.settings.setDefaultExport(format.name) } },
                        label = { Text(format.extension.uppercase()) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            SectionLabel("Cloud storage")
            SettingRow("Custom storage server", "Server URL and API key for backup") { showCloud = true }
            SettingRow(
                "Backup now",
                if (backingUp) "Uploading..." else "Upload all notes to your server - explicit action, never automatic"
            ) { showBackup = true }

            SectionLabel("About")
            SettingRow("App version", "1.0.0") { }
            SettingRow("Open-source licenses", "Compose, Room, OkHttp, WorkManager") { }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text("AI Instructions") },
            text = {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text(com.ainotes.app.ai.PromptManager.DEFAULT_SYSTEM_PROMPT) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.settings.setCustomPrompt(promptDraft) }
                    showPrompt = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        promptDraft = com.ainotes.app.ai.PromptManager.DEFAULT_SYSTEM_PROMPT
                    }) { Text("Reset") }
                    TextButton(onClick = { showPrompt = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showBackup) {
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text("Upload to your server?") },
            text = {
                Text(
                    "Your notes will be sent to: " +
                        (cloudUrl.ifBlank { "(no server URL set yet)" }) +
                        ".\n\nUploading only ever happens when you tap Backup - nothing is sent silently."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !backingUp,
                    onClick = {
                        showBackup = false
                        backingUp = true
                        cloudStatus = null
                        scope.launch(Dispatchers.IO) {
                            val url = container.secureStore.get("cloud_url") ?: ""
                            val key = container.secureStore.get("cloud_key") ?: ""
                            cloudStatus = if (url.isBlank()) {
                                "Set a server URL first (Cloud storage settings)."
                            } else {
                                val notes = runCatching { container.repository.allNotes() }
                                    .getOrDefault(emptyList())
                                val storage = CustomCloudStorage(CustomCloudConfig(url, key))
                                val ok = runCatching { storage.backup(notes) }.getOrDefault(false)
                                if (ok) "Uploaded ${notes.size} notes successfully."
                                else "Upload failed - check your server URL and API key."
                            }
                            backingUp = false
                        }
                    }
                ) { Text("Backup now") }
            },
            dismissButton = {
                TextButton(onClick = { showBackup = false }) { Text("Cancel") }
            }
        )
    }

    cloudStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { cloudStatus = null },
            title = { Text("Backup result") },
            text = { Text(status) },
            confirmButton = {
                TextButton(onClick = { cloudStatus = null }) { Text("OK") }
            }
        )
    }

    if (showCloud) {
        AlertDialog(
            onDismissRequest = { showCloud = false },
            title = { Text("Custom cloud storage") },
            text = {
                Column {
                    OutlinedTextField(
                        value = cloudUrl,
                        onValueChange = { cloudUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/api") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cloudKey,
                        onValueChange = { cloudKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("API key") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Stored encrypted on this device. Never committed to source control.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    container.secureStore.put("cloud_url", cloudUrl)
                    if (cloudKey.isNotBlank() && !cloudKey.contains("\u2022")) {
                        container.secureStore.put("cloud_key", cloudKey)
                    }
                    showCloud = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCloud = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
