package com.ainotes.app.ui.editor

import android.net.Uri
import android.provider.OpenableColumns
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotes.app.AppContainer
import com.ainotes.app.ai.AiProgress
import com.ainotes.app.domain.model.AiAction
import com.ainotes.app.domain.model.Attachment
import com.ainotes.app.domain.model.ExportFormat
import com.ainotes.app.domain.model.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiUiState(
    val running: Boolean = false,
    val stage: String = "",
    val action: AiAction? = null,
    val original: String? = null,
    val result: String? = null,
    val error: String? = null
)

data class EditorUiState(
    val loaded: Boolean = false,
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val ai: AiUiState = AiUiState(),
    val exported: ExportManagerExported? = null
)

data class ExportManagerExported(val path: String, val mime: String, val name: String)

class EditorViewModel(
    private val container: AppContainer,
    private val noteId: String
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state

    private var aiJob: Job? = null
    private var originalContent: String? = null

    init {
        viewModelScope.launch {
            val note = container.repository.note(noteId)
            val tags = container.repository.noteTags(noteId)
            _state.update {
                it.copy(
                    loaded = true,
                    id = noteId,
                    title = note?.title ?: "",
                    content = note?.content ?: "",
                    tags = tags,
                    isFavorite = note?.isFavorite ?: false
                )
            }
            originalContent = note?.content
        }
        viewModelScope.launch {
            container.repository.observeAttachments(noteId).collect { list ->
                _state.update { it.copy(attachments = list) }
            }
        }
    }

    fun onTitle(value: String) = _state.update { it.copy(title = value) }
    fun onContent(value: String) = _state.update { it.copy(content = value) }

    fun toggleLine(index: Int) {
        val lines = _state.value.content.lines()
        if (index !in lines.indices) return
        val line = lines[index]
        val updated = when {
            line.contains("[ ]") -> line.replace("[ ]", "[x]")
            line.contains("[x]") || line.contains("[X]") -> line.replace("[x]", "[ ]").replace("[X]", "[ ]")
            line.trimStart().startsWith("\u2610") -> line.replace("\u2610", "\u2611")
            line.trimStart().startsWith("\u2611") -> line.replace("\u2611", "\u2610")
            else -> line
        }
        val newContent = lines.toMutableList().also { it[index] = updated }.joinToString("\n")
        _state.update { it.copy(content = newContent) }
        persist()
    }

    fun insertToken(token: String) = _state.update {
        it.copy(content = if (it.content.isEmpty()) token else it.content + "\n" + token)
    }

    fun setTags(raw: String) = _state.update {
        it.copy(
            tags = raw.split(Regex("[,\\s]+"))
                .map { t -> t.trim().trimStart('#').lowercase() }
                .filter { t -> t.isNotBlank() }
        )
    }

    fun toggleFavorite() {
        _state.update { it.copy(isFavorite = !it.isFavorite) }
        persist()
    }

    fun persist() {
        val s = _state.value
        viewModelScope.launch {
            runCatching {
                container.repository.saveNote(
                    Note(
                        id = s.id,
                        title = s.title,
                        content = s.content,
                        tags = s.tags,
                        createdAt = 0,
                        updatedAt = System.currentTimeMillis(),
                        isFavorite = s.isFavorite
                    )
                )
            }.onFailure { e ->
                _state.update {
                    it.copy(ai = it.ai.copy(error = "err:saveFailed:" + (e.message ?: e.javaClass.simpleName)))
                }
            }
        }
    }

    // ---- AI ----

    fun runAi(action: AiAction, extra: String? = null) {
        val content = _state.value.content
        if (content.isBlank()) {
            _state.update { it.copy(ai = AiUiState(error = "err:emptyNote")) }
            return
        }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(ai = AiUiState(running = true, action = action, original = content)) }
            val provider = container.repository.activeProvider()
            if (provider == null) {
                _state.update {
                    it.copy(ai = AiUiState(action = action, error = "err:noProvider"))
                }
                return@launch
            }
            try {
                container.aiService.run(provider, action, content, extra).collect { progress ->
                    when (progress) {
                        is AiProgress.Stage ->
                            _state.update { it.copy(ai = it.ai.copy(stage = progress.text)) }
                        is AiProgress.Result -> {
                            val title = _state.value.title
                            val generated = if (action == AiAction.GENERATE_TITLE) title else null
                            _state.update {
                                it.copy(
                                    ai = it.ai.copy(
                                        running = false,
                                        result = progress.text,
                                        stage = ""
                                    )
                                )
                            }
                            if (generated != null) {
                                // title action returns a plain line; handled by the caller UI
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(ai = AiUiState(action = action, error = e.message ?: "err:aiFailed"))
                }
            }
        }
    }

    fun applyAiResult() {
        val result = _state.value.ai.result ?: return
        if (_state.value.ai.action == AiAction.GENERATE_TITLE) {
            _state.update {
                it.copy(title = result.lineSequence().first().trim('"', '\u201C', '\u201D'), ai = AiUiState())
            }
        } else {
            _state.update { it.copy(content = result, ai = AiUiState()) }
            persist()
        }
    }

    fun cancelAi() {
        aiJob?.cancel()
        _state.update { it.copy(ai = AiUiState()) }
    }

    fun dismissAi() = cancelAi()

    // ---- attachments ----

    fun addAttachment(context: Context, uri: Uri) = viewModelScope.launch {
        var name = "file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val mime = context.contentResolver.getType(uri)
        container.repository.addAttachment(noteId, name, mime, size, uri.toString())
    }

    fun removeAttachment(id: String) = viewModelScope.launch {
        container.repository.removeAttachment(id)
    }

    // ---- export ----

    fun export(format: ExportFormat) {
        val s = _state.value
        persist()
        runCatching {
            val note = Note(
                id = s.id, title = s.title.ifBlank { "Untitled" }, content = s.content,
                tags = s.tags, createdAt = 0, updatedAt = System.currentTimeMillis(),
                isFavorite = s.isFavorite
            )
            val out = container.exporter.export(note, format)
            _state.update {
                it.copy(exported = ExportManagerExported(out.file.absolutePath, out.mime, out.file.name))
            }
        }.onFailure { e ->
            _state.update { it.copy(ai = it.ai.copy(error = e.message)) }
        }
    }

    fun shareExported() {
        val e = _state.value.exported ?: return
        container.exporter.share(
            com.ainotes.app.export.ExportManager.Exported(
                java.io.File(e.path), e.mime
            )
        )
    }

    fun clearExported() = _state.update { it.copy(exported = null) }
}
