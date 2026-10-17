package com.stanislavselivanov82.planner.domain.model

import java.time.LocalDate
import java.time.YearMonth

object CalendarLogic {
    fun weekStart(date: LocalDate): LocalDate = date.minusDays(date.dayOfWeek.value - 1L)
    fun range(anchor: LocalDate, mode: CalendarViewMode): LongRange = when (mode) {
        CalendarViewMode.MONTH -> YearMonth.from(anchor).let { it.atDay(1).toEpochDay()..it.atEndOfMonth().toEpochDay() }
        CalendarViewMode.WEEK -> weekStart(anchor).let { it.toEpochDay()..it.plusDays(6).toEpochDay() }
    }
    fun shift(anchor: LocalDate, mode: CalendarViewMode, direction: Int): LocalDate =
        if (mode == CalendarViewMode.MONTH) anchor.plusMonths(direction.toLong()) else anchor.plusWeeks(direction.toLong())
    fun opensOnTap(selected: Long, tapped: Long) = selected == tapped
}

data class CategoryTotal(val categoryId: Long?, val count: Int, val minutes: Long)
data class CalendarDaySummary(
    val tasks: List<Task>, val totals: List<CategoryTotal>, val busyMinutes: Long,
    val freeMinutes: Long, val hasConflicts: Boolean,
)

fun summarizeCalendarDay(day: Long, allTasks: List<Task>, settings: PlannerSettings): CalendarDaySummary {
    val active = allTasks.filter { it.deletedAt == null }
    val tasks = active.filter { it.date == day }.sortedWith(compareBy<Task> { it.startMinutes }.thenBy { it.id })
    val totals = tasks.groupBy { it.categoryId }.map { (id, list) ->
        CategoryTotal(id, list.size, list.sumOf { it.durationMinutes.toLong() })
    }
    val busy = tasks.sumOf { it.durationMinutes.toLong() }
    return CalendarDaySummary(tasks, totals, busy, settings.wakingMinutes - busy,
        DaySchedule.conflicts(active, day).isNotEmpty())
}
