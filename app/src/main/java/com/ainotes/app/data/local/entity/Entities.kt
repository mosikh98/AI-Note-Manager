package com.ainotes.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val version: Int = 0
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String?,
    val fileSize: Long,
    val localUri: String,
    val remoteUri: String? = null,
    val createdAt: Long
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String? = null
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class NoteTagCrossRef(val noteId: String, val tagId: String)

@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKeyEnc: String? = null,
    val organizationId: String? = null,
    val customHeaders: String? = null,
    val isActive: Boolean = false
)

@Entity(tableName = "sync_records")
data class SyncRecordEntity(
    @PrimaryKey val id: String,
    val entityId: String,
    val entityType: String,
    val status: String,
    val lastSync: Long,
    val remoteVersion: Int
)
