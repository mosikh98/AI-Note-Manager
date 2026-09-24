package com.ainote.manager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.data.ProviderConfigEntity

/**
 * Full config form: Provider name, API Base URL, API Key, Model name, optional Org ID,
 * optional custom headers — matches the spec's "must be fully configurable" requirement.
 * Fields start empty (no pre-filled provider/model) so the user always enters their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigEditorDialog(
    existing: ProviderConfigEntity?,
    existingApiKey: String?,
    onSave: (name: String, baseUrl: String, model: String, apiKey: String, orgId: String, headers: Map<String, String>, makeActive: Boolean) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var apiKey by remember { mutableStateOf(existingApiKey ?: "") }
    var orgId by remember { mutableStateOf(existing?.organizationId ?: "") }
    var headersText by remember { mutableStateOf(existing?.customHeadersJson?.takeIf { it != "{}" } ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var makeActive by remember { mutableStateOf(existing == null || existing.isActive) }
    val testState by viewModel.testState.collectAsState()

    fun parsedHeaders(): Map<String, String> = try {
        if (headersText.isBlank()) emptyMap() else {
            val obj = org.json.JSONObject(headersText)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }
    } catch (e: Exception) { emptyMap() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add AI Provider" else "Edit AI Provider") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Provider name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. OpenAI") })
                OutlinedTextField(baseUrl, { baseUrl = it; viewModel.resetTestState() }, label = { Text("API Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.openai.com/v1") },
                    supportingText = { Text("e.g. https://api.openai.com/v1 or https://openrouter.ai/api/v1 or your own server") })
                OutlinedTextField(model, { model = it; viewModel.resetTestState() }, label = { Text("Model name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. gpt-4o-mini") })
                OutlinedTextField(
                    apiKey, { apiKey = it; viewModel.resetTestState() }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                    supportingText = { Text("Stored encrypted on-device (Android Keystore). Never sent anywhere except this endpoint.") }
                )
                OutlinedTextField(orgId, { orgId = it }, label = { Text("Organization ID (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    headersText, { headersText = it },
                    label = { Text("Custom headers (optional, JSON)") },
                    placeholder = { Text("""{"X-Custom-Header":"value"}""") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { viewModel.testConnection(baseUrl, apiKey, model, orgId, parsedHeaders()) },
                        enabled = testState !is ConnectionTestState.Testing
                    ) { Text("Test connection") }
                    Spacer(Modifier.width(10.dp))
                    when (val state = testState) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = makeActive, onCheckedChange = { makeActive = it })
                    Text("Use as active provider")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank(),
                onClick = { onSave(name, baseUrl, model, apiKey, orgId, parsedHeaders(), makeActive) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
