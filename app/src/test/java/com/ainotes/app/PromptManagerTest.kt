package com.ainotes.app

import com.ainotes.app.ai.PromptManager
import com.ainotes.app.domain.model.AiAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptManagerTest {

    @Test
    fun organizePromptContainsContent() {
        val prompt = PromptManager.userPrompt(AiAction.ORGANIZE, "today server\nneed install docker")
        assertTrue(prompt.contains("need install docker"))
        assertTrue(prompt.contains("Organize"))
    }

    @Test
    fun systemPromptForbidsInventingInformation() {
        assertTrue(PromptManager.DEFAULT_SYSTEM_PROMPT.contains("Do not invent information"))
        assertTrue(PromptManager.DEFAULT_SYSTEM_PROMPT.contains("Preserve the original meaning"))
    }

    @Test
    fun askPromptIncludesQuestion() {
        val prompt = PromptManager.userPrompt(AiAction.ASK, "note body", "what is left?")
        assertTrue(prompt.contains("what is left?"))
        assertFalse(prompt.contains("null"))
    }
}
