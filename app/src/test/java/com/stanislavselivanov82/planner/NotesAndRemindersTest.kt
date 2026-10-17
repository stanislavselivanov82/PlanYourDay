package com.stanislavselivanov82.planner

import androidx.lifecycle.SavedStateHandle
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.NoteRepository
import com.stanislavselivanov82.planner.ui.notes.NoteDraft
import com.stanislavselivanov82.planner.ui.notes.NoteEditViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NotesAndRemindersTest {
    private class MemoryNotes : NoteRepository {
        val rows = MutableStateFlow<List<Note>>(emptyList())
        var inserts = 0
        override fun getAll() = rows.asStateFlow()
        override fun search(query: String) = rows.map { filterNotes(it, query, null) }
        override suspend fun getById(id: Long) = rows.value.firstOrNull { it.id == id }
        override suspend fun save(note: Note): Long {
            val id = if (note.id == 0L) (++inserts).toLong() else note.id
            rows.value = rows.value.filterNot { it.id == id } + note.copy(id = id)
            return id
        }
        override suspend fun delete(id: Long): Boolean {
            val exists = rows.value.any { it.id == id }; rows.value = rows.value.filterNot { it.id == id }; return exists
        }
    }
    @Test fun supportsSixColors() {
        assertEquals(6, NoteColors.tags.size)
        NoteColors.tags.forEach { assertEquals(it, Note(colorTag = it).colorTag) }
    }
    @Test fun filtersCyrillicCaseAndColorAndPinnedFirst() {
        val notes = listOf(Note(id = 1, title = "ФЕРМА", colorTag = "orange", updatedAt = 300),
            Note(id = 2, content = "ферма", colorTag = "orange", isPinned = true, updatedAt = 1),
            Note(id = 3, content = "ферма", colorTag = "green"))
        assertEquals(listOf(2L, 1L), filterNotes(notes, "ферма", "orange").map { it.id })
        assertEquals(3, filterNotes(notes, "", null).size)
    }
    @Test fun autoSaveDebouncesAndNeverDuplicatesNewNote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = MemoryNotes()
            val vm = NoteEditViewModel(repository, SavedStateHandle(mapOf("noteId" to 0L)))
            runCurrent()
            vm.edit(NoteDraft(content = "A")); advanceTimeBy(300)
            vm.edit(NoteDraft(content = "AB", color = "orange")); advanceTimeBy(499); runCurrent()
            assertEquals(0, repository.inserts)
            advanceTimeBy(1); runCurrent()
            assertEquals(1, repository.inserts)
            vm.edit(vm.state.value.draft.copy(content = "ABC", pinned = true))
            advanceTimeBy(500); runCurrent()
            assertEquals(1, repository.inserts)
            assertEquals("ABC", repository.rows.value.single().content)
            assertTrue(repository.rows.value.single().isPinned)
            assertFalse(vm.state.value.dirty)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun closeFlushesWithoutWaitingForDebounce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = MemoryNotes(); val vm = NoteEditViewModel(repository, SavedStateHandle())
            runCurrent(); vm.edit(NoteDraft(content = "Text")); vm.close(); runCurrent()
            assertEquals("Text", repository.rows.value.single().content)
            assertEquals(1, repository.inserts)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun openingBlankEditorCreatesNoEmptyNote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = MemoryNotes(); val vm = NoteEditViewModel(repository, SavedStateHandle())
            runCurrent(); vm.close(); runCurrent(); assertEquals(0, repository.inserts)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun deletionDoesNotRecreateAutoSavedNote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = MemoryNotes(); val vm = NoteEditViewModel(repository, SavedStateHandle())
            runCurrent(); vm.edit(NoteDraft(content = "Text")); advanceTimeBy(500); runCurrent()
            vm.delete(); runCurrent(); advanceUntilIdle()
            assertTrue(repository.rows.value.isEmpty()); assertEquals(1, repository.inserts)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun reminderUsesTaskTimeMinusOffsetInSelectedZone() {
        val date = LocalDate.of(2026, 10, 17)
        val task = Task(title = "Task", date = date.toEpochDay(), startMinutes = 600, reminderOffsetMinutes = 15)
        val expected = date.atTime(9,45).atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()
        assertEquals(expected, reminderTimeMillis(task, ZoneId.of("Europe/Moscow")))
    }
    @Test fun reminderOffsetCanCrossMidnight() {
        val date = LocalDate.of(2026, 10, 17)
        val task = Task(title = "Task", date = date.toEpochDay(), startMinutes = 5, reminderOffsetMinutes = 15)
        assertEquals(date.minusDays(1).atTime(23,50).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            reminderTimeMillis(task, ZoneId.of("UTC")))
    }
    @Test fun completedDeletedAndDisabledTasksHaveNoActiveReminder() {
        val task = Task(title = "Task", reminderEnabled = true)
        assertTrue(task.hasActiveReminder())
        assertFalse(task.copy(isDone = true).hasActiveReminder())
        assertFalse(task.copy(deletedAt = 1).hasActiveReminder())
        assertFalse(task.copy(reminderEnabled = false).hasActiveReminder())
    }
}
