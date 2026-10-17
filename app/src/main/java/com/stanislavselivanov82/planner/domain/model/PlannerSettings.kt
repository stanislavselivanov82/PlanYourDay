package com.stanislavselivanov82.planner.domain.model

import java.time.ZoneId

enum class AppTheme { DARK, LIGHT, SYSTEM }
enum class CalendarViewMode { MONTH, WEEK }

data class PlannerSettings(
    val sleepStart: Int = 23 * 60,
    val sleepEnd: Int = 7 * 60,
    val theme: AppTheme = AppTheme.DARK,
    val calendarViewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val timeZoneId: String? = null,
    val reminderSoundUri: String? = null,
) {
    init {
        require(sleepStart in 0..1439 && sleepEnd in 0..1439)
        require(sleepStart != sleepEnd)
        require(timeZoneId == null || timeZoneId in ZoneId.getAvailableZoneIds())
    }

    val sleepMinutes: Int get() = (sleepEnd - sleepStart + 1440) % 1440
    val wakingMinutes: Int get() = 1440 - sleepMinutes
    val zoneId: ZoneId get() = timeZoneId?.let(ZoneId::of) ?: ZoneId.systemDefault()
}
