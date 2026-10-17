package com.stanislavselivanov82.planner

import com.stanislavselivanov82.planner.data.local.db.entity.toDomain
import com.stanislavselivanov82.planner.data.local.db.entity.toEntity
import com.stanislavselivanov82.planner.domain.model.Category
import com.stanislavselivanov82.planner.domain.model.Note
import com.stanislavselivanov82.planner.domain.model.ReminderType
import com.stanislavselivanov82.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    @Test fun taskRoundTripPreservesEveryField() {
        val task = Task(
            id = 8, title = "Task", description = "Text", date = 20_000,
            startMinutes = 1430, durationMinutes = 120, categoryId = 1,
            isDone = true, isPinned = true, reminderEnabled = true,
            reminderOffsetMinutes = 15, reminderType = ReminderType.ALARM,
            isRecurring = true, recurrenceRule = "FREQ=DAILY", createdAt = 100,
            updatedAt = 200, deletedAt = 300,
        )
        assertEquals(task, task.toEntity().toDomain())
        assertEquals(1550L, task.endMinutes)
    }

    @Test fun categoryRoundTripPreservesEveryField() {
        val category = Category(5, "Custom", "#5db6ff", 7, false)
        assertEquals(category, category.toEntity().toDomain())
    }

    @Test fun noteRoundTripPreservesEveryField() {
        val note = Note(4, "Title", "Text", "purple", true, 100, 200, 300)
        assertEquals(note, note.toEntity().toDomain())
    }

    @Test fun unknownReminderTypeFallsBackToNotification() {
        assertEquals(ReminderType.NOTIFICATION, ReminderType.fromDatabase(99))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyTitle() { Task(title = " ") }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidStartTime() { Task(title = "Task", startMinutes = 1440) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroDuration() { Task(title = "Task", durationMinutes = 0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeReminderOffset() { Task(title = "Task", reminderOffsetMinutes = -1) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidColor() { Category(name = "Custom", colorHex = "red") }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownNoteColor() { Note(colorTag = "unknown") }
}
