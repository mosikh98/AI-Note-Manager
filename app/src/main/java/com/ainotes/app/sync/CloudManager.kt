package com.ainotes.app.sync

import com.ainotes.app.domain.model.Note
import com.ainotes.app.security.SecureStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object CloudKinds {
    const val CUSTOM = "custom"
    const val WEBDAV = "webdav"
    const val DROPBOX = "dropbox"
    const val GDRIVE = "gdrive"
    const val ONEDRIVE = "onedrive"
    const val FILE_NAME = "ainotes-backup.json"
    val ALL = listOf(CUSTOM, WEBDAV, DROPBOX, GDRIVE, ONEDRIVE)
}

data class CloudConfig(
    val kind: String,
    val url: String,
    val apiKey: String,
    val username: String,
    val password: String,
    val token: String
)

/**
 * Backup destinations: custom HTTP server, WebDAV (Nextcloud/Box/pCloud),
 * Dropbox, Google Drive and OneDrive (Microsoft Graph).
 * Everything here runs ONLY on explicit user taps from Settings
 * (test connection / backup now) - never automatically, never silently.
 * All credentials come from SecureStore (entered by the user at runtime).
 */
class CloudManager(private val store: SecureStore) {

    private val json = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun load(): CloudConfig = CloudConfig(
        kind = store.get("cloud_kind") ?: CloudKinds.CUSTOM,
        url = store.get("cloud_url") ?: "",
        apiKey = store.get("cloud_key") ?: "",
        username = store.get("cloud_user") ?: "",
        password = store.get("cloud_pass") ?: "",
        token = store.get("cloud_token") ?: ""
    )

    fun isConfigured(cfg: CloudConfig): Boolean = when (cfg.kind) {
        CloudKinds.CUSTOM -> cfg.url.isNotBlank()
        CloudKinds.WEBDAV -> cfg.url.isNotBlank() && cfg.username.isNotBlank()
        else -> cfg.token.isNotBlank()
    }

    suspend fun test(cfg: CloudConfig): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (cfg.kind) {
                    CloudKinds.CUSTOM -> {
                        val ok = CustomCloudStorage(CustomCloudConfig(cfg.url, cfg.apiKey)).connect()
                        if (!ok) throw IllegalStateException("ping failed - check URL and API key")
                        "ping ok"
                    }
                    CloudKinds.WEBDAV -> webdavTest(cfg)
                    CloudKinds.DROPBOX -> dropboxTest(cfg)
                    CloudKinds.GDRIVE -> gdriveTest(cfg)
                    else -> onedriveTest(cfg)
                }
            }
        }

    suspend fun backup(notes: List<Note>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = payload(notes)
                when (load().kind) {
                    CloudKinds.CUSTOM -> {
                        val cfg = load()
                        val ok = CustomCloudStorage(CustomCloudConfig(cfg.url, cfg.apiKey))
                            .backup(notes)
                        if (!ok) throw IllegalStateException("server rejected the backup")
                        "custom http ok"
                    }
                    CloudKinds.WEBDAV -> webdavUpload(load(), body)
                    CloudKinds.DROPBOX -> dropboxUpload(load(), body)
                    CloudKinds.GDRIVE -> gdriveUpload(load(), body)
                    else -> onedriveUpload(load(), body)
                }
            }
        }

    // ---- payload ----

    private fun payload(notes: List<Note>): String {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("folderId", n.folderId)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
                    .put("favorite", n.isFavorite)
                    .put("tags", JSONArray(n.tags))
            )
        }
        return JSONObject()
            .put("app", "AI-Note-Manager")
            .put("format", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("notes", arr)
            .toString(2)
    }

    private fun basic(user: String, pass: String): String =
        "Basic " + android.util.Base64.encodeToString(
            "$user:$pass".toByteArray(), android.util.Base64.NO_WRAP
        )

    private fun fail(what: String, resp: okhttp3.Response): Nothing {
        val snippet = runCatching { resp.body?.string().orEmpty() }.getOrDefault("")
        throw IllegalStateException("$what - HTTP ${resp.code} ${snippet.take(180)}")
    }

    // ---- WebDAV (Nextcloud, Box, pCloud, ...) ----

    private fun webdavDir(cfg: CloudConfig): String =
        cfg.url.trim().trimEnd('/') + "/"

    private fun webdavTest(cfg: CloudConfig): String {
        val req = Request.Builder()
            .url(webdavDir(cfg))
            .header("Depth", "0")
            .header("Authorization", basic(cfg.username, cfg.password))
            .method(
                "PROPFIND",
                """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>"""
                    .toRequestBody("application/xml".toMediaType())
            )
            .build()
        http.newCall(req).execute().use { resp ->
            return when (resp.code) {
                200, 207 -> "webdav ok"
                401, 403 -> { fail("authentication failed", resp) }
                404 -> { fail("folder not found", resp) }
                405 -> "webdav reachable (PROPFIND disabled)"
                else -> { fail("unexpected response", resp) }
            }
        }
    }

    private fun webdavUpload(cfg: CloudConfig, body: String): String {
        val dir = webdavDir(cfg)
        // Best-effort folder creation for nested target paths.
        val segments = cfg.url.trim().trimStart('/').split("/").filter { it.isNotBlank() }
        // recreate parent chain (skip host part already included in dir)
        // simple approach: try MKCOL for the final folder once
        runCatching {
            http.newCall(
                Request.Builder()
                    .url(dir)
                    .header("Authorization", basic(cfg.username, cfg.password))
                    .method("MKCOL", null)
                    .build()
            ).execute().close()
        }
        val req = Request.Builder()
            .url(dir + CloudKinds.FILE_NAME)
            .header("Authorization", basic(cfg.username, cfg.password))
            .put(body.toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            if (resp.code !in 200..204) fail("upload rejected", resp)
        }
        return "webdav ok (${segments.size} levels)"
    }

    // ---- Dropbox ----

    private fun dropboxTest(cfg: CloudConfig): String {
        val req = Request.Builder()
            .url("https://api.dropboxapi.com/2/users/get_current_account")
            .header("Authorization", "Bearer " + cfg.token)
            .post("null".toRequestBody("text/plain".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) fail("dropbox auth failed", resp)
            val name = runCatching {
                JSONObject(text).optJSONObject("name")?.opt("display_name")?.toString()
            }.getOrNull() ?: "?"
            return "dropbox ok ($name)"
        }
    }

    private fun dropboxUpload(cfg: CloudConfig, body: String): String {
        val arg = JSONObject()
            .put("path", "/" + CloudKinds.FILE_NAME)
            .put("mode", "overwrite")
            .put("autorename", false)
            .put("mute", true)
            .toString()
        val req = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/upload")
            .header("Authorization", "Bearer " + cfg.token)
            .header("Dropbox-API-Arg", arg)
            .post(body.toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) fail("dropbox upload failed", resp)
        }
        return "dropbox ok"
    }

    // ---- Google Drive ----

    private fun gdriveTest(cfg: CloudConfig): String {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/about?fields=user")
            .header("Authorization", "Bearer " + cfg.token)
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) fail("google auth failed", resp)
            val email = runCatching {
                JSONObject(text).getJSONObject("user").optString("emailAddress")
            }.getOrDefault("")
            return if (email.isBlank()) "google ok" else "google ok ($email)"
        }
    }

    private fun gdriveUpload(cfg: CloudConfig, body: String): String {
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
            .header("Authorization", "Bearer " + cfg.token)
            .post(body.toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) fail("drive upload failed", resp)
            val id = runCatching { JSONObject(text).getString("id") }.getOrDefault("?")
            return "google drive ok (file $id)"
        }
    }

    // ---- OneDrive (Microsoft Graph) ----

    private fun onedriveTest(cfg: CloudConfig): String {
        val req = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me")
            .header("Authorization", "Bearer " + cfg.token)
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) fail("microsoft auth failed", resp)
            val who = runCatching { JSONObject(text).optString("userPrincipalName") }.getOrDefault("")
            return if (who.isBlank()) "onedrive ok" else "onedrive ok ($who)"
        }
    }

    private fun onedriveUpload(cfg: CloudConfig, body: String): String {
        val req = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me/drive/root:/" + CloudKinds.FILE_NAME + ":/content")
            .header("Authorization", "Bearer " + cfg.token)
            .put(body.toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) fail("onedrive upload failed", resp)
        }
        return "onedrive ok"
    }
}
