package com.stanislavselivanov82.planner.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotesState(val notes: List<Note> = emptyList(), val total: Int = 0,
    val query: String = "", val color: String? = null, val loading: Boolean = true, val failed: Boolean = false)

@HiltViewModel
class NotesViewModel @Inject constructor(private val repository: NoteRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val query = saved.getStateFlow("notesQuery", "")
    private val color = saved.getStateFlow<String?>("notesColor", null)
    private val errorChannel = Channel<Unit>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    val state = combine(repository.getAll(), query, color) { list, q, c ->
        NotesState(filterNotes(list, q, c), list.size, q, c, loading = false)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(NotesState(loading = false, failed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesState())
    fun search(value: String) { saved["notesQuery"] = value }
    fun filter(value: String?) { saved["notesColor"] = value }
    fun pin(note: Note) { viewModelScope.launch {
        try { repository.getById(note.id)?.let { repository.save(it.copy(isPinned = !it.isPinned)) } }
        catch (error: Exception) { if (error is CancellationException) throw error; errorChannel.send(Unit) }
    } }
}
