package com.ainote.manager.export

import android.content.Context
import android.content.Intent
import com.ainote.manager.data.NoteEntity
import com.ainote.manager.util.FileUtils
import java.io.File

object NoteExporter {

    /** Renders [note] into [format] under the app's cache/exports dir and returns the file. */
    fun exportToFile(context: Context, note: NoteEntity, format: ExportFormat): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeTitle = note.title.ifBlank { "note" }
            .replace(Regex("[^A-Za-z0-9 _-]"), "").take(60).ifBlank { "note" }
        val outFile = File(exportsDir, "$safeTitle.${format.extension}")

        when (format) {
            ExportFormat.TXT -> TxtExporter.export(note, outFile)
            ExportFormat.MARKDOWN -> MarkdownExporter.export(note, outFile)
            ExportFormat.HTML -> HtmlExporter.export(note, outFile)
            ExportFormat.PDF -> PdfExporter.export(note, outFile)
            ExportFormat.DOCX -> DocxExporter.export(note, outFile)
            ExportFormat.JSON -> JsonExporter.export(note, outFile)
        }
        return outFile
    }

    /** Exports then launches Android's share sheet for the resulting file. */
    fun exportAndShare(context: Context, note: NoteEntity, format: ExportFormat) {
        val file = exportToFile(context, note, format)
        val uri = FileUtils.uriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${note.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
