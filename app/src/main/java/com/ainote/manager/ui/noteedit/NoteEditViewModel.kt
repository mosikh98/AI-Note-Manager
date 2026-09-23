package com.ainote.manager.ui.noteedit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ainote.manager.ai.AiClient
import com.ainote.manager.ai.AiRequestConfig
import com.ainote.manager.ai.AiResult
import com.ainote.manager.cloud.UploadWorker
import com.ainote.manager.data.AttachmentEntity
import com.ainote.manager.data.NoteEntity
import com.ainote.manager.data.NoteRepository
import com.ainote.manager.export.ExportFormat
import com.ainote.manager.export.NoteExporter
import com.ainote.manager.util.FileUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AiOrganizeUiState {
    object Idle : AiOrganizeUiState()
    object Loading : AiOrganizeUiState()
    data class Preview(val title: String, val content: String) : AiOrganizeUiState()
    data class Error(val message: String) : AiOrganizeUiState()
    object NoProviderConfigured : AiOrganizeUiState()
}

sealed class CloudUploadUiState {
    object Idle : CloudUploadUiState()
    object InProgress : CloudUploadUiState()
    object Success : CloudUploadUiState()
    data class Error(val message: String) : CloudUploadUiState()
    object NoCloudConfigured : CloudUploadUiState()
}

class NoteEditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)
    private val aiClient = AiClient()
    private val workManager = WorkManager.getInstance(app)

    private var noteId: Long = 0L

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _attachments = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    val attachments: StateFlow<List<AttachmentEntity>> = _attachments.asStateFlow()

    private val _aiState = MutableStateFlow<AiOrganizeUiState>(AiOrganizeUiState.Idle)
    val aiState: StateFlow<AiOrganizeUiState> = _aiState.asStateFlow()

    private val _uploadState = MutableStateFlow<CloudUploadUiState>(CloudUploadUiState.Idle)
    val uploadState: StateFlow<CloudUploadUiState> = _uploadState.asStateFlow()

    fun load(id: Long) {
        noteId = id
        viewModelScope.launch {
            repo.observeNote(id).collect { note ->
                if (note != null) {
                    _title.value = note.title
                    _content.value = note.content
                    _isFavorite.value = note.isFavorite
                }
            }
        }
        viewModelScope.launch {
            repo.observeAttachments(id).collect { _attachments.value = it }
        }
    }

    fun onTitleChange(value: String) { _title.value = value; persist() }
    fun onContentChange(value: String) { _content.value = value; persist() }
    fun onChecklistToggle(lineIndex: Int, checked: Boolean) {
        val lines = _content.value.lines().toMutableList()
        var counter = -1
        for (i in lines.indices) {
            val t = lines[i].trimStart()
            val isChecklist = t.startsWith("- [ ] ") || t.startsWith("- [x] ") || t.startsWith("- [X] ")
            if (isChecklist) {
                counter++
                if (counter == lineIndex) {
                    val text = t.substring(6)
                    lines[i] = if (checked) "- [x] $text" else "- [ ] $text"
                    break
                }
            }
        }
        _content.value = lines.joinToString("\n")
        persist()
    }

    private var saveJob: kotlinx.coroutines.Job? = null
    private fun persist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            if (noteId != 0L) {
                repo.updateNote(
                    NoteEntity(
                        id = noteId,
                        title = _title.value,
                        content = _content.value,
                        isFavorite = _isFavorite.value,
                    )
                )
            }
        }
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        persist()
    }

    // ---- AI Organize ----
    fun runAiOrganize() = viewModelScope.launch {
        val active = repo.getActiveProviderConfig()
        if (active == null) { _aiState.value = AiOrganizeUiState.NoProviderConfigured; return@launch }
        val apiKey = repo.getProviderApiKey(active.id)
        if (apiKey.isNullOrBlank()) { _aiState.value = AiOrganizeUiState.NoProviderConfigured; return@launch }

        _aiState.value = AiOrganizeUiState.Loading
        val headers = try {
            val obj = JSONObject(active.customHeadersJson)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) { emptyMap() }

        val config = AiRequestConfig(
            baseUrl = active.baseUrl,
            apiKey = apiKey,
            model = active.model,
            organizationId = active.organizationId,
            customHeaders = headers,
        )

        when (val result = aiClient.organize(config, _content.value)) {
            is AiResult.Success -> _aiState.value = AiOrganizeUiState.Preview(result.result.title, result.result.organizedContent)
            is AiResult.Error -> _aiState.value = AiOrganizeUiState.Error(result.message)
        }
    }

    fun applyAiResult() {
        val state = _aiState.value
        if (state is AiOrganizeUiState.Preview) {
            if (_title.value.isBlank()) _title.value = state.title
            _content.value = state.content
            persist()
        }
        _aiState.value = AiOrganizeUiState.Idle
    }

    fun dismissAiPreview() { _aiState.value = AiOrganizeUiState.Idle }

    // ---- Attachments ----
    fun addAttachment(uri: Uri) = viewModelScope.launch {
        val context = getApplication<Application>()
        val info = FileUtils.copyToAppStorage(context, uri) ?: return@launch
        repo.addAttachment(
            AttachmentEntity(
                noteId = noteId,
                fileName = info.fileName,
                mimeType = info.mimeType,
                sizeBytes = info.sizeBytes,
                localPath = info.localPath,
            )
        )
    }

    fun removeAttachment(attachment: AttachmentEntity) = viewModelScope.launch {
        repo.removeAttachment(attachment)
    }

    // ---- Export ----
    fun exportAndShare(format: ExportFormat) {
        val context = getApplication<Application>()
        NoteExporter.exportAndShare(context, currentNoteSnapshot(), format)
    }

    private fun currentNoteSnapshot() = NoteEntity(
        id = noteId, title = _title.value, content = _content.value, isFavorite = _isFavorite.value
    )

    // ---- Cloud backup ----
    fun backupToCloud() = viewModelScope.launch {
        val active = repo.getActiveCloudConfig()
        if (active == null) { _uploadState.value = CloudUploadUiState.NoCloudConfigured; return@launch }

        _uploadState.value = CloudUploadUiState.InProgress
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_NOTE_ID to noteId))
            .build()
        workManager.enqueue(request)

        workManager.getWorkInfoByIdLiveData(request.id).asFlowCompat().collect { info ->
            when (info?.state) {
                WorkInfo.State.SUCCEEDED -> { _uploadState.value = CloudUploadUiState.Success; return@collect }
                WorkInfo.State.FAILED -> {
                    val err = info.outputData.getString(UploadWorker.KEY_ERROR) ?: "Upload failed"
                    _uploadState.value = CloudUploadUiState.Error(err)
                    return@collect
                }
                else -> {}
            }
        }
    }

    fun dismissUploadState() { _uploadState.value = CloudUploadUiState.Idle }
}

// Small LiveData->Flow bridge to avoid pulling in the lifecycle-livedata-ktx artifact just for this.
private fun <T> androidx.lifecycle.LiveData<T>.asFlowCompat(): Flow<T?> = kotlinx.coroutines.flow.callbackFlow {
    val observer = androidx.lifecycle.Observer<T> { trySend(it) }
    observeForever(observer)
    awaitClose { removeObserver(observer) }
}
