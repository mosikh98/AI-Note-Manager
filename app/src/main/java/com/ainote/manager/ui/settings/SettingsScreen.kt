package com.ainote.manager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.data.ProviderConfigEntity
import com.ainote.manager.ui.components.ConfirmDialog
import com.ainote.manager.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val configs by viewModel.configs.collectAsState()
    var editorTarget by remember { mutableStateOf<ProviderConfigEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProviderConfigEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editorTarget = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add provider")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Configure any OpenAI-compatible /v1/chat/completions endpoint — OpenAI, OpenRouter, " +
                    "a self-hosted model, or a custom gateway. AI Organize is fully optional: notes work offline without it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            if (configs.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.SmartToy,
                    title = "No AI provider configured",
                    subtitle = "Tap + to add one. You can save several and switch anytime.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        ElevatedCard {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(config.name.ifBlank { config.baseUrl }, style = MaterialTheme.typography.titleMedium)
                                        if (config.isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Active") })
                                        }
                                    }
                                    Text(config.model, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(config.baseUrl, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!config.isActive) {
                                    TextButton(onClick = { viewModel.activate(config.id) }) { Text("Use") }
                                }
                                IconButton(onClick = { editorTarget = config; showEditor = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { deleteTarget = config }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showEditor) {
        ProviderConfigEditorDialog(
            existing = editorTarget,
            existingApiKey = editorTarget?.let { viewModel.getSavedApiKey(it.id) },
            onSave = { name, baseUrl, model, apiKey, orgId, headers, makeActive ->
                viewModel.saveConfig(editorTarget, name, baseUrl, model, apiKey, orgId, headers, makeActive)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    deleteTarget?.let { config ->
        ConfirmDialog(
            title = "Delete provider?",
            message = "“${config.name}” and its saved API key will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.delete(config); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}
