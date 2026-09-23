package com.ainote.manager.export

import com.ainote.manager.data.NoteEntity
import com.ainote.manager.util.MarkdownParser
import com.ainote.manager.util.MdBlock
import org.json.JSONObject
import java.io.File

object TxtExporter {
    fun export(note: NoteEntity, outFile: File) {
        val blocks = MarkdownParser.parse(note.content)
        val sb = StringBuilder()
        sb.appendLine(note.title)
        sb.appendLine("=".repeat(note.title.length.coerceAtLeast(1)))
        sb.appendLine()
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> sb.appendLine(MarkdownParser.stripInlineMarkup(block.text).uppercase())
                is MdBlock.Paragraph -> sb.appendLine(MarkdownParser.stripInlineMarkup(block.text))
                is MdBlock.BulletItem -> sb.appendLine("  • " + MarkdownParser.stripInlineMarkup(block.text))
                is MdBlock.NumberedItem -> sb.appendLine("  ${block.number}. " + MarkdownParser.stripInlineMarkup(block.text))
                is MdBlock.ChecklistItem -> sb.appendLine("  [${if (block.checked) "x" else " "}] " + MarkdownParser.stripInlineMarkup(block.text))
            }
        }
        outFile.writeText(sb.toString())
    }
}

object MarkdownExporter {
    fun export(note: NoteEntity, outFile: File) {
        val sb = StringBuilder()
        sb.appendLine("# ${note.title}")
        sb.appendLine()
        sb.append(note.content)
        outFile.writeText(sb.toString())
    }
}

object HtmlExporter {
    fun export(note: NoteEntity, outFile: File) {
        val blocks = MarkdownParser.parse(note.content)
        val body = StringBuilder()
        var inList: String? = null // "ul" | "ol"

        fun closeList() {
            if (inList != null) { body.append("</$inList>\n"); inList = null }
        }

        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> { closeList(); body.append("<h${block.level}>${inlineHtml(block.text)}</h${block.level}>\n") }
                is MdBlock.Paragraph -> { closeList(); body.append("<p>${inlineHtml(block.text)}</p>\n") }
                is MdBlock.BulletItem -> {
                    if (inList != "ul") { closeList(); body.append("<ul>\n"); inList = "ul" }
                    body.append("<li>${inlineHtml(block.text)}</li>\n")
                }
                is MdBlock.NumberedItem -> {
                    if (inList != "ol") { closeList(); body.append("<ol>\n"); inList = "ol" }
                    body.append("<li>${inlineHtml(block.text)}</li>\n")
                }
                is MdBlock.ChecklistItem -> {
                    if (inList != "ul") { closeList(); body.append("<ul style=\"list-style:none;padding-left:0\">\n"); inList = "ul" }
                    val checkedAttr = if (block.checked) "checked" else ""
                    body.append("<li><input type=\"checkbox\" disabled $checkedAttr> ${inlineHtml(block.text)}</li>\n")
                }
            }
        }
        closeList()

        val html = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>${escapeHtml(note.title)}</title>
            <style>
              body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:720px;margin:40px auto;padding:0 20px;color:#1b1b26;line-height:1.6}
              h1{font-size:1.8em} h2{font-size:1.4em} h3{font-size:1.15em}
            </style></head>
            <body>
            <h1>${escapeHtml(note.title)}</h1>
            $body
            </body></html>
        """.trimIndent()
        outFile.writeText(html)
    }

    private fun inlineHtml(text: String): String =
        MarkdownParser.parseInline(text).joinToString("") { run ->
            val escaped = escapeHtml(run.text)
            when {
                run.bold -> "<strong>$escaped</strong>"
                run.italic -> "<em>$escaped</em>"
                else -> escaped
            }
        }

    private fun escapeHtml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

object JsonExporter {
    fun export(note: NoteEntity, outFile: File) {
        val obj = JSONObject().apply {
            put("title", note.title)
            put("content", note.content)
            put("isFavorite", note.isFavorite)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
        }
        outFile.writeText(obj.toString(2))
    }
}
