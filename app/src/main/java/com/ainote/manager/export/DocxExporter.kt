package com.ainote.manager.export

import com.ainote.manager.data.NoteEntity
import com.ainote.manager.util.MarkdownParser
import com.ainote.manager.util.MdBlock
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal but valid OOXML .docx file (a zip containing the standard Word parts)
 * without pulling in a heavyweight external docx library. Supports headings, paragraphs,
 * bold/italic runs, bullet lists, numbered lists and checklist items (rendered as
 * "☐ / ☑ text" runs, since real interactive checkboxes require a much larger OOXML
 * content-control payload than is worth it for an export feature).
 */
object DocxExporter {

    fun export(note: NoteEntity, outFile: File) {
        ZipOutputStream(outFile.outputStream()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeEntry(zip, "_rels/.rels", relsXml())
            writeEntry(zip, "word/_rels/document.xml.rels", documentRelsXml())
            writeEntry(zip, "word/document.xml", documentXml(note))
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        </Types>
    """.trimIndent()

    private fun relsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    private fun documentRelsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
    """.trimIndent()

    private fun documentXml(note: NoteEntity): String {
        val body = StringBuilder()
        body.append(paragraph(runs(note.title, bold = true, sizeHalfPoints = 44)))

        MarkdownParser.parse(note.content).forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val size = when (block.level) { 1 -> 32; 2 -> 28; else -> 24 }
                    body.append(paragraph(runs(block.text, bold = true, sizeHalfPoints = size)))
                }
                is MdBlock.Paragraph -> body.append(paragraph(runsFromInline(block.text)))
                is MdBlock.BulletItem -> body.append(paragraph(runsFromInline("•  " + block.text)))
                is MdBlock.NumberedItem -> body.append(paragraph(runsFromInline("${block.number}.  " + block.text)))
                is MdBlock.ChecklistItem -> {
                    val box = if (block.checked) "☑" else "☐"
                    body.append(paragraph(runsFromInline("$box  " + block.text)))
                }
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                $body
                <w:sectPr/>
              </w:body>
            </w:document>
        """.trimIndent()
    }

    private fun runsFromInline(text: String): String =
        MarkdownParser.parseInline(text).joinToString("") { run ->
            runs(run.text, bold = run.bold, italic = run.italic)
        }

    private fun runs(text: String, bold: Boolean = false, italic: Boolean = false, sizeHalfPoints: Int? = null): String {
        val props = mutableListOf<String>()
        if (bold) props += "<w:b/>"
        if (italic) props += "<w:i/>"
        if (sizeHalfPoints != null) props += "<w:sz w:val=\"$sizeHalfPoints\"/>"
        val rpr = if (props.isNotEmpty()) "<w:rPr>${props.joinToString("")}</w:rPr>" else ""
        return "<w:r>$rpr<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r>"
    }

    private fun paragraph(runsXml: String): String = "<w:p>$runsXml</w:p>"

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
