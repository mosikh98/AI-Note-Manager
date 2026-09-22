package com.ainotes.app.ai

import com.ainotes.app.domain.model.AiProviderConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(val role: String, val content: String)

/** Provider-agnostic AI transport: any OpenAI-compatible /chat/completions endpoint. */
interface AiClient {
    suspend fun chat(messages: List<ChatMessage>): String
}

class OpenAiCompatibleClient(private val provider: AiProviderConfig) : AiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        if (provider.baseUrl.isBlank()) throw AiException("آدرس API خالیه")
        val url = provider.baseUrl.trimEnd('/') + "/chat/completions"

        val payload = JSONObject().apply {
            put("model", provider.model)
            put(
                "messages",
                JSONArray().apply {
                    messages.forEach { m ->
                        put(
                            JSONObject().apply {
                                put("role", m.role)
                                put("content", m.content)
                            }
                        )
                    }
                }
            )
            put("temperature", 0.2)
        }

        val builder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
        if (provider.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer " + provider.apiKey)
        }
        provider.organizationId?.takeIf { it.isNotBlank() }?.let {
            builder.header("OpenAI-Organization", it)
        }
        provider.customHeaders.forEach { (k, v) -> builder.header(k, v) }

        val response = http.newCall(builder.build()).execute()
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw AiException(
                when (response.code) {
                    401 -> "کلید API نامعتبره (401)"
                    404 -> "مدل یا آدرس endpoint پیدا نشد (404)"
                    429 -> "محدودیت نرخ (429) - کمی بعد دوباره امتحان کن"
                    in 500..599 -> "خطای سرور (${response.code})"
                    else -> "درخواست ناموفق بود (${response.code})"
                }
            )
        }
        try {
            JSONObject(text).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) {
            throw AiException("پاسخ غیرمنتظره از سرور AI")
        }
    }
}

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)
