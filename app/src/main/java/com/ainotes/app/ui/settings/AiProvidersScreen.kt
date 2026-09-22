package com.ainotes.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.AiProviderConfig
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProvidersScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val providers by container.repository.observeProviders()
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<AiProviderConfig?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Providers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showForm = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Provider") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
            )
        ) {
            if (providers.isEmpty()) {
                item {
                    Text(
                        "No providers yet. Add your own OpenAI-compatible API - a VPS gateway, OpenRouter, a local server - anything with /v1/chat/completions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            items(providers, key = { it.id }) { provider ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (provider.isActive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            scope.launch { container.repository.setActiveProvider(provider.id) }
                        }
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = provider.isActive, onClick = {
                            scope.launch { container.repository.setActiveProvider(provider.id) }
                        })
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Model: " + provider.model,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Text(
                                provider.baseUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = {
                            editing = provider
                            showForm = true
                        }) { Text("Edit") }
                        IconButton(onClick = {
                            scope.launch { container.repository.deleteProvider(provider.id) }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        ProviderForm(
            initial = editing,
            onSave = { config ->
                scope.launch {
                    container.repository.saveProvider(config)
                    showForm = false
                }
            },
            onDismiss = { showForm = false }
        )
    }
}

@Composable
private fun ProviderForm(
    initial: AiProviderConfig?,
    onSave: (AiProviderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var org by remember { mutableStateOf(initial?.organizationId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Provider" else "Edit Provider") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                FormField("Provider name", name) { name = it }
                FormField("API URL (base)", baseUrl) { baseUrl = it; }
                FormField("API Key", apiKey) { apiKey = it; }
                FormField("Model", model) { model = it }
                FormField("Organization ID (optional)", org) { org = it }
                Text(
                    "The key is encrypted with the Android Keystore and never written to source or logs.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank(),
                onClick = {
                    onSave(
                        AiProviderConfig(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            baseUrl = baseUrl.trim(),
                            model = model.trim(),
                            apiKey = apiKey.trim(),
                            organizationId = org.trim().ifBlank { null },
                            isActive = initial?.isActive ?: true
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = { Text(label) },
        singleLine = true
    )
}

/** Public wrapper so onboarding can reuse the provider form. */
@Composable
fun ProviderFormDialog(
    container: com.ainotes.app.AppContainer,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    ProviderForm(
        initial = null,
        onSave = { config ->
            scope.launch {
                container.repository.saveProvider(config)
                onSaved()
            }
        },
        onDismiss = onDismiss
    )
}
