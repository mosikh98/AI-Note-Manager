package com.ainote.manager.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Uploads files to Dropbox using a user-generated access token (Dropbox App Console →
 * your app → "Generated access token") — no OAuth flow needed in-app, just paste the
 * token, matching the same UX as the custom-server API key field. See README
 * "Connecting Dropbox" for how to generate one.
 */
object DropboxService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BACKUP_FOLDER = "/AI Note Manager Backups"

    suspend fun uploadFile(accessToken: String, fileName: String, file: File): UploadResult =
        withContext(Dispatchers.IO) {
            try {
                val apiArg = JSONObject().apply {
                    put("path", "$BACKUP_FOLDER/$fileName")
                    put("mode", "overwrite")
                    put("autorename", false)
                    put("mute", true)
                }.toString()
                    // Dropbox requires non-ASCII header values to be escaped this way.
                    .replace("\u007f", "\\u007f")

                val request = Request.Builder()
                    .url("https://content.dropboxapi.com/2/files/upload")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Dropbox-API-Arg", apiArg)
                    .addHeader("Content-Type", "application/octet-stream")
                    .post(file.asRequestBody("application/octet-stream".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) UploadResult.Success
                    else UploadResult.Error("Dropbox upload failed: HTTP ${response.code}: ${response.body?.string()?.take(200)}")
                }
            } catch (e: Exception) {
                UploadResult.Error(e.message ?: "Dropbox upload failed")
            }
        }

    /** Lightweight reachability check for the "Test connection" button. */
    suspend fun testToken(accessToken: String): UploadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.dropboxapi.com/2/users/get_current_account")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("null".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) UploadResult.Success
                else UploadResult.Error("HTTP ${response.code}: ${response.body?.string()?.take(200)}")
            }
        } catch (e: Exception) {
            UploadResult.Error(e.message ?: "Could not reach Dropbox")
        }
    }
}
