package com.stanislavselivanov82.planner.data.local.prefs

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stanislavselivanov82.planner.domain.model.AppTheme
import com.stanislavselivanov82.planner.domain.model.CalendarViewMode
import com.stanislavselivanov82.planner.domain.model.PlannerSettings
import com.stanislavselivanov82.planner.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.plannerSettingsStore by preferencesDataStore(
    name = "planner_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext context: Context) : SettingsRepository {
    private val store = context.plannerSettingsStore
    private object Keys {
        val sleepStart = intPreferencesKey("sleepStart")
        val sleepEnd = intPreferencesKey("sleepEnd")
        val theme = stringPreferencesKey("theme")
        val calendarViewMode = stringPreferencesKey("calendarViewMode")
        val timeZoneId = stringPreferencesKey("timeZoneId")
        val reminderSoundUri = stringPreferencesKey("reminderSoundUri")
    }

    override val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        val start = prefs[Keys.sleepStart]?.takeIf { it in 0..1439 } ?: 1380
        val end = prefs[Keys.sleepEnd]?.takeIf { it in 0..1439 } ?: 420
        val validSleep = start != end
        PlannerSettings(
            sleepStart = if (validSleep) start else 1380,
            sleepEnd = if (validSleep) end else 420,
            theme = AppTheme.entries.firstOrNull { it.name == prefs[Keys.theme] } ?: AppTheme.DARK,
            calendarViewMode = CalendarViewMode.entries.firstOrNull { it.name == prefs[Keys.calendarViewMode] } ?: CalendarViewMode.MONTH,
            timeZoneId = prefs[Keys.timeZoneId]?.takeIf { it in ZoneId.getAvailableZoneIds() },
            reminderSoundUri = prefs[Keys.reminderSoundUri],
        )
    }

    override suspend fun setSleep(startMinutes: Int, endMinutes: Int) {
        require(startMinutes in 0..1439 && endMinutes in 0..1439 && startMinutes != endMinutes)
        store.edit { it[Keys.sleepStart] = startMinutes; it[Keys.sleepEnd] = endMinutes }
    }
    override suspend fun setTheme(theme: AppTheme) { store.edit { it[Keys.theme] = theme.name } }
    override suspend fun setCalendarViewMode(mode: CalendarViewMode) { store.edit { it[Keys.calendarViewMode] = mode.name } }
    override suspend fun setTimeZone(zoneId: String?) {
        require(zoneId == null || zoneId in ZoneId.getAvailableZoneIds())
        store.edit { if (zoneId == null) it.remove(Keys.timeZoneId) else it[Keys.timeZoneId] = zoneId }
    }
    override suspend fun setReminderSound(uri: String?) {
        require(uri == null || uri.startsWith("content://"))
        store.edit { if (uri == null) it.remove(Keys.reminderSoundUri) else it[Keys.reminderSoundUri] = uri }
    }
}
