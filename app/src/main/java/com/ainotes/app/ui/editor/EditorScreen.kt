package com.ainotes.app.ui.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.AiAction
import com.ainotes.app.domain.model.ExportFormat
import com.ainotes.app.ui.components.EmptyState
import com.ainotes.app.ui.components.LoadingDots
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    container: AppContainer,
    noteId: String,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
    vm: EditorViewModel = viewModel { EditorViewModel(container, noteId) }
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var showAiSheet by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var showAsk by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var askText by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.addAttachment(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.loaded) "یادداشت" else "در حال بارگذاری...") },
                navigationIcon = {
                    IconButton(onClick = { vm.persist(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showTags = true }) {
                        Text("#", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "برون‌بری")
                    }
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "علاقه"
                        )
                    }
                    IconButton(onClick = { preview = !preview }) {
                        Icon(
                            if (preview) Icons.Filled.Edit else Icons.Filled.Visibility,
                            contentDescription = if (preview) "ویرایش" else "پیش‌نمایش"
                        )
                    }
                    IconButton(onClick = { vm.persist() }) {
                        Icon(Icons.Filled.Save, contentDescription = "ذخیره")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiSheet = true },
                icon = { Text("\u2728") },
                text = { Text("✨ سازمان‌دهی با AI") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitle,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("عنوان") },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            if (!preview) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(
                    listOf(
                        "H2" to "## ", "B" to "**", "I" to "_", "u" to "--",
                        "\u2022" to "- ", "1." to "1. ", "[] \u2610" to "\u2610 ",
                        "> \u201C" to "> ", "</>" to "```\n```", "link" to "[text](url)"
                    )
                ) { (label, token) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { vm.insertToken(token) }
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState = preview,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + slideInVertically { it / 14 }) togetherWith
                        (fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 14 })
                },
                label = "editorMode"
            ) { isPreview ->
            if (isPreview) {
                if (state.content.isBlank()) {
                    Text(
                        "چیزی برای پیش‌نمایش نیست.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    com.ainotes.app.ui.markdown.MarkdownView(
                        content = state.content,
                        onToggleLine = vm::toggleLine,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = state.content,
                    onValueChange = vm::onContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    placeholder = {
                        Text("بنویس، متن نامرتب بذار، یا روی «✨ سازمان‌دهی با AI» بزن")
                    }
                )
            }
            }

            if (state.ai.running) {
                Spacer(Modifier.height(14.dp))
                LoadingDots(label = state.ai.stage.ifBlank { "در حال انجام..." })
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            state.ai.error?.let { err ->
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            TextButton(onClick = onOpenProviders) { Text("تنظیمات AI") }
                            TextButton(onClick = vm::dismissAi) { Text("بستن") }
                        }
                    }
                }
            }

            state.ai.result?.let { result ->
                Spacer(Modifier.height(14.dp))
                AiPreviewDialog(
                    original = state.ai.original ?: "",
                    result = result,
                    onApply = vm::applyAiResult,
                    onCancel = vm::cancelAi
                )
            }

            Spacer(Modifier.height(14.dp))

            if (state.attachments.isNotEmpty()) {
                Text(
                    "پیوست‌ها",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(6.dp))
                state.attachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(iconFor(att.mimeType), Modifier.padding(end = 10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    att.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    formatSize(att.fileSize),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                android.net.Uri.parse(att.localUri),
                                                att.mimeType ?: "*/*"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    )
                                }
                            }) { Text("\u2197") }
                            IconButton(onClick = { vm.removeAttachment(att.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }

    // add attachment FAB lives in the sheet to keep one FAB on screen
    if (showAiSheet) {
        ModalBottomSheet(onDismissRequest = { showAiSheet = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "✨ اکشن‌های AI",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                AiAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        leadingIcon = { Text("\u2728") },
                        onClick = {
                            showAiSheet = false
                            if (action == AiAction.ASK) showAsk = true else vm.runAi(action)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("افزودن فایل پیوست") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = {
                        showAiSheet = false
                        picker.launch(
                            arrayOf(
                                "image/*", "video/*", "application/pdf", "text/*",
                                "application/zip", "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "audio/*", "*/*"
                            )
                        )
                    }
                )
            }
        }
    }

    if (showAsk) {
        AlertDialog(
            onDismissRequest = { showAsk = false },
            title = { Text("پرسش از AI دربارهٔ این یادداشت") },
            text = {
                OutlinedTextField(
                    value = askText,
                    onValueChange = { askText = it },
                    placeholder = { Text("کارهای باقی‌مونده چیه؟") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAsk = false
                    vm.runAi(AiAction.ASK, askText)
                    askText = ""
                }) { Text("پرسیدن") }
            },
            dismissButton = {
                TextButton(onClick = { showAsk = false }) { Text("انصراف") }
            }
        )
    }

    if (showTags) {
        var tagsDraft by remember { mutableStateOf(state.tags.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showTags = false },
            title = { Text("تگ‌ها") },
            text = {
                OutlinedTextField(
                    value = tagsDraft,
                    onValueChange = { tagsDraft = it },
                    placeholder = { Text("کاری، ایده‌ها، شخصی") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTags(tagsDraft)
                    vm.persist()
                    showTags = false
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showTags = false }) { Text("انصراف") }
            }
        )
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("برون‌بری یادداشت") },
            text = {
                Column {
                    Text(
                        "✨ مسترپیس — اول متن رو با AI به‌شدت بهینه‌سازی کن، بعد خروجی بگیر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExport = false
                                vm.runAi(com.ainotes.app.domain.model.AiAction.MASTERPIECE)
                            }
                            .padding(vertical = 10.dp)
                    )
                    ExportFormat.entries.forEach { format ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExport = false
                                    vm.export(format)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(format.label, Modifier.weight(1f))
                            Text("\u25B8")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExport = false }) { Text("انصراف") }
            }
        )
    }

    state.exported?.let { out ->
        AlertDialog(
            onDismissRequest = vm::clearExported,
            title = { Text("خروجی آماده شد") },
            text = { Text(out.name) },
            confirmButton = {
                TextButton(onClick = {
                    vm.shareExported()
                    vm.clearExported()
                }) { Text("اشتراک‌گذاری") }
            },
            dismissButton = {
                TextButton(onClick = vm::clearExported) { Text("بستن") }
            }
        )
    }
}

@Composable
private fun AiPreviewDialog(
    original: String,
    result: String,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    var compare by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("نتیجهٔ AI", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (compare) {
                Text(
                    "متن اصلی",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    original,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "نسخهٔ AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(result, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onApply) { Text("اعمال") }
                TextButton(onClick = onCancel) { Text("انصراف") }
                TextButton(onClick = { compare = !compare }) {
                    Text(if (compare) "پنهان کردن اصل" else "مقایسه")
                }
            }
        }
    }
}

private fun iconFor(mime: String?): String = when {
    mime == null -> "\uD83D\uDCC4"
    mime.startsWith("image/") -> "\uD83D\uDDBC\uFE0F"
    mime.startsWith("video/") -> "\uD83C\uDFA5"
    mime.startsWith("audio/") -> "\uD83C\uDFB5"
    mime.contains("zip") -> "\uD83D\uDCE6"
    mime.contains("pdf") -> "\uD83D\uDCC4"
    else -> "\uD83D\uDCC4"
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
