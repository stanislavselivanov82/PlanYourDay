package com.stanislavselivanov82.planner.domain.model

import java.time.LocalDate

/** Dates are epoch days; times are local wall-clock minutes, not timestamps. */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val date: Long = LocalDate.now().toEpochDay(),
    val startMinutes: Int = 9 * 60,
    val durationMinutes: Int = 60,
    val categoryId: Long? = null,
    val isDone: Boolean = false,
    val isPinned: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int? = null,
    val reminderType: ReminderType = ReminderType.NOTIFICATION,
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null,
) {
    init {
        require(title.isNotBlank())
        require(startMinutes in 0..1439)
        require(durationMinutes > 0)
        require(reminderOffsetMinutes == null || reminderOffsetMinutes >= 0)
    }

    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val endMinutes: Long get() = startMinutes.toLong() + durationMinutes
}
