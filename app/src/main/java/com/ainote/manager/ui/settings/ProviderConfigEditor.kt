package com.ainote.manager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ainote.manager.data.ProviderConfigEntity

/**
 * Full config form: Provider name, API Base URL, API Key, Model name, optional Org ID,
 * optional custom headers — matches the spec's "must be fully configurable" requirement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigEditorDialog(
    existing: ProviderConfigEntity?,
    existingApiKey: String?,
    onSave: (name: String, baseUrl: String, model: String, apiKey: String, orgId: String, headers: Map<String, String>, makeActive: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "https://api.openai.com/v1") }
    var model by remember { mutableStateOf(existing?.model ?: "gpt-4o-mini") }
    var apiKey by remember { mutableStateOf(existingApiKey ?: "") }
    var orgId by remember { mutableStateOf(existing?.organizationId ?: "") }
    var headersText by remember { mutableStateOf(existing?.customHeadersJson?.takeIf { it != "{}" } ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var makeActive by remember { mutableStateOf(existing == null || existing.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add AI Provider" else "Edit AI Provider") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Provider name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("API Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("e.g. https://api.openai.com/v1 or https://openrouter.ai/api/v1 or your own server") })
                OutlinedTextField(model, { model = it }, label = { Text("Model name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    apiKey, { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = makeActive, onCheckedChange = { makeActive = it })
                    Text("Use as active provider")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val headers = try {
                    if (headersText.isBlank()) emptyMap() else {
                        val obj = org.json.JSONObject(headersText)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    }
                } catch (e: Exception) { emptyMap() }
                onSave(name, baseUrl, model, apiKey, orgId, headers, makeActive)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
