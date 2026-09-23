package com.ainote.manager.ui.cloud

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainote.manager.data.CloudConfigEntity
import com.ainote.manager.data.NoteRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CloudSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)

    val configs: StateFlow<List<CloudConfigEntity>> = repo.observeCloudConfigs()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveConfig(existing: CloudConfigEntity?, name: String, serverUrl: String, apiKey: String, makeActive: Boolean) =
        viewModelScope.launch {
            val config = CloudConfigEntity(
                id = existing?.id ?: 0L,
                name = name,
                serverUrl = serverUrl,
                isActive = existing?.isActive ?: false,
            )
            val id = repo.saveCloudConfig(config, apiKey.ifBlank { null })
            if (makeActive) repo.activateCloudConfig(id)
        }

    fun activate(id: Long) = viewModelScope.launch { repo.activateCloudConfig(id) }
    fun delete(config: CloudConfigEntity) = viewModelScope.launch { repo.deleteCloudConfig(config) }
    fun getSavedApiKey(id: Long): String? = repo.getCloudApiKey(id)
}
