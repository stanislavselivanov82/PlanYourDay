package com.stanislavselivanov82.planner

import com.stanislavselivanov82.planner.domain.model.AppTheme
import com.stanislavselivanov82.planner.domain.model.CalendarViewMode
import com.stanislavselivanov82.planner.domain.model.PlannerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerSettingsTest {
    @Test fun defaultsMatchSpecification() {
        val settings = PlannerSettings()
        assertEquals(480, settings.sleepMinutes)
        assertEquals(960, settings.wakingMinutes)
        assertEquals(AppTheme.DARK, settings.theme)
        assertEquals(CalendarViewMode.MONTH, settings.calendarViewMode)
    }
    @Test fun supportsSleepWithoutMidnightCrossing() {
        val settings = PlannerSettings(sleepStart = 60, sleepEnd = 360)
        assertEquals(300, settings.sleepMinutes)
        assertEquals(1140, settings.wakingMinutes)
    }
    @Test fun acceptsExplicitTimeZone() {
        assertEquals("Europe/Moscow", PlannerSettings(timeZoneId = "Europe/Moscow").zoneId.id)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsIdenticalSleepBoundaries() { PlannerSettings(sleepStart = 420, sleepEnd = 420) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTimeZone() { PlannerSettings(timeZoneId = "Not/AZone") }
}
