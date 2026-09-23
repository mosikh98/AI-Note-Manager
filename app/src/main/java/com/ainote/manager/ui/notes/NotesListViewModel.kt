package com.ainote.manager.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainote.manager.data.NoteEntity
import com.ainote.manager.data.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository.getInstance(app)

    private val query = MutableStateFlow("")
    fun onQueryChange(value: String) { query.value = value }

    val notes: StateFlow<List<NoteEntity>> = query
        .debounce(200)
        .flatMapLatest { q -> if (q.isBlank()) repo.observeNotes() else repo.searchNotes(q) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val currentQuery: StateFlow<String> = query.asStateFlow()

    fun toggleFavorite(note: NoteEntity) = viewModelScope.launch {
        repo.setFavorite(note.id, !note.isFavorite)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repo.deleteNote(note)
    }

    fun createNote(onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = repo.createNote(title = "", content = "")
        onCreated(id)
    }
}
