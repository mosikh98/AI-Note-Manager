package com.ainote.manager.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ainote.manager.data.NoteRepository
import kotlinx.coroutines.flow.first

/**
 * Runs a single note's upload in the background via WorkManager, so a backup survives
 * process death / the user navigating away. Triggered only from an explicit user action
 * (see CloudSettingsScreen / note "Backup to cloud" action) — never scheduled silently.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId == -1L) return Result.failure()

        val repo = NoteRepository.getInstance(applicationContext)
        val note = repo.getNote(noteId) ?: return Result.failure()
        val cloudConfig = repo.getActiveCloudConfig()
            ?: return Result.failure(workDataOf(KEY_ERROR to "No active cloud storage configured"))
        val apiKey = repo.getCloudApiKey(cloudConfig.id)
            ?: return Result.failure(workDataOf(KEY_ERROR to "No API key saved for this cloud config"))

        val snapshot = repo.observeAttachments(noteId).first()

        val result = CloudUploader().uploadNote(cloudConfig.serverUrl, apiKey, note, snapshot)
        return when (result) {
            is UploadResult.Success -> Result.success()
            is UploadResult.Error -> Result.failure(workDataOf(KEY_ERROR to result.message))
        }
    }

    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_ERROR = "error"
    }
}
