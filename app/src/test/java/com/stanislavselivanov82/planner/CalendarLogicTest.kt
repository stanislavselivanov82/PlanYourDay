package com.stanislavselivanov82.planner

import com.stanislavselivanov82.planner.domain.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CalendarLogicTest {
    @Test fun weekStartsOnMondayAcrossYearBoundary() {
        assertEquals(LocalDate.of(2025, 12, 29), CalendarLogic.weekStart(LocalDate.of(2026, 1, 4)))
    }
    @Test fun monthRangeIncludesLeapDay() {
        val range = CalendarLogic.range(LocalDate.of(2024, 2, 15), CalendarViewMode.MONTH)
        assertEquals(29L, range.last - range.first + 1)
    }
    @Test fun weekRangeHasExactlySevenDays() {
        val range = CalendarLogic.range(LocalDate.of(2026, 10, 16), CalendarViewMode.WEEK)
        assertEquals(7L, range.last - range.first + 1)
    }
    @Test fun navigationUsesMonthOrWeek() {
        val date = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), CalendarLogic.shift(date, CalendarViewMode.MONTH, 1))
        assertEquals(date.minusDays(7), CalendarLogic.shift(date, CalendarViewMode.WEEK, -1))
    }
    @Test fun firstTapSelectsAndSecondOpens() {
        assertFalse(CalendarLogic.opensOnTap(100, 101))
        assertTrue(CalendarLogic.opensOnTap(101, 101))
    }
    private fun task(id: Long, duration: Int, categoryId: Long? = null, day: Long = 100,
        start: Int = 600, deletedAt: Long? = null) = Task(id = id, title = "Task", date = day,
        durationMinutes = duration, categoryId = categoryId, startMinutes = start, deletedAt = deletedAt)
    @Test fun summaryGroupsCategoriesAndExcludesOtherDatesAndDeleted() {
        val summary = summarizeCalendarDay(100, listOf(task(1, 60, 1), task(2, 30, 1), task(3, 45),
            task(4, 100, day = 101), task(5, 100, deletedAt = 1)), PlannerSettings())
        assertEquals(135L, summary.busyMinutes)
        assertEquals(825L, summary.freeMinutes)
        assertEquals(CategoryTotal(1, 2, 90), summary.totals.first())
        assertEquals(3, summary.tasks.size)
    }
    @Test fun freeTimeMayBeNegativeAndDoesNotDeduplicateOverlaps() {
        val summary = summarizeCalendarDay(100, listOf(task(1, 600), task(2, 600)), PlannerSettings())
        assertEquals(-240L, summary.freeMinutes)
        assertTrue(summary.hasConflicts)
    }
    @Test fun conflictsIncludeCarryOverFromPreviousDay() {
        val summary = summarizeCalendarDay(100, listOf(task(1, 120, day = 99, start = 1410),
            task(2, 30, start = 60)), PlannerSettings())
        assertTrue(summary.hasConflicts)
        assertEquals(30L, summary.busyMinutes)
    }
    @Test fun emptySummaryStillHasWakingCapacity() {
        val summary = summarizeCalendarDay(100, emptyList(), PlannerSettings())
        assertEquals(960L, summary.freeMinutes)
        assertFalse(summary.hasConflicts)
    }
}
