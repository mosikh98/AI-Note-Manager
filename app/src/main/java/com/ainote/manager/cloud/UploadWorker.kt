package com.ainote.manager.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ainote.manager.data.AttachmentEntity
import com.ainote.manager.data.CloudProviderType
import com.ainote.manager.data.NoteRepository
import com.ainote.manager.export.JsonExporter
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Runs a single note's upload in the background via WorkManager, so a backup survives
 * process death / the user navigating away. Triggered only from an explicit user action
 * (see CloudSettingsScreen / note "Backup to cloud" action) — never scheduled silently.
 * Dispatches to the configured provider: a custom server, Google Drive, or Dropbox.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId == -1L) return Result.failure()

        val repo = NoteRepository.getInstance(applicationContext)
        val note = repo.getNote(noteId) ?: return Result.failure()
        val cloudConfig = repo.getActiveCloudConfig()
            ?: return Result.failure(workDataOf(KEY_ERROR to "No active cloud storage configured"))
        val attachments = repo.observeAttachments(noteId).first()

        val result: UploadResult = when (cloudConfig.provider) {
            CloudProviderType.CUSTOM_SERVER -> {
                val apiKey = repo.getCloudApiKey(cloudConfig.id)
                    ?: return Result.failure(workDataOf(KEY_ERROR to "No API key saved for this cloud config"))
                CloudUploader().uploadNote(cloudConfig.serverUrl, apiKey, note, attachments)
            }

            CloudProviderType.GOOGLE_DRIVE -> {
                val email = cloudConfig.accountLabel
                    ?: return Result.failure(workDataOf(KEY_ERROR to "No Google account connected for this destination"))
                uploadViaFileService(attachments) { fileName, mimeType, file ->
                    GoogleDriveService.uploadFile(applicationContext, email, fileName, mimeType, file)
                }
            }

            CloudProviderType.DROPBOX -> {
                val token = repo.getCloudApiKey(cloudConfig.id)
                    ?: return Result.failure(workDataOf(KEY_ERROR to "No Dropbox access token saved for this destination"))
                uploadViaFileService(attachments) { fileName, _, file ->
                    DropboxService.uploadFile(token, fileName, file)
                }
            }
        }

        return when (result) {
            is UploadResult.Success -> Result.success()
            is UploadResult.Error -> Result.failure(workDataOf(KEY_ERROR to result.message))
        }
    }

    private suspend fun uploadViaFileService(
        attachments: List<AttachmentEntity>,
        upload: suspend (fileName: String, mimeType: String, file: File) -> UploadResult,
    ): UploadResult {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        val repo = NoteRepository.getInstance(applicationContext)
        val note = repo.getNote(noteId) ?: return UploadResult.Error("Note no longer exists")

        val tempDir = File(applicationContext.cacheDir, "cloud_export").apply { mkdirs() }
        val safeTitle = note.title.ifBlank { "note" }.replace(Regex("[^A-Za-z0-9 _-]"), "").take(60).ifBlank { "note" }
        val metadataFile = File(tempDir, "$safeTitle.json")
        JsonExporter.export(note, metadataFile)

        val metadataResult = upload(metadataFile.name, "application/json", metadataFile)
        if (metadataResult is UploadResult.Error) return metadataResult

        for (attachment in attachments) {
            val file = File(attachment.localPath)
            if (!file.exists()) continue
            val attachmentResult = upload(attachment.fileName, attachment.mimeType, file)
            if (attachmentResult is UploadResult.Error) return attachmentResult
        }

        return UploadResult.Success
    }

    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_ERROR = "error"
    }
}
