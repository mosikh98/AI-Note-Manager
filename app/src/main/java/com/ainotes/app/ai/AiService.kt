package com.ainotes.app.ai

import com.ainotes.app.domain.model.AiAction
import com.ainotes.app.domain.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface AiProgress {
    data class Stage(val text: String) : AiProgress
    data class Result(val text: String) : AiProgress
}

class AiService(
    private val clientFactory: (AiProviderConfig) -> AiClient = { OpenAiCompatibleClient(it) }
) {

    /** Runs an AI action, emitting friendly stages; cancelling the collector cancels the call. */
    fun run(
        provider: AiProviderConfig,
        action: AiAction,
        content: String,
        extra: String? = null
    ): Flow<AiProgress> = flow {
        emit(AiProgress.Stage("تحلیل محتوا"))
        val client = clientFactory(provider)
        emit(AiProgress.Stage("ساخت بخش‌ها"))
        val answer = client.chat(
            listOf(
                ChatMessage("system", PromptManager.DEFAULT_SYSTEM_PROMPT),
                ChatMessage("user", PromptManager.userPrompt(action, content, extra))
            )
        )
        emit(AiProgress.Stage("بهبود قالب‌بندی"))
        emit(AiProgress.Result(answer.trim()))
    }
}
