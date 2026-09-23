package com.ainote.manager.util

/**
 * A small, deliberately-limited Markdown-like parser shared by both the in-app renderer
 * (MarkdownRenderer.kt) and the exporters. It understands exactly the subset the AI is
 * prompted to produce: headings, bold/italic, bullet lists, numbered lists and checklists.
 */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class BulletItem(val text: String) : MdBlock()
    data class NumberedItem(val number: Int, val text: String) : MdBlock()
    data class ChecklistItem(val text: String, val checked: Boolean) : MdBlock()
}

object MarkdownParser {

    fun parse(raw: String): List<MdBlock> {
        val lines = raw.lines()
        val blocks = mutableListOf<MdBlock>()
        var autoNumber = 1

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            if (line.isBlank()) { autoNumber = 1; continue }

            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> blocks += MdBlock.Heading(3, trimmed.removePrefix("### "))
                trimmed.startsWith("## ") -> blocks += MdBlock.Heading(2, trimmed.removePrefix("## "))
                trimmed.startsWith("# ") -> blocks += MdBlock.Heading(1, trimmed.removePrefix("# "))

                trimmed.startsWith("- [ ] ") -> blocks += MdBlock.ChecklistItem(trimmed.removePrefix("- [ ] "), checked = false)
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
                    blocks += MdBlock.ChecklistItem(trimmed.substring(6), checked = true)

                trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                    blocks += MdBlock.BulletItem(trimmed.substring(2))

                Regex("""^\d+\.\s+.*""").matches(trimmed) -> {
                    val text = trimmed.substringAfter(". ")
                    blocks += MdBlock.NumberedItem(autoNumber, text)
                    autoNumber++
                }

                else -> blocks += MdBlock.Paragraph(trimmed)
            }
        }
        return blocks
    }

    /** Strips inline bold/italic markup for exporters that need plain text (TXT). */
    fun stripInlineMarkup(text: String): String =
        text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")

    /** Splits text on **bold** and *italic* markers into (text, bold, italic) runs for rendering. */
    data class InlineRun(val text: String, val bold: Boolean, val italic: Boolean)

    fun parseInline(text: String): List<InlineRun> {
        val runs = mutableListOf<InlineRun>()
        val regex = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*")
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            if (match.range.first > lastIndex) {
                runs += InlineRun(text.substring(lastIndex, match.range.first), bold = false, italic = false)
            }
            val boldGroup = match.groups[1]
            if (boldGroup != null) {
                runs += InlineRun(boldGroup.value, bold = true, italic = false)
            } else {
                val italicGroup = match.groups[2]!!
                runs += InlineRun(italicGroup.value, bold = false, italic = true)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            runs += InlineRun(text.substring(lastIndex), bold = false, italic = false)
        }
        if (runs.isEmpty()) runs += InlineRun(text, bold = false, italic = false)
        return runs
    }
}
