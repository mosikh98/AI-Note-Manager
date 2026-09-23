package com.ainote.manager.export

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    TXT("txt", "text/plain", "Plain Text (.txt)"),
    MARKDOWN("md", "text/markdown", "Markdown (.md)"),
    HTML("html", "text/html", "HTML (.html)"),
    PDF("pdf", "application/pdf", "PDF (.pdf)"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (.docx)"),
    JSON("json", "application/json", "JSON (.json)"),
}
