package com.ainotes.app.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val attachmentCount: Int = 0,
    val syncStatus: String = "SYNCED"
)

data class Folder(val id: String, val name: String, val parentId: String? = null)
data class Tag(val id: String, val name: String)

data class Attachment(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String?,
    val fileSize: Long,
    val localUri: String,
    val createdAt: Long
)

data class AiProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String = "",
    val organizationId: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
    val isActive: Boolean = false
)

enum class AiAction(val label: String) {
    ORGANIZE("Organize"),
    SUMMARIZE("Summarize"),
    EXPAND("Expand"),
    REWRITE("Rewrite"),
    EXTRACT_TASKS("Extract Tasks"),
    GENERATE_TITLE("Generate Title"),
    TRANSLATE("Translate"),
    ASK("Ask AI")
}

enum class ExportFormat(val label: String, val mime: String, val extension: String) {
    TXT("Text", "text/plain", "txt"),
    MARKDOWN("Markdown", "text/markdown", "md"),
    HTML("HTML", "text/html", "html"),
    PDF("PDF", "application/pdf", "pdf"),
    DOCX(
        "Word (DOCX)",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "docx"
    ),
    JSON("JSON", "application/json", "json")
}

enum class SyncState(val label: String) {
    SYNCED("Synced"),
    PENDING_UPLOAD("Pending upload"),
    UPLOADING("Uploading"),
    DOWNLOADED("Downloaded"),
    CONFLICT("Conflict"),
    ERROR("Error")
}
