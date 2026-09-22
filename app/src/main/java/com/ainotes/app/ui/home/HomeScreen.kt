package com.ainotes.app.ui.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.Note
import com.ainotes.app.ui.components.EmptyState
import com.ainotes.app.ui.components.SectionLabel
import com.ainotes.app.ui.i18n.txt
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private fun mediaPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_IMAGES"
    else "android.permission.READ_EXTERNAL_STORAGE"

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel { HomeViewModel(container) }
) {
    val state by vm.state.collectAsState()
    var showNewFolder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var crashLog by remember {
        mutableStateOf(
            runCatching {
                File(container.app.filesDir, "crash.log").takeIf { it.exists() }?.readText()
            }.getOrNull()
        )
    }

    var showPermDialog by remember { mutableStateOf(false) }
    var permGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                container.app, mediaPermission()
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permGranted = granted }

    // Gate on the REAL permission state: keep offering on launch until granted.
    LaunchedEffect(permGranted) {
        if (!permGranted) showPermDialog = true
    }

    var newFolderName by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.createNote(onOpenNote) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(txt("یادداشت جدید", "New note")) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            txt("یادداشت‌های من", "My Notes"),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            txt("یادداشت‌هات، فایل‌ها، هوش مصنوعی‌ات", "Your notes, files and AI"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = txt("تنظیمات", "Settings"),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(txt("جستجو در یادداشت‌ها، تگ‌ها، پوشه‌ها...", "Search notes, tags, folders..."))
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            if (state.folders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipBox(
                            label = txt("همه", "All"),
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
                            label = txt("+ پوشه", "+ Folder"),
                            selected = false,
                            onClick = {
                                newFolderName = ""
                                showNewFolder = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.favorites.isNotEmpty() && state.query.isBlank()) {
                    item { SectionLabel(txt("علاقه‌مندی‌ها", "Favorites")) }
                    items(state.favorites, key = { "fav-" + it.id }) { note ->
                        NoteCard(
                            note,
                            onOpen = { onOpenNote(note.id) },
                            onToggleFavorite = { vm.toggleFavorite(note) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                item {
                    SectionLabel(
                        if (state.query.isBlank()) txt("اخیر", "Recent")
                        else txt("نتایج", "Results")
                    )
                }

                if (state.recent.isEmpty()) {
                    item {
                        EmptyState(
                            title = txt("هنوز یادداشتی نداری", "No notes yet"),
                            subtitle = txt(
                                "روی «یادداشت جدید» بزن، یا متن نامرتب رو بذار تا خودم مرتبش کنم.",
                                "Tap \u201CNew note\u201D, or paste messy text and let AI organize it."
                            )
                        )
                    }
                } else {
                    items(state.recent, key = { it.id }) { note ->
                        NoteCard(
                            note,
                            onOpen = { onOpenNote(note.id) },
                            onToggleFavorite = { vm.toggleFavorite(note) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(txt("پوشهٔ جدید", "New folder")) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    placeholder = { Text(txt("نام پوشه", "Folder name")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createFolder(newFolderName)
                    showNewFolder = false
                }) { Text(txt("ساختن", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) {
                    Text(txt("انصراف", "Cancel"))
                }
            }
        )
    }

    if (showPermDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermDialog = false
                            },
            title = { Text(txt("دسترسی به حافظه", "Storage access")) },
            text = {
                Text(
                    txt(
                        "برای انتخاب عکس و فایل‌ها از حافظهٔ گوشی و پیوست کردنشون به یادداشت‌ها، اجازهٔ دسترسی لازمه.\n\nفقط وقتی خودت پیوست بگیری استفاده میشه.",
                        "To pick photos and files from storage and attach them to notes, permission is needed.\n\nUsed only when you attach something."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermDialog = false
                                        permLauncher.launch(mediaPermission())
                }) { Text(txt("اجازه بده", "Allow")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermDialog = false
                                    }) { Text(txt("الان نه", "Not now")) }
            }
        )
    }

    crashLog?.let { trace ->
        AlertDialog(
            onDismissRequest = { crashLog = null },
            title = { Text(txt("متاسفانه اپ بسته شد", "The app crashed")) },
            text = {
                Text(
                    trace.take(2500),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(trace))
                    crashLog = null
                }) { Text(txt("کپی خطا", "Copy error")) }
            },
            dismissButton = {
                TextButton(onClick = { crashLog = null }) { Text(txt("بستن", "Close")) }
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
fun NoteCard(
    note: Note,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { txt("بدون عنوان", "Untitled") },
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
                        txt("ویرایش: ", "Edited: ") + DateFormat
                            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
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
                    contentDescription = txt("علاقه", "Favorite"),
                    tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
