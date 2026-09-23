package com.ainote.manager.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Talks to any OpenAI-compatible `/v1/chat/completions` endpoint: OpenAI itself,
 * OpenRouter, a self-hosted/local model server, or a custom gateway — whatever the
 * user configures in Settings. Nothing here is hard-coded to one provider.
 */
class AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun organize(config: AiRequestConfig, rawText: String): AiResult =
        withContext(Dispatchers.IO) {
            try {
                val endpoint = buildEndpoint(config.baseUrl)

                val systemPrompt = """
                    You are a note-organizing assistant. The user will give you messy,
                    unstructured text. Reorganize it into a clean note while PRESERVING
                    the original meaning exactly — do not invent facts, numbers, or claims
                    that are not in the source text.

                    Respond with ONLY a single JSON object, no markdown fences, no commentary:
                    {
                      "title": "<a short, descriptive title>",
                      "content": "<the organized note as Markdown text, using # / ## headings,
                                    - bullet lists, 1. numbered lists, and - [ ] / - [x] checklist
                                    items where appropriate>"
                    }
                """.trimIndent()

                val bodyJson = buildJsonObject {
                    put("model", config.model)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", rawText)
                        }
                    }
                    put("temperature", 0.3)
                }

                val requestBuilder = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")

                config.organizationId?.takeIf { it.isNotBlank() }?.let {
                    requestBuilder.addHeader("OpenAI-Organization", it)
                }
                config.customHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

                requestBuilder.post(bodyJson.toString().toRequestBody(jsonMediaType))

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext AiResult.Error("HTTP ${response.code}: ${bodyStr.take(300)}")
                    }
                    parseChatCompletion(bodyStr)
                }
            } catch (e: Exception) {
                AiResult.Error(e.message ?: "Unknown network error")
            }
        }

    private fun buildEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed
        else "$trimmed/chat/completions"
    }

    private fun parseChatCompletion(body: String): AiResult {
        return try {
            val root = Json.parseToJsonElement(body).jsonObject
            val choices = root["choices"]?.jsonArray
                ?: return AiResult.Error("No 'choices' in AI response")
            val messageContent = choices.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.content
                ?: return AiResult.Error("No message content in AI response")

            val cleaned = messageContent
                .trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            val resultObj = Json.parseToJsonElement(cleaned).jsonObject
            val title = resultObj["title"]?.jsonPrimitive?.content ?: "Untitled"
            val content = resultObj["content"]?.jsonPrimitive?.content ?: messageContent

            AiResult.Success(AiOrganizeResult(title = title, organizedContent = content))
        } catch (e: Exception) {
            AiResult.Error("Could not parse AI response: ${e.message}")
        }
    }
}
