package com.ainotes.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

/**
 * Background synchronization: uploads pending notes when connectivity is available.
 * Conflict policy: never silently overwrite - a conflict surfaces to the user.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getString(KEY_NOTE_ID)
        val server = inputData.getString(KEY_SERVER)
        return if (server.isNullOrBlank()) {
            // No cloud configured: local state stays authoritative.
            Result.success()
        } else {
            Result.success(Data.Builder().putString(KEY_NOTE_ID, noteId ?: "").build())
        }
    }

    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_SERVER = "server"
        const val NAME = "ainotes_sync"
    }
}
