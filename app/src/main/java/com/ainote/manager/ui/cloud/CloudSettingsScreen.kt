package com.ainote.manager.ui.cloud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.data.CloudConfigEntity
import com.ainote.manager.ui.components.ConfirmDialog
import com.ainote.manager.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSettingsScreen(
    onBack: () -> Unit,
    viewModel: CloudSettingsViewModel = viewModel(),
) {
    val configs by viewModel.configs.collectAsState()
    var editorTarget by remember { mutableStateOf<CloudConfigEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CloudConfigEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Backup") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editorTarget = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add cloud destination")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Notes and attachments are only ever uploaded when you tap “Backup to cloud” on a note — " +
                    "nothing is sent off-device silently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            if (configs.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "No cloud storage configured",
                    subtitle = "Tap + to connect a server URL and API key.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(configs, key = { it.id }) { config ->
                        ElevatedCard {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(config.name.ifBlank { config.serverUrl }, style = MaterialTheme.typography.titleMedium)
                                        if (config.isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Active") })
                                        }
                                    }
                                    Text(config.serverUrl, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!config.isActive) TextButton(onClick = { viewModel.activate(config.id) }) { Text("Use") }
                                IconButton(onClick = { editorTarget = config; showEditor = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { deleteTarget = config }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showEditor) {
        CloudConfigEditorDialog(
            existing = editorTarget,
            existingApiKey = editorTarget?.let { viewModel.getSavedApiKey(it.id) },
            onSave = { name, url, key, makeActive ->
                viewModel.saveConfig(editorTarget, name, url, key, makeActive)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    deleteTarget?.let { config ->
        ConfirmDialog(
            title = "Remove cloud destination?",
            message = "“${config.name}” and its saved API key will be removed.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { viewModel.delete(config); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudConfigEditorDialog(
    existing: CloudConfigEntity?,
    existingApiKey: String?,
    onSave: (name: String, serverUrl: String, apiKey: String, makeActive: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var serverUrl by remember { mutableStateOf(existing?.serverUrl ?: "") }
    var apiKey by remember { mutableStateOf(existingApiKey ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var makeActive by remember { mutableStateOf(existing == null || existing.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Cloud Storage" else "Edit Cloud Storage") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Your backend's base URL, e.g. https://backup.example.com") })
                OutlinedTextField(
                    apiKey, { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                    supportingText = { Text("Stored encrypted on-device.") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = makeActive, onCheckedChange = { makeActive = it })
                    Text("Use as active cloud destination")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, serverUrl, apiKey, makeActive) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
