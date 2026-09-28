package com.ainote.manager.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.MediaMetadataRetriever
import com.ainote.manager.data.AttachmentEntity
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

    fun export(note: NoteEntity, outFile: File, attachments: List<AttachmentEntity> = emptyList()) {
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

        attachments.filter { it.mimeType.startsWith("image/") || it.mimeType.startsWith("video/") }.forEach { attachment ->
            val file = File(attachment.localPath)
            if (!file.isFile) return@forEach
            val bitmap = if (attachment.mimeType.startsWith("video/")) videoFrame(file) else decodeImage(file)
                ?: return@forEach

            drawWrapped("${if (attachment.mimeType.startsWith("video/")) "Video" else "Image"}: ${attachment.fileName}", bodyPaint, 0f, 16f)
            val maxWidth = PAGE_WIDTH - MARGIN * 2
            val scale = minOf(maxWidth / bitmap.width, 320f / bitmap.height)
            val imageWidth = bitmap.width * scale
            val imageHeight = bitmap.height * scale
            newPageIfNeeded(imageHeight + 12f)
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(MARGIN, y, MARGIN + imageWidth, y + imageHeight),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            y += imageHeight + 12f
            bitmap.recycle()
        }

        document.finishPage(page)
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
    }

    private fun decodeImage(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxWidth = PAGE_WIDTH - MARGIN.toInt() * 2
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > 640) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }

    private fun videoFrame(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun plain(text: String) = MarkdownParser.stripInlineMarkup(text)
}
