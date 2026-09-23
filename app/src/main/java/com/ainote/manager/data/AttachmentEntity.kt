package com.ainote.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    // Path to the copy of the file kept inside app-private storage.
    val localPath: String,
    val addedAt: Long = System.currentTimeMillis(),
)
