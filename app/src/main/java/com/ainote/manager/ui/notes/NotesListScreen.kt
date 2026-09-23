package com.ainote.manager.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainote.manager.data.NoteEntity
import com.ainote.manager.ui.components.ConfirmDialog
import com.ainote.manager.ui.components.EmptyState
import com.ainote.manager.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCloudSettings: () -> Unit,
    viewModel: NotesListViewModel = viewModel(),
) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.currentQuery.collectAsState()
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text("Search notes…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("My Notes")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) viewModel.onQueryChange("")
                    }) {
                        Icon(if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenCloudSettings) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Cloud backup")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createNote(onOpenNote) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Notes,
                title = if (query.isBlank()) "No notes yet" else "No matches",
                subtitle = if (query.isBlank())
                    "Tap “New note” to write something — you can organize it with AI afterwards."
                else "Try a different search term.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { noteToDelete = note },
                        onToggleFavorite = { viewModel.toggleFavorite(note) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    noteToDelete?.let { note ->
        ConfirmDialog(
            title = "Delete note?",
            message = "“${note.title.ifBlank { "Untitled" }}” and its attachments will be permanently deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.deleteNote(note); noteToDelete = null },
            onDismiss = { noteToDelete = null }
        )
    }
}
