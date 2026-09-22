package com.ainotes.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.Note
import com.ainotes.app.ui.components.EmptyState
import com.ainotes.app.ui.components.SectionLabel
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel { HomeViewModel(container) }
) {
    val state by vm.state.collectAsState()
    var showNewFolder by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.createNote(onOpenNote) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI Notes",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Your notes. Your files. Your AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Search notes, tags, folders...") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            if (state.folders.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp, vertical = 10.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipBox(
                            label = "All",
                            selected = state.selectedFolder == null,
                            onClick = { vm.selectFolder(null) }
                        )
                    }
                    items(state.folders) { folder ->
                        FilterChipBox(
                            label = folder.name,
                            selected = state.selectedFolder == folder.id,
                            onClick = { vm.selectFolder(folder.id) }
                        )
                    }
                    item {
                        FilterChipBox(
                            label = "+ Folder",
                            selected = false,
                            onClick = { showNewFolder = true }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.favorites.isNotEmpty() && state.query.isBlank()) {
                    item { SectionLabel("Favorites") }
                    items(state.favorites, key = { "fav-" + it.id }) { note ->
                        NoteCard(note, onOpen = { onOpenNote(note.id) }, onToggleFavorite = { vm.toggleFavorite(note) })
                    }
                }

                item { SectionLabel(if (state.query.isBlank()) "Recent" else "Results") }

                if (state.recent.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No notes yet",
                            subtitle = "Tap \u201CNew note\u201D to write something,\nor paste messy text and let AI organize it."
                        )
                    }
                } else {
                    items(state.recent, key = { it.id }) { note ->
                        NoteCard(
                            note,
                            onOpen = { onOpenNote(note.id) },
                            onToggleFavorite = { vm.toggleFavorite(note) }
                        )
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    placeholder = { Text("Folder name") }
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.createFolder(folderName)
                    showNewFolder = false
                }) { Text("Create") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNewFolder = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterChipBox(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun NoteCard(note: Note, onOpen: () -> Unit, onToggleFavorite: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note.content.replace("\n", " ").take(120),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Edited " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    if (note.tags.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            note.tags.take(2).joinToString(" ") { "#" + it },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
