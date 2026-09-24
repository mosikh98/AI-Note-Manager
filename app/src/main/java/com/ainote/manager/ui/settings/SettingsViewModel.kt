package com.ainote.manager.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainote.manager.ai.AiClient
import com.ainote.manager.ai.AiRequestConfig
import com.ainote.manager.ai.AiResult
import com.ainote.manager.data.NoteRepository
import com.ainote.manager.data.ProviderConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    object Success : ConnectionTestState()
    data class Failure(val message: String) : ConnectionTestState()
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)
    private val aiClient = AiClient()

    val configs: StateFlow<List<ProviderConfigEntity>> = repo.observeProviderConfigs()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    fun testConnection(baseUrl: String, apiKey: String, model: String, orgId: String, headers: Map<String, String>) =
        viewModelScope.launch {
            if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
                _testState.value = ConnectionTestState.Failure("Fill in Base URL, API Key, and Model first")
                return@launch
            }
            _testState.value = ConnectionTestState.Testing
            val config = AiRequestConfig(baseUrl = baseUrl, apiKey = apiKey, model = model, organizationId = orgId, customHeaders = headers)
            _testState.value = when (val result = aiClient.testConnection(config)) {
                is AiResult.Success -> ConnectionTestState.Success
                is AiResult.Error -> ConnectionTestState.Failure(result.message)
            }
        }

    fun resetTestState() { _testState.value = ConnectionTestState.Idle }

    fun saveConfig(
        existing: ProviderConfigEntity?,
        name: String,
        baseUrl: String,
        model: String,
        apiKey: String,
        organizationId: String,
        customHeaders: Map<String, String>,
        makeActive: Boolean,
    ) = viewModelScope.launch {
        val config = ProviderConfigEntity(
            id = existing?.id ?: 0L,
            name = name,
            baseUrl = baseUrl,
            model = model,
            organizationId = organizationId.ifBlank { null },
            customHeadersJson = JSONObject(customHeaders as Map<*, *>).toString(),
            isActive = existing?.isActive ?: false,
        )
        val id = repo.saveProviderConfig(config, apiKey.ifBlank { null })
        if (makeActive) repo.activateProviderConfig(id)
    }

    fun activate(id: Long) = viewModelScope.launch { repo.activateProviderConfig(id) }

    fun delete(config: ProviderConfigEntity) = viewModelScope.launch { repo.deleteProviderConfig(config) }

    fun getSavedApiKey(id: Long): String? = repo.getProviderApiKey(id)
}
