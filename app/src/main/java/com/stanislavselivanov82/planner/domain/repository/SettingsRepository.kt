package com.stanislavselivanov82.planner.domain.repository

import com.stanislavselivanov82.planner.domain.model.AppTheme
import com.stanislavselivanov82.planner.domain.model.CalendarViewMode
import com.stanislavselivanov82.planner.domain.model.PlannerSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PlannerSettings>
    suspend fun setSleep(startMinutes: Int, endMinutes: Int)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setCalendarViewMode(mode: CalendarViewMode)
    suspend fun setTimeZone(zoneId: String?)
    /** Null selects the system default sound. */
    suspend fun setReminderSound(uri: String?)
}
