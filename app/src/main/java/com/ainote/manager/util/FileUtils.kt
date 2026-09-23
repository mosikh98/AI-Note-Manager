package com.ainote.manager.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class PickedFileInfo(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String,
)

object FileUtils {

    /** Copies the content behind [uri] into app-private storage so it survives even if the
     *  original source (e.g. a Downloads file) is later moved or the picker grants a
     *  temporary permission that expires. */
    fun copyToAppStorage(context: Context, uri: Uri): PickedFileInfo? {
        val resolver = context.contentResolver
        var displayName = "attachment"
        var size = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }

        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val uniqueName = "${UUID.randomUUID()}_$displayName"
        val destFile = File(attachmentsDir, uniqueName)

        return try {
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            if (size == 0L) size = destFile.length()

            PickedFileInfo(
                fileName = displayName,
                mimeType = mimeType,
                sizeBytes = size,
                localPath = destFile.absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun humanReadableSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024; unitIndex++
        }
        return "%.1f %s".format(value, units[unitIndex])
    }

    fun iconForMime(mimeType: String): String = when {
        mimeType.startsWith("image/") -> "🖼️"
        mimeType.startsWith("video/") -> "🎬"
        mimeType.startsWith("audio/") -> "🎵"
        mimeType == "application/pdf" -> "📕"
        mimeType.contains("zip") -> "🗜️"
        mimeType.contains("word") || mimeType.contains("document") -> "📄"
        mimeType.startsWith("text/") -> "📝"
        else -> "📎"
    }
}
