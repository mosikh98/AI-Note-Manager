package com.ainote.manager.ai

data class AiOrganizeResult(
    val title: String,
    val organizedContent: String, // Markdown-like content: #, ##, -, 1., - [ ]
)

sealed class AiResult {
    data class Success(val result: AiOrganizeResult) : AiResult()
    data class Error(val message: String) : AiResult()
}

class AiRequestConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val organizationId: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
)
