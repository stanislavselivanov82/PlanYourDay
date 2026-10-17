package com.stanislavselivanov82.planner.domain.model

import java.time.LocalDate
import java.time.ZoneId

fun reminderTimeMillis(task: Task, zoneId: ZoneId): Long = LocalDate.ofEpochDay(task.date)
    .atStartOfDay().plusMinutes(task.startMinutes.toLong()).atZone(zoneId)
    .toInstant().toEpochMilli() - (task.reminderOffsetMinutes ?: 0) * 60_000L

fun Task.hasActiveReminder(): Boolean = reminderEnabled && !isDone && deletedAt == null
