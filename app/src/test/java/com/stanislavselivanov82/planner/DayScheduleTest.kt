package com.stanislavselivanov82.planner

import com.stanislavselivanov82.planner.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DayScheduleTest {
    private val settings = PlannerSettings()
    private fun task(start: Int, duration: Int = 60, day: Long = 100, id: Long = 1) =
        Task(id = id, title = "Test", date = day, startMinutes = start, durationMinutes = duration)

    @Test fun defaultWakingAxisContainsNoSleep() {
        assertEquals(listOf(DaySegment(420, 1380, 0)), DaySchedule.waking(settings))
        assertEquals(960, DaySchedule.waking(settings).sumOf { it.duration })
    }
    @Test fun sleepChronologyCrossesMidnight() {
        assertEquals(listOf(DaySegment(1380, 1440, 0), DaySegment(0, 420, 60)), DaySchedule.sleep(settings))
    }
    @Test fun sleepEndIsExclusive() {
        assertTrue(DaySchedule.slices(listOf(task(420)), 100, DaySchedule.sleep(settings)).isEmpty())
    }
    @Test fun sleepStartIsInclusive() {
        assertEquals(60, DaySchedule.slices(listOf(task(1380)), 100, DaySchedule.sleep(settings)).sumOf { it.duration })
    }
    @Test fun taskEndingAtSleepStartIsNotSleeping() {
        assertTrue(DaySchedule.slices(listOf(task(1320)), 100, DaySchedule.sleep(settings)).isEmpty())
    }
    @Test fun splitsTaskAtSleepBoundary() {
        val tasks = listOf(task(1350))
        assertEquals(30, DaySchedule.slices(tasks, 100, DaySchedule.sleep(settings)).sumOf { it.duration })
        assertEquals(30, DaySchedule.slices(tasks, 100, DaySchedule.waking(settings)).sumOf { it.duration })
    }
    @Test fun previousDayTaskContinuesAfterMidnight() {
        val slices = DaySchedule.slices(listOf(task(1410, 120, 99)), 100, DaySchedule.sleep(settings))
        assertEquals(90, slices.sumOf { it.duration })
        assertEquals(60, slices.single().offset)
    }
    @Test fun taskAfterMidnightIsNotDuplicatedIntoSameCivilDay() {
        assertEquals(30, DaySchedule.slices(listOf(task(1410, 120)), 100, DaySchedule.sleep(settings)).sumOf { it.duration })
    }
    @Test fun wakingAxisCanCrossMidnight() {
        val prefs = PlannerSettings(sleepStart = 120, sleepEnd = 480)
        assertEquals(listOf(DaySegment(480, 1440, 0), DaySegment(0, 120, 960)), DaySchedule.waking(prefs))
    }
    @Test fun touchingTasksDoNotConflict() {
        assertTrue(DaySchedule.conflicts(listOf(task(600), task(660, id = 2)), 100).isEmpty())
    }
    @Test fun overlappingTasksConflict() {
        assertEquals(setOf(1L, 2L), DaySchedule.conflicts(listOf(task(600), task(630, id = 2)), 100))
    }
    @Test fun allDayMinutesPartitionExactlyForNonHourlyBoundaries() {
        val prefs = PlannerSettings(sleepStart = 1405, sleepEnd = 437)
        val tasks = listOf(task(0, 1440))
        assertEquals(1440, (DaySchedule.slices(tasks, 100, DaySchedule.sleep(prefs)) +
            DaySchedule.slices(tasks, 100, DaySchedule.waking(prefs))).sumOf { it.duration })
    }
}
