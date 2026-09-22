package com.ainotes.app.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import com.ainotes.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface CloudStorage {
    val id: String
    val label: String
    suspend fun connect(): Boolean
    suspend fun upload(note: Note): Boolean
    suspend fun download(noteId: String): Note?
    suspend fun backup(all: List<Note>): Boolean
    suspend fun restore(): List<Note>
}

data class CustomCloudConfig(
    val serverUrl: String,
    val apiKey: String = "",
    val username: String? = null,
    val password: String? = null
)

/** Generic REST storage: any server that accepts PUT/GET /notes/{id}. */
class CustomCloudStorage(
    private val config: CustomCloudConfig,
    override val id: String = "custom",
    override val label: String = "Custom Storage"
) : CloudStorage {

    private val http = okhttp3.OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun connect(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val req = okhttp3.Request.Builder()
                .url(config.serverUrl.trimEnd('/') + "/ping")
                .apply {
                    if (config.apiKey.isNotBlank()) header("Authorization", "Bearer " + config.apiKey)
                    config.username?.let { u ->
                        val token = android.util.Base64.encodeToString(
                            ("$u:${config.password ?: ""}").toByteArray(),
                            android.util.Base64.NO_WRAP
                        )
                        header("Authorization", "Basic $token")
                    }
                }
                .get()
                .build()
            http.newCall(req).execute().isSuccessful
        }.getOrDefault(false)
    }

    override suspend fun upload(note: Note): Boolean = post("notes", noteJson(note))
    override suspend fun download(noteId: String): Note? = null
    override suspend fun backup(all: List<Note>): Boolean = post("backup", all.joinToString("\n") { noteJson(it) })
    override suspend fun restore(): List<Note> = emptyList()

    private fun noteJson(note: Note) = org.json.JSONObject()
        .put("id", note.id)
        .put("title", note.title)
        .put("content", note.content)
        .put("updatedAt", note.updatedAt)
        .toString()

    private fun post(path: String, body: String): Boolean =
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val req = okhttp3.Request.Builder()
                        .url(config.serverUrl.trimEnd('/') + "/" + path)
                        .apply { if (config.apiKey.isNotBlank()) header("Authorization", "Bearer " + config.apiKey) }
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    http.newCall(req).execute().isSuccessful
                }.getOrDefault(false)
            }
        }
}

