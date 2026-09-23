package com.ainote.manager.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainote.manager.data.NoteRepository
import com.ainote.manager.data.ProviderConfigEntity
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)

    val configs: StateFlow<List<ProviderConfigEntity>> = repo.observeProviderConfigs()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

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
