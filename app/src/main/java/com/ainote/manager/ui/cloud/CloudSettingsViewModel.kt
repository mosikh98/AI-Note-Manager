package com.ainote.manager.ui.cloud

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainote.manager.cloud.DropboxService
import com.ainote.manager.cloud.UploadResult
import com.ainote.manager.data.CloudConfigEntity
import com.ainote.manager.data.CloudProviderType
import com.ainote.manager.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    object Success : ConnectionTestState()
    data class Failure(val message: String) : ConnectionTestState()
}

class CloudSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)

    val configs: StateFlow<List<CloudConfigEntity>> = repo.observeCloudConfigs()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    fun saveConfig(
        existing: CloudConfigEntity?,
        name: String,
        provider: CloudProviderType,
        serverUrl: String,
        accountLabel: String?,
        secret: String,
        makeActive: Boolean,
    ) = viewModelScope.launch {
        val config = CloudConfigEntity(
            id = existing?.id ?: 0L,
            name = name,
            providerType = provider.name,
            serverUrl = serverUrl,
            accountLabel = accountLabel,
            isActive = existing?.isActive ?: false,
        )
        val id = repo.saveCloudConfig(config, secret.ifBlank { null })
        if (makeActive) repo.activateCloudConfig(id)
    }

    fun activate(id: Long) = viewModelScope.launch { repo.activateCloudConfig(id) }
    fun delete(config: CloudConfigEntity) = viewModelScope.launch { repo.deleteCloudConfig(config) }
    fun getSavedSecret(id: Long): String? = repo.getCloudApiKey(id)

    fun testDropboxToken(token: String) = viewModelScope.launch {
        _testState.value = ConnectionTestState.Testing
        _testState.value = when (val result = DropboxService.testToken(token)) {
            is UploadResult.Success -> ConnectionTestState.Success
            is UploadResult.Error -> ConnectionTestState.Failure(result.message)
        }
    }

    fun resetTestState() { _testState.value = ConnectionTestState.Idle }
}
