package com.ainotes.app.ai

import com.ainotes.app.domain.model.AiAction

object PromptManager {

    val DEFAULT_SYSTEM_PROMPT = listOf(
        "You are a note organization assistant.",
        "Preserve the original meaning of the user text.",
        "Do not invent information that is not present.",
        "Create logical headings and subheadings.",
        "Use bullet lists, numbered lists and checklists where appropriate.",
        "Keep technical terms, commands and identifiers exactly as written.",
        "Return only the finished note content in Markdown."
    ).joinToString("\n")

    fun userPrompt(action: AiAction, content: String, extra: String? = null): String =
        when (action) {
            AiAction.ORGANIZE ->
                "Organize the following note into clear sections with headings, lists and tasks:\n\n" + content
            AiAction.SUMMARIZE ->
                "Write a concise summary of the following note:\n\n" + content
            AiAction.EXPAND ->
                "Expand the following short note into a structured, well written explanation:\n\n" + content
            AiAction.REWRITE ->
                "Rewrite the following note to improve readability while keeping the meaning:\n\n" + content
            AiAction.EXTRACT_TASKS ->
                "Extract every TODO / action item from the following note as a Markdown checklist:\n\n" + content
            AiAction.GENERATE_TITLE ->
                "Suggest one short, specific title for this note. Answer with the title only:\n\n" + content
            AiAction.TRANSLATE ->
                "Translate the following note to " + (extra ?: "English") + ". Keep formatting:\n\n" + content
            AiAction.ASK ->
                "Note:\n" + content + "\n\nQuestion: " + (extra ?: "")
        }
}
