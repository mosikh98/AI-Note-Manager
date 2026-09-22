package com.ainotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ainotes.app.data.local.entity.AiProviderEntity
import com.ainotes.app.data.local.entity.AttachmentEntity
import com.ainotes.app.data.local.entity.FolderEntity
import com.ainotes.app.data.local.entity.NoteEntity
import com.ainotes.app.data.local.entity.NoteTagCrossRef
import com.ainotes.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND folderId = :folderId ORDER BY updatedAt DESC")
    fun observeInFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query(
        """SELECT * FROM notes WHERE isDeleted = 0 AND (
              title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%'
              OR id IN (SELECT noteId FROM note_tags WHERE tagId LIKE '%' || :q || '%')
           ) ORDER BY updatedAt DESC"""
    )
    fun search(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun all(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, syncStatus = 'PENDING_UPLOAD' WHERE id = :id")
    suspend fun trash(id: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON nt.tagId = t.id WHERE nt.noteId = :noteId")
    suspend fun forNote(noteId: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(ref: NoteTagCrossRef)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearLinks(noteId: String)
}

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers")
    suspend fun all(): List<AiProviderEntity>

    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    suspend fun active(): AiProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: AiProviderEntity)

    @Query("UPDATE ai_providers SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE ai_providers SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun delete(id: String)
}
