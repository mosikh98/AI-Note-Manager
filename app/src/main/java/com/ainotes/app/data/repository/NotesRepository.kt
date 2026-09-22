package com.ainotes.app.data.repository

import com.ainotes.app.data.local.AiNotesDatabase
import com.ainotes.app.data.local.entity.AiProviderEntity
import com.ainotes.app.data.local.entity.AttachmentEntity
import com.ainotes.app.data.local.entity.FolderEntity
import com.ainotes.app.data.local.entity.NoteEntity
import com.ainotes.app.data.local.entity.NoteTagCrossRef
import com.ainotes.app.data.local.entity.TagEntity
import com.ainotes.app.domain.model.AiProviderConfig
import com.ainotes.app.domain.model.Attachment
import com.ainotes.app.domain.model.Folder
import com.ainotes.app.domain.model.Note
import com.ainotes.app.domain.model.Tag
import com.ainotes.app.security.SecureStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository(
    private val db: AiNotesDatabase,
    private val secureStore: SecureStore
) {
    private val notes = db.noteDao()
    private val attachments = db.attachmentDao()
    private val folders = db.folderDao()
    private val tags = db.tagDao()
    private val providers = db.aiProviderDao()

    // ---- notes ----

    fun observeNotes(folderId: String? = null): Flow<List<Note>> =
        (if (folderId == null) notes.observeAll() else notes.observeInFolder(folderId))
            .map { list -> list.map { it.toDomain() } }

    fun search(query: String): Flow<List<Note>> =
        notes.search(query).map { list -> list.map { it.toDomain() } }

    fun observeFavorites(): Flow<List<Note>> =
        notes.observeFavorites().map { list -> list.map { it.toDomain() } }

    suspend fun allNotes(): List<Note> = notes.all().map { it.toDomain() }

    suspend fun note(id: String): Note? = notes.byId(id)?.toDomain()

    suspend fun createNote(
        title: String = "Untitled",
        content: String = "",
        folderId: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        notes.upsert(
            NoteEntity(
                id = id, title = title, content = content, folderId = folderId,
                createdAt = now, updatedAt = now, syncStatus = "PENDING_UPLOAD"
            )
        )
        return id
    }

    suspend fun saveNote(note: Note) {
        val existing = notes.byId(note.id)
        val entity = NoteEntity(
            id = note.id,
            title = note.title.ifBlank { "Untitled" },
            content = note.content,
            folderId = note.folderId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isFavorite = existing?.isFavorite ?: note.isFavorite,
            syncStatus = "PENDING_UPLOAD",
            version = (existing?.version ?: 0) + 1
        )
        notes.upsert(entity)
        tags.clearLinks(note.id)
        note.tags.forEach { name ->
            val tagId = name.lowercase().trimStart('#')
            if (tagId.isNotBlank()) {
                tags.upsert(TagEntity(id = tagId, name = tagId))
                tags.link(NoteTagCrossRef(noteId = note.id, tagId = tagId))
            }
        }
    }

    suspend fun toggleFavorite(id: String) {
        val n = notes.byId(id) ?: return
        notes.upsert(n.copy(isFavorite = !n.isFavorite))
    }

    suspend fun trash(id: String) = notes.trash(id)
    suspend fun deleteNote(id: String) = notes.delete(id)

    suspend fun restoreFromTrash(id: String) {
        notes.byId(id)?.let {
            notes.upsert(it.copy(isDeleted = false, syncStatus = "PENDING_UPLOAD"))
        }
    }

    suspend fun noteTags(id: String): List<String> = tags.forNote(id).map { it.name }

    suspend fun attachmentCount(noteId: String): Int = 0

    // ---- attachments ----

    fun observeAttachments(noteId: String): Flow<List<Attachment>> =
        attachments.observeForNote(noteId).map { list -> list.map { it.toDomain() } }

    suspend fun addAttachment(noteId: String, fileName: String, mime: String?, size: Long, uri: String) {
        attachments.insert(
            AttachmentEntity(
                id = UUID.randomUUID().toString(), noteId = noteId, fileName = fileName,
                mimeType = mime, fileSize = size, localUri = uri,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeAttachment(id: String) = attachments.delete(id)

    // ---- folders & tags ----

    fun observeFolders(): Flow<List<Folder>> =
        folders.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun createFolder(name: String, parentId: String? = null): String {
        val id = UUID.randomUUID().toString()
        folders.upsert(FolderEntity(id = id, name = name, parentId = parentId))
        return id
    }

    suspend fun deleteFolder(id: String) = folders.delete(id)

    fun observeTags(): Flow<List<Tag>> = tags.observeAll().map { list -> list.map { it.toDomain() } }

    // ---- AI providers ----

    fun observeProviders(): Flow<List<AiProviderConfig>> = providers.observeAll()
        .map { list -> list.map { it.toConfig() } }

    suspend fun activeProvider(): AiProviderConfig? =
        providers.active()?.let { realProvider(it.id) }

    suspend fun saveProvider(config: AiProviderConfig) {
        val id = config.id.ifBlank { UUID.randomUUID().toString() }
        val existing = providers.all().firstOrNull { it.id == id }
        val keyEnc = when {
            config.apiKey.isBlank() -> null
            config.apiKey == MASK -> existing?.apiKeyEnc
            else -> secureStore.encrypt(config.apiKey)
        }
        providers.upsert(
            AiProviderEntity(
                id = id,
                name = config.name,
                baseUrl = config.baseUrl,
                model = config.model,
                apiKeyEnc = keyEnc,
                organizationId = config.organizationId,
                customHeaders = config.customHeaders.entries
                    .joinToString("\n") { "${it.key}=${it.value}" }
                    .ifBlank { null },
                isActive = config.isActive
            )
        )
        if (config.isActive) providers.setActive(id)
    }

    suspend fun setActiveProvider(id: String) = providers.setActive(id)

    suspend fun deleteProvider(id: String) = providers.delete(id)

    private fun AiProviderEntity.toConfig() = AiProviderConfig(
        id = id, name = name, baseUrl = baseUrl, model = model,
        apiKey = MASK,
        organizationId = organizationId,
        customHeaders = customHeaders
            ?.lineSequence()
            ?.filter { it.contains('=') }
            ?.associate { val i = it.indexOf('='); it.substring(0, i) to it.substring(i + 1) }
            ?: emptyMap(),
        isActive = isActive
    )

    /** Resolves the real (decrypted) key; used only when performing network calls. */
    suspend fun realProvider(id: String): AiProviderConfig? =
        providers.all().firstOrNull { it.id == id }?.let { entity ->
            entity.toConfig().copy(
                apiKey = entity.apiKeyEnc?.let { runCatching { secureStore.decrypt(it) }.getOrNull() }
                    .orEmpty()
            )
        }

    private fun NoteEntity.toDomain() = Note(
        id = id, title = title, content = content, folderId = folderId,
        createdAt = createdAt, updatedAt = updatedAt, isFavorite = isFavorite,
        syncStatus = syncStatus
    )

    private fun AttachmentEntity.toDomain() =
        Attachment(id, noteId, fileName, mimeType, fileSize, localUri, createdAt)

    private fun FolderEntity.toDomain() = Folder(id, name, parentId)
    private fun TagEntity.toDomain() = Tag(id, name)

    companion object {
        private const val MASK = "••••••••"
    }
}
