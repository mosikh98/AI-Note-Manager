package com.ainotes.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.ainotes.app.data.local.dao.AiProviderDao
import com.ainotes.app.data.local.dao.AttachmentDao
import com.ainotes.app.data.local.dao.FolderDao
import com.ainotes.app.data.local.dao.NoteDao
import com.ainotes.app.data.local.dao.TagDao
import com.ainotes.app.data.local.entity.AiProviderEntity
import com.ainotes.app.data.local.entity.AttachmentEntity
import com.ainotes.app.data.local.entity.FolderEntity
import com.ainotes.app.data.local.entity.NoteEntity
import com.ainotes.app.data.local.entity.NoteTagCrossRef
import com.ainotes.app.data.local.entity.SyncRecordEntity
import com.ainotes.app.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        AttachmentEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AiProviderEntity::class,
        SyncRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AiNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun aiProviderDao(): AiProviderDao

    companion object {
        @Volatile private var instance: AiNotesDatabase? = null

        fun get(context: Context): AiNotesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiNotesDatabase::class.java,
                    "ai_notes.db"
                ).build().also { instance = it }
            }
    }
}
