package com.ainote.manager.ui.noteedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.export.ExportFormat
import com.ainote.manager.ui.components.AttachmentItem
import com.ainote.manager.ui.components.ConfirmDialog
import com.ainote.manager.ui.components.MarkdownContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = viewModel(),
) {
    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    var isEditingContent by remember { mutableStateOf(true) }
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var attachMenuExpanded by remember { mutableStateOf(false) }
    var removeAttachmentTarget by remember { mutableStateOf<com.ainote.manager.data.AttachmentEntity?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Filled.Share, contentDescription = "Export & share")
                        }
                        DropdownMenu(expanded = exportMenuExpanded, onDismissRequest = { exportMenuExpanded = false }) {
                            ExportFormat.values().forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.label) },
                                    onClick = { exportMenuExpanded = false; viewModel.exportAndShare(format) }
                                )
                            }
                        }
                    }
                    IconButton(onClick = viewModel::backupToCloud) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Backup to cloud")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent)
            )

            Spacer(Modifier.height(8.dp))

            // Editor / Preview toggle — editing shows raw text for editing; toggled off shows
            // the fully rendered view (headings, bold, lists, checkboxes) per the spec.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = isEditingContent,
                        onClick = { isEditingContent = true },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Edit") }
                    SegmentedButton(
                        selected = !isEditingContent,
                        onClick = { isEditingContent = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Preview") }
                }

                FilledTonalButton(
                    onClick = viewModel::runAiOrganize,
                    enabled = aiState !is AiOrganizeUiState.Loading && content.isNotBlank()
                ) {
                    if (aiState is AiOrganizeUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Organize with AI")
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (isEditingContent) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = viewModel::onContentChange,
                        placeholder = { Text("Start typing your note… paste messy text and tap “Organize with AI”.") },
                        modifier = Modifier.fillMaxSize(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            MarkdownContent(
                                content = content,
                                onChecklistToggle = viewModel::onChecklistToggle,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Attachments row
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleMedium)
                Box {
                    TextButton(onClick = { attachMenuExpanded = true }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                    DropdownMenu(expanded = attachMenuExpanded, onDismissRequest = { attachMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Photo / Image") }, onClick = {
                            attachMenuExpanded = false
                            imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                        DropdownMenuItem(text = { Text("Any file") }, onClick = {
                            attachMenuExpanded = false
                            filePicker.launch(arrayOf("*/*"))
                        })
                    }
                }
            }
            if (attachments.isNotEmpty()) {
                LazyColumn(
                    Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(attachments, key = { it.id }) { attachment ->
                        AttachmentItem(
                            attachment = attachment,
                            onOpen = { /* opened via system viewer intent, wired in MainActivity-level util if needed */ },
                            onRemove = { removeAttachmentTarget = attachment }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // AI organize preview sheet
    when (val state = aiState) {
        is AiOrganizeUiState.Preview -> AiOrganizeSheet(
            originalContent = content,
            aiTitle = state.title,
            aiContent = state.content,
            onApply = viewModel::applyAiResult,
            onDismiss = viewModel::dismissAiPreview,
        )
        is AiOrganizeUiState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissAiPreview,
            title = { Text("AI Organize failed") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissAiPreview) { Text("OK") } }
        )
        is AiOrganizeUiState.NoProviderConfigured -> AlertDialog(
            onDismissRequest = viewModel::dismissAiPreview,
            title = { Text("No AI provider configured") },
            text = { Text("Add an API provider in Settings → AI Provider to use AI Organize.") },
            confirmButton = { TextButton(onClick = viewModel::dismissAiPreview) { Text("OK") } }
        )
        else -> {}
    }

    // Cloud upload feedback
    when (val state = uploadState) {
        is CloudUploadUiState.InProgress -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Uploading…") },
            text = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sending this note and its attachments to your cloud storage.")
                }
            },
            confirmButton = {}
        )
        is CloudUploadUiState.Success -> AlertDialog(
            onDismissRequest = viewModel::dismissUploadState,
            title = { Text("Backed up") },
            text = { Text("This note and its attachments were uploaded to your configured cloud storage.") },
            confirmButton = { TextButton(onClick = viewModel::dismissUploadState) { Text("OK") } }
        )
        is CloudUploadUiState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissUploadState,
            title = { Text("Backup failed") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissUploadState) { Text("OK") } }
        )
        is CloudUploadUiState.NoCloudConfigured -> AlertDialog(
            onDismissRequest = viewModel::dismissUploadState,
            title = { Text("No cloud storage configured") },
            text = { Text("Add a cloud storage endpoint in Cloud Backup settings first.") },
            confirmButton = { TextButton(onClick = viewModel::dismissUploadState) { Text("OK") } }
        )
        else -> {}
    }

    removeAttachmentTarget?.let { attachment ->
        ConfirmDialog(
            title = "Remove attachment?",
            message = "“${attachment.fileName}” will be removed from this note.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { viewModel.removeAttachment(attachment); removeAttachmentTarget = null },
            onDismiss = { removeAttachmentTarget = null }
        )
    }
}
