package com.stanislavselivanov82.planner.data.local.db.entity

import com.stanislavselivanov82.planner.domain.model.Category
import com.stanislavselivanov82.planner.domain.model.Note
import com.stanislavselivanov82.planner.domain.model.ReminderType
import com.stanislavselivanov82.planner.domain.model.Task

fun CategoryEntity.toDomain(): Category = Category(
    id = id, name = name, colorHex = colorHex, sortOrder = sortOrder, isSystem = isSystem,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id, name = name, colorHex = colorHex, sortOrder = sortOrder, isSystem = isSystem,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    date = date,
    startMinutes = startMinutes,
    durationMinutes = durationMinutes,
    categoryId = categoryId,
    isDone = isDone,
    isPinned = isPinned,
    reminderEnabled = reminderEnabled,
    reminderOffsetMinutes = reminderOffsetMinutes,
    reminderType = ReminderType.fromDatabase(reminderType),
    isRecurring = isRecurring,
    recurrenceRule = recurrenceRule,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    date = date,
    startMinutes = startMinutes,
    durationMinutes = durationMinutes,
    categoryId = categoryId,
    isDone = isDone,
    isPinned = isPinned,
    reminderEnabled = reminderEnabled,
    reminderOffsetMinutes = reminderOffsetMinutes,
    reminderType = reminderType.databaseValue,
    isRecurring = isRecurring,
    recurrenceRule = recurrenceRule,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun NoteEntity.toDomain(): Note = Note(
    id = id, title = title, content = content, colorTag = colorTag,
    isPinned = isPinned, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id, title = title, content = content, colorTag = colorTag,
    isPinned = isPinned, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt,
)
