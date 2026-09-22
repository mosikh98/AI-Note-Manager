package com.ainotes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.Folder
import com.ainotes.app.domain.model.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val recent: List<Note> = emptyList(),
    val favorites: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val selectedFolder: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedFolder = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> = combine(
        query.flatMapLatest { q ->
            if (q.isBlank()) flowOf(null) else container.repository.search(q)
        },
        container.repository.observeNotes(),
        container.repository.observeFavorites(),
        container.repository.observeFolders(),
        selectedFolder
    ) { searchResult, all, favorites, folders, folder ->
        val base = if (folder == null) all else all.filter { it.folderId == folder }
        HomeUiState(
            query = query.value,
            recent = searchResult ?: base,
            favorites = if (folder == null) favorites else favorites.filter { it.folderId == folder },
            folders = folders,
            selectedFolder = folder
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQuery(value: String) {
        query.value = value
    }

    fun selectFolder(id: String?) {
        selectedFolder.value = id
    }

    fun createNote(onCreated: (String) -> Unit) = viewModelScope.launch {
        val id = container.repository.createNote(title = "Untitled")
        onCreated(id)
    }

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        container.repository.toggleFavorite(note.id)
    }

    fun createFolder(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) container.repository.createFolder(name)
    }

    fun trash(note: Note) = viewModelScope.launch { container.repository.trash(note.id) }
}
