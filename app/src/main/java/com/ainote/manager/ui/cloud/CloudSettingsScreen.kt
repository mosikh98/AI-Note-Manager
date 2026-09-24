package com.ainote.manager.ui.cloud

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.cloud.GoogleDriveService
import com.ainote.manager.data.CloudConfigEntity
import com.ainote.manager.data.CloudProviderType
import com.ainote.manager.ui.components.ConfirmDialog
import com.ainote.manager.ui.components.EmptyState
import com.google.android.gms.auth.api.signin.GoogleSignIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSettingsScreen(
    viewModel: CloudSettingsViewModel = viewModel(),
) {
    val configs by viewModel.configs.collectAsState()
    var editorTarget by remember { mutableStateOf<CloudConfigEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CloudConfigEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Notes and attachments are only ever uploaded when you tap \u201cBackup to cloud\u201d on a note — " +
                "nothing is sent off-device silently. Add a custom server, or connect Google Drive / Dropbox directly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        Box(Modifier.weight(1f)) {
            if (configs.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "No cloud storage configured",
                    subtitle = "Tap + to connect Google Drive, Dropbox, or your own server.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(configs, key = { it.id }) { config ->
                        ElevatedCard {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(config.name.ifBlank { providerLabel(config.provider) }, style = MaterialTheme.typography.titleMedium)
                                        if (config.isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Active") })
                                        }
                                    }
                                    Text(providerLabel(config.provider), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    val subtitle = when (config.provider) {
                                        CloudProviderType.GOOGLE_DRIVE -> config.accountLabel ?: "Not connected"
                                        CloudProviderType.DROPBOX -> "Access token saved"
                                        CloudProviderType.CUSTOM_SERVER -> config.serverUrl
                                    }
                                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
            FilledTonalButton(onClick = { editorTarget = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add cloud destination")
            }
        }
    }

    if (showEditor) {
        CloudConfigEditorDialog(
            existing = editorTarget,
            existingSecret = editorTarget?.let { viewModel.getSavedSecret(it.id) },
            viewModel = viewModel,
            onSave = { name, provider, url, accountLabel, secret, makeActive ->
                viewModel.saveConfig(editorTarget, name, provider, url, accountLabel, secret, makeActive)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    deleteTarget?.let { config ->
        ConfirmDialog(
            title = "Remove cloud destination?",
            message = "\u201c${config.name}\u201d and any saved credentials will be removed.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { viewModel.delete(config); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

private fun providerLabel(provider: CloudProviderType): String = when (provider) {
    CloudProviderType.CUSTOM_SERVER -> "Custom server"
    CloudProviderType.GOOGLE_DRIVE -> "Google Drive"
    CloudProviderType.DROPBOX -> "Dropbox"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudConfigEditorDialog(
    existing: CloudConfigEntity?,
    existingSecret: String?,
    viewModel: CloudSettingsViewModel,
    onSave: (name: String, provider: CloudProviderType, serverUrl: String, accountLabel: String?, secret: String, makeActive: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var provider by remember { mutableStateOf(existing?.provider ?: CloudProviderType.CUSTOM_SERVER) }
    var serverUrl by remember { mutableStateOf(existing?.serverUrl ?: "") }
    var secret by remember { mutableStateOf(existingSecret ?: "") }
    var showSecret by remember { mutableStateOf(false) }
    var accountLabel by remember { mutableStateOf(existing?.accountLabel) }
    var makeActive by remember { mutableStateOf(existing == null || existing.isActive) }
    val testState by viewModel.testState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(com.google.android.gms.common.api.ApiException::class.java)
            accountLabel = GoogleDriveService.extractAccountEmail(account)
        } catch (e: Exception) {
            // Sign-in was cancelled or failed — leave accountLabel as-is so the user can retry.
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Cloud Storage" else "Edit Cloud Storage") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = provider == CloudProviderType.GOOGLE_DRIVE, onClick = { provider = CloudProviderType.GOOGLE_DRIVE; viewModel.resetTestState() }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Drive") }
                    SegmentedButton(selected = provider == CloudProviderType.DROPBOX, onClick = { provider = CloudProviderType.DROPBOX; viewModel.resetTestState() }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Dropbox") }
                    SegmentedButton(selected = provider == CloudProviderType.CUSTOM_SERVER, onClick = { provider = CloudProviderType.CUSTOM_SERVER; viewModel.resetTestState() }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("Custom") }
                }

                when (provider) {
                    CloudProviderType.GOOGLE_DRIVE -> {
                        Text(
                            "Signs in with your Google account and backs up into a \u201cAI Note Manager Backups\u201d folder in your Drive. Requires one-time setup — see README \u201cConnecting Google Drive\u201d.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        if (accountLabel != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Connected: $accountLabel", style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { signInLauncher.launch(GoogleDriveService.signInIntent(context)) }) { Text("Switch account") }
                        } else {
                            Button(onClick = { signInLauncher.launch(GoogleDriveService.signInIntent(context)) }) {
                                Text("Connect Google account")
                            }
                        }
                    }
                    CloudProviderType.DROPBOX -> {
                        Text(
                            "Paste a Dropbox access token — generate one at dropbox.com/developers/apps \u2192 your app \u2192 \u201cGenerated access token\u201d. See README \u201cConnecting Dropbox\u201d.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            secret, { secret = it }, label = { Text("Access token") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { TextButton(onClick = { showSecret = !showSecret }) { Text(if (showSecret) "Hide" else "Show") } }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { viewModel.testDropboxToken(secret) }, enabled = secret.isNotBlank() && testState !is ConnectionTestState.Testing) {
                                Text("Test connection")
                            }
                            Spacer(Modifier.width(10.dp))
                            TestStateIndicator(testState)
                        }
                    }
                    CloudProviderType.CUSTOM_SERVER -> {
                        OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://backup.example.com") },
                            supportingText = { Text("Your backend's base URL — the app POSTs to <url>/notes") })
                        OutlinedTextField(
                            secret, { secret = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { TextButton(onClick = { showSecret = !showSecret }) { Text(if (showSecret) "Hide" else "Show") } },
                            supportingText = { Text("Stored encrypted on-device.") }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = makeActive, onCheckedChange = { makeActive = it })
                    Text("Use as active cloud destination")
                }
            }
        },
        confirmButton = {
            val canSave = when (provider) {
                CloudProviderType.GOOGLE_DRIVE -> accountLabel != null
                CloudProviderType.DROPBOX -> secret.isNotBlank()
                CloudProviderType.CUSTOM_SERVER -> serverUrl.isNotBlank()
            }
            TextButton(
                enabled = canSave,
                onClick = { onSave(name, provider, serverUrl, accountLabel, secret, makeActive) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TestStateIndicator(state: ConnectionTestState) {
    when (state) {
        is ConnectionTestState.Testing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        is ConnectionTestState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Works", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
        is ConnectionTestState.Failure -> Text(state.message.take(60), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        is ConnectionTestState.Idle -> {}
    }
}
