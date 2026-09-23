package com.ainote.manager.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.ainote.manager.data.NoteEntity
import com.ainote.manager.util.MarkdownParser
import com.ainote.manager.util.MdBlock
import java.io.File
import java.io.FileOutputStream

/** Uses Android's built-in PdfDocument API — no external PDF library dependency needed. */
object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 @ 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun export(note: NoteEntity, outFile: File) {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        val bulletPaint = Paint().apply { textSize = 12f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun newPageIfNeeded(lineHeight: Float) {
            if (y + lineHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawWrapped(text: String, paint: Paint, xOffset: Float, lineHeight: Float) {
            val maxWidth = PAGE_WIDTH - MARGIN * 2 - xOffset
            val words = text.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    newPageIfNeeded(lineHeight)
                    canvas.drawText(line.toString(), MARGIN + xOffset, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) {
                newPageIfNeeded(lineHeight)
                canvas.drawText(line.toString(), MARGIN + xOffset, y, paint)
                y += lineHeight
            }
        }

        drawWrapped(note.title, titlePaint, 0f, 28f)
        y += 8f

        MarkdownParser.parse(note.content).forEach { block ->
            when (block) {
                is MdBlock.Heading -> { y += 6f; drawWrapped(plain(block.text), headingPaint, 0f, 20f) }
                is MdBlock.Paragraph -> drawWrapped(plain(block.text), bodyPaint, 0f, 16f)
                is MdBlock.BulletItem -> drawWrapped("•  " + plain(block.text), bulletPaint, 10f, 16f)
                is MdBlock.NumberedItem -> drawWrapped("${block.number}.  " + plain(block.text), bulletPaint, 10f, 16f)
                is MdBlock.ChecklistItem -> {
                    val box = if (block.checked) "☑" else "☐"
                    drawWrapped("$box  " + plain(block.text), bulletPaint, 10f, 16f)
                }
            }
        }

        document.finishPage(page)
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
    }

    private fun plain(text: String) = MarkdownParser.stripInlineMarkup(text)
}
