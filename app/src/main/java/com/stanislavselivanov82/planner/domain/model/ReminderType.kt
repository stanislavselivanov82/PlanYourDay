package com.stanislavselivanov82.planner.domain.model

enum class ReminderType(val databaseValue: Int) {
    NOTIFICATION(0),
    ALARM(1);

    companion object {
        fun fromDatabase(value: Int): ReminderType =
            entries.firstOrNull { it.databaseValue == value } ?: NOTIFICATION
    }
}
