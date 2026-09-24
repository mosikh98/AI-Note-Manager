package com.ainote.manager.cloud

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Minimal Google Drive integration using on-device Google Sign-In (no server-side
 * component, no client secret in the app) plus the plain Drive v3 REST API — avoids
 * pulling in the full Google API Java client just to upload a file.
 *
 * Requires the app's package name (com.ainote.manager) and signing certificate SHA-1 to be
 * registered as an OAuth Android client in Google Cloud Console (APIs & Services →
 * Credentials) with the Drive API enabled — see README "Connecting Google Drive".
 * Uses the drive.file scope, so the app can only see/manage files it creates itself.
 */
object GoogleDriveService {

    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
    private const val BACKUP_FOLDER_NAME = "AI Note Manager Backups"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun signInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(context: Context): Intent = signInClient(context).signInIntent

    /** Returns the signed-in account's email, or null if sign-in didn't succeed. */
    fun extractAccountEmail(account: GoogleSignInAccount?): String? = account?.email

    fun lastSignedInEmail(context: Context): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email

    private suspend fun getAccessToken(context: Context, accountEmail: String): String =
        withContext(Dispatchers.IO) {
            val account = Account(accountEmail, "com.google")
            GoogleAuthUtil.getToken(context, account, SCOPE)
        }

    private suspend fun findOrCreateBackupFolder(token: String): String = withContext(Dispatchers.IO) {
        val query = "name='$BACKUP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"
        val searchRequest = Request.Builder().url(searchUrl).addHeader("Authorization", "Bearer $token").get().build()
        client.newCall(searchRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val files = JSONObject(body).optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return@withContext files.getJSONObject(0).getString("id")
                }
            }
        }
        // Not found — create it.
        val metadata = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }
        val createRequest = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .addHeader("Authorization", "Bearer $token")
            .post(metadata.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(createRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Could not create Drive backup folder: HTTP ${response.code}")
            JSONObject(body).getString("id")
        }
    }

    suspend fun uploadFile(
        context: Context,
        accountEmail: String,
        fileName: String,
        mimeType: String,
        file: File,
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context, accountEmail)
            val folderId = findOrCreateBackupFolder(token)

            val metadata = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().put(folderId))
            }

            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addFormDataPart("file", fileName, file.asRequestBody(mimeType.toMediaType()))
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) UploadResult.Success
                else UploadResult.Error("Google Drive upload failed: HTTP ${response.code}: ${response.body?.string()?.take(200)}")
            }
        } catch (e: Exception) {
            UploadResult.Error(e.message ?: "Google Drive upload failed")
        }
    }
}
