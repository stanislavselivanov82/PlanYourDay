package com.stanislavselivanov82.planner.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.Note
import com.stanislavselivanov82.planner.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

 data class NoteDraft(val title: String = "", val content: String = "", val color: String? = null, val pinned: Boolean = false)
 data class NoteEditState(val draft: NoteDraft = NoteDraft(), val loading: Boolean = true,
    val saving: Boolean = false, val dirty: Boolean = false, val error: Boolean = false, val closing: Boolean = false)

@HiltViewModel
class NoteEditViewModel @Inject constructor(private val repository: NoteRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(NoteEditState())
    val state = mutable.asStateFlow()
    private val closeChannel = Channel<Unit>(Channel.BUFFERED)
    val closed = closeChannel.receiveAsFlow()
    private val mutex = Mutex()
    private var debounce: Job? = null
    private var id: Long = saved.get<Long>("persistedNoteId") ?: saved.get<Long>("noteId") ?: 0L
    private var baseline: NoteDraft? = null

    init { viewModelScope.launch {
        try {
            val note = if (id == 0L) null else requireNotNull(repository.getById(id))
            baseline = NoteDraft(note?.title.orEmpty(), note?.content.orEmpty(), note?.colorTag, note?.isPinned ?: false)
            val draft = if (saved.get<Boolean>("hasDraft") == true) NoteDraft(
                saved.get<String>("draftTitle").orEmpty(), saved.get<String>("draftContent").orEmpty(),
                saved.get<String>("draftColor"), saved.get<Boolean>("draftPinned") ?: false) else requireNotNull(baseline)
            mutable.value = NoteEditState(draft, loading = false, dirty = draft != baseline)
            if (draft != baseline) schedule()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            mutable.value = NoteEditState(loading = false, error = true)
        }
    } }

    fun edit(draft: NoteDraft) {
        if (mutable.value.loading || mutable.value.closing) return
        saved["hasDraft"] = true; saved["draftTitle"] = draft.title; saved["draftContent"] = draft.content
        saved["draftColor"] = draft.color; saved["draftPinned"] = draft.pinned
        mutable.update { it.copy(draft = draft, dirty = draft != baseline, error = false) }
        schedule()
    }
    private fun schedule() {
        debounce?.cancel()
        debounce = viewModelScope.launch { delay(500); persist() }
    }
    // Once a DB write starts it completes, even if a newer edit cancels the debounce job.
    private suspend fun persist(): Boolean = withContext(NonCancellable) { mutex.withLock {
        if (mutable.value.loading || baseline == null) return@withLock false
        val draft = mutable.value.draft
        if (draft == baseline) return@withLock true
        if (id == 0L && draft.title.isBlank() && draft.content.isBlank()) {
            baseline = draft; mutable.update { it.copy(dirty = false) }; return@withLock true
        }
        mutable.update { it.copy(saving = true, error = false) }
        try {
            val old = if (id == 0L) null else requireNotNull(repository.getById(id))
            id = repository.save((old ?: Note()).copy(title = draft.title, content = draft.content,
                colorTag = draft.color, isPinned = draft.pinned))
            saved["persistedNoteId"] = id
            baseline = draft
            mutable.update { it.copy(saving = false, dirty = it.draft != draft, error = false) }
            true
        } catch (error: Exception) {
            mutable.update { it.copy(saving = false, error = true) }; false
        }
    } }
    fun flush() { debounce?.cancel(); viewModelScope.launch { persist() } }
    fun close() {
        if (mutable.value.closing) return
        debounce?.cancel(); mutable.update { it.copy(closing = true) }
        viewModelScope.launch {
            if (persist()) closeChannel.send(Unit) else mutable.update { it.copy(closing = false) }
        }
    }
    fun delete() {
        if (mutable.value.closing || mutable.value.loading) return
        debounce?.cancel(); mutable.update { it.copy(closing = true) }
        viewModelScope.launch { mutex.withLock {
            try {
                if (id != 0L) check(repository.delete(id))
                closeChannel.send(Unit)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(closing = false, error = true) }
            }
        } }
    }
}
