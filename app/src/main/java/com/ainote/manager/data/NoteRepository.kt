package com.ainote.manager.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class NoteRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val noteDao = db.noteDao()
    private val attachmentDao = db.attachmentDao()
    private val providerDao = db.providerConfigDao()
    private val cloudDao = db.cloudConfigDao()
    val securePrefs = SecurePrefs(context)

    // Notes
    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()
    fun observeNote(id: Long): Flow<NoteEntity?> = noteDao.observeById(id)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.search(query)
    suspend fun getNote(id: Long): NoteEntity? = noteDao.getById(id)

    suspend fun createNote(title: String, content: String): Long =
        noteDao.insert(NoteEntity(title = title, content = content))

    suspend fun updateNote(note: NoteEntity) =
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(note: NoteEntity) {
        attachmentDao.getForNote(note.id).forEach { attachmentDao.delete(it) }
        noteDao.delete(note)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = noteDao.setFavorite(id, favorite)

    // Attachments
    fun observeAttachments(noteId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.observeForNote(noteId)

    suspend fun addAttachment(attachment: AttachmentEntity): Long =
        attachmentDao.insert(attachment)

    suspend fun removeAttachment(attachment: AttachmentEntity) =
        attachmentDao.delete(attachment)

    // Provider configs
    fun observeProviderConfigs(): Flow<List<ProviderConfigEntity>> = providerDao.observeAll()
    fun observeActiveProviderConfig(): Flow<ProviderConfigEntity?> = providerDao.observeActive()
    suspend fun getActiveProviderConfig(): ProviderConfigEntity? = providerDao.getActive()

    suspend fun saveProviderConfig(config: ProviderConfigEntity, apiKey: String?): Long {
        val id = if (config.id == 0L) providerDao.insert(config) else {
            providerDao.update(config); config.id
        }
        if (!apiKey.isNullOrBlank()) securePrefs.putProviderApiKey(id, apiKey)
        return id
    }

    suspend fun deleteProviderConfig(config: ProviderConfigEntity) {
        securePrefs.removeProviderApiKey(config.id)
        providerDao.delete(config)
    }

    suspend fun activateProviderConfig(id: Long) = providerDao.activate(id)

    fun getProviderApiKey(id: Long): String? = securePrefs.getProviderApiKey(id)

    // Cloud configs
    fun observeCloudConfigs(): Flow<List<CloudConfigEntity>> = cloudDao.observeAll()
    suspend fun getActiveCloudConfig(): CloudConfigEntity? = cloudDao.getActive()

    suspend fun saveCloudConfig(config: CloudConfigEntity, apiKey: String?): Long {
        val id = if (config.id == 0L) cloudDao.insert(config) else {
            cloudDao.update(config); config.id
        }
        if (!apiKey.isNullOrBlank()) securePrefs.putCloudApiKey(id, apiKey)
        return id
    }

    suspend fun deleteCloudConfig(config: CloudConfigEntity) {
        securePrefs.removeCloudApiKey(config.id)
        cloudDao.delete(config)
    }

    suspend fun activateCloudConfig(id: Long) = cloudDao.activate(id)

    fun getCloudApiKey(id: Long): String? = securePrefs.getCloudApiKey(id)

    companion object {
        @Volatile private var INSTANCE: NoteRepository? = null
        fun getInstance(context: Context): NoteRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
