package com.ainotes.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ainotes.app.domain.model.ExportFormat
import com.ainotes.app.domain.model.Note
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/** Turns a note into shareable bytes for every supported format. */
class ExportManager(private val context: Context) {

    data class Exported(val file: File, val mime: String)

    fun export(note: Note, format: ExportFormat): Exported {
        val safe = note.title.ifBlank { "note" }.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$safe.${format.extension}")
        file.writeBytes(render(note, format))
        return Exported(file, format.mime)
    }

    fun share(exported: Exported) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", exported.file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = exported.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export note"))
    }

    private fun render(note: Note, format: ExportFormat): ByteArray = when (format) {
        ExportFormat.TXT -> (titleBlock(note) + "\n\n" + note.content).toByteArray()
        ExportFormat.MARKDOWN -> ("# " + note.title + "\n\n" + note.content).toByteArray()
        ExportFormat.HTML -> html(note).toByteArray()
        ExportFormat.JSON -> json(note)
        ExportFormat.PDF -> pdf(note)
        ExportFormat.DOCX -> docx(note)
    }

    private fun titleBlock(note: Note) = note.title + "\n" + "=".repeat(note.title.length)

    private fun html(note: Note) = """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8">
        <style>
          body { font-family: sans-serif; margin: 40px; color: #1B1B2F; line-height: 1.6; }
          h1 { border-bottom: 3px solid #7C4DFF; padding-bottom: 8px; }
          pre { white-space: pre-wrap; }
        </style></head>
        <body><h1>${escapeHtml(note.title)}</h1><pre>${escapeHtml(note.content)}</pre></body></html>
    """.trimIndent()

    private fun json(note: Note): ByteArray {
        val o = JSONObject()
            .put("id", note.id)
            .put("title", note.title)
            .put("content", note.content)
            .put("createdAt", note.createdAt)
            .put("updatedAt", note.updatedAt)
            .put("favorite", note.isFavorite)
            .put("tags", org.json.JSONArray(note.tags))
        return o.toString(2).toByteArray()
    }

    private fun pdf(note: Note): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val bodyWidth = pageWidth - 2 * margin

        val titlePaint = TextPaint().apply {
            color = Color.rgb(27, 27, 47)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val content = note.content
        val bodyLayout = StaticLayout.Builder
            .obtain(content, 0, content.length, bodyPaint, bodyWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()

        var firstLine = 0
        var pageNumber = 0
        val lineCount = bodyLayout.lineCount

        while (firstLine < lineCount || pageNumber == 0) {
            pageNumber++
            val page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            val canvas = page.canvas

            var y = margin
            if (firstLine == 0) {
                canvas.drawText(note.title, margin, margin + 20f, titlePaint)
                y = margin + 50f
            }

            val usable = pageHeight - margin - y
            val offsetTop = bodyLayout.getLineTop(firstLine)

            canvas.save()
            canvas.clipRect(margin, 0f, margin + bodyWidth, pageHeight - margin.toFloat())
            canvas.translate(margin, y - offsetTop)
            bodyLayout.draw(canvas)
            canvas.restore()

            var last = firstLine
            while (last < lineCount &&
                bodyLayout.getLineBottom(last) - offsetTop <= usable
            ) {
                last++
            }
            if (last == firstLine) last = firstLine + 1
            firstLine = last

            doc.finishPage(page)
            if (firstLine >= lineCount) break
        }

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun docx(note: Note): ByteArray {
        val paragraphs = buildString {
            append(wrapP(note.title, bold = true))
            note.content.split("\n").forEach { line ->
                append(
                    when {
                        line.startsWith("# ") -> wrapP(line.removePrefix("# "), bold = true, size = 32)
                        line.startsWith("## ") -> wrapP(line.removePrefix("## "), bold = true, size = 26)
                        line.startsWith("- ") || line.startsWith("* ") ->
                            wrapP(line.replace(Regex("^[-*] "), "• "), indent = true)
                        line.isBlank() -> wrapP("")
                        else -> wrapP(line)
                    }
                )
            }
        }

        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$paragraphs</w:body></w:document>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write(contentTypes.toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels")); zip.write(rels.toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml")); zip.write(document.toByteArray()); zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun wrapP(text: String, bold: Boolean = false, size: Int = 22, indent: Boolean = false): String {
        val runProps = buildString {
            append("<w:rPr>")
            if (bold) append("<w:b/>")
            append("<w:sz w:val=\"$size\"/>")
            append("</w:rPr>")
        }
        val ind = if (indent) "<w:ind w:left=\"720\"/>" else ""
        return "<w:p><w:pPr>$ind</w:pPr><w:r>$runProps<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p>"
    }

    private fun escapeHtml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")
}
