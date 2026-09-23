package com.ainote.manager.cloud

import com.ainote.manager.data.AttachmentEntity
import com.ainote.manager.data.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

sealed class UploadResult {
    object Success : UploadResult()
    data class Error(val message: String) : UploadResult()
}

/**
 * Uploads a note (and its attachments) to a user-configured remote server. The server
 * contract is intentionally simple: a single POST endpoint under the configured base
 * URL that accepts multipart form data — this works with any custom backend/object
 * store the user points the app at, matching the spec's "custom server URL + API key"
 * minimum requirement. Content is only ever sent when the user explicitly triggers a
 * backup — never silently in the background without their action.
 */
class CloudUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadNote(
        serverUrl: String,
        apiKey: String,
        note: NoteEntity,
        attachments: List<AttachmentEntity>
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val endpoint = serverUrl.trimEnd('/') + "/notes"

            val metadata = buildJsonObject {
                put("title", note.title)
                put("content", note.content)
                put("updatedAt", note.updatedAt)
            }.toString()

            val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadata.toRequestBody("application/json".toMediaType()))

            attachments.forEach { attachment ->
                val file = File(attachment.localPath)
                if (file.exists()) {
                    bodyBuilder.addFormDataPart(
                        "attachment",
                        attachment.fileName,
                        file.asRequestBody(attachment.mimeType.toMediaType())
                    )
                }
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(bodyBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) UploadResult.Success
                else UploadResult.Error("HTTP ${response.code}: ${response.body?.string()?.take(200)}")
            }
        } catch (e: Exception) {
            UploadResult.Error(e.message ?: "Unknown upload error")
        }
    }
}
