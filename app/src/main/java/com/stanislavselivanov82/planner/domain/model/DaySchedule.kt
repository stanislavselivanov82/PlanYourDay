package com.stanislavselivanov82.planner.domain.model

/** Half-open civil-day interval, with an offset on a circular display axis. */
data class DaySegment(val start: Int, val end: Int, val offset: Int) {
    val duration: Int get() = end - start
}

data class TaskSlice(val task: Task, val start: Int, val end: Int, val offset: Int) {
    val duration: Int get() = end - start
}

object DaySchedule {
    fun segments(start: Int, end: Int): List<DaySegment> {
        require(start in 0..1439 && end in 0..1439 && start != end)
        return if (start < end) listOf(DaySegment(start, end, 0))
        else buildList {
            add(DaySegment(start, 1440, 0))
            if (end > 0) add(DaySegment(0, end, 1440 - start))
        }
    }

    fun sleep(settings: PlannerSettings) = segments(settings.sleepStart, settings.sleepEnd)
    fun waking(settings: PlannerSettings) = segments(settings.sleepEnd, settings.sleepStart)

    /** Clip real task intervals to the chosen civil day, then project onto the axis. */
    fun slices(tasks: List<Task>, epochDay: Long, axis: List<DaySegment>): List<TaskSlice> = buildList {
        tasks.forEach { task ->
            val start = (task.date - epochDay) * 1440L + task.startMinutes
            val end = start + task.durationMinutes
            axis.forEach { segment ->
                val from = maxOf(start, segment.start.toLong())
                val to = minOf(end, segment.end.toLong())
                if (from < to) add(TaskSlice(task, from.toInt(), to.toInt(), segment.offset + from.toInt() - segment.start))
            }
        }
    }

    fun conflicts(tasks: List<Task>, epochDay: Long): Set<Long> {
        val intervals = tasks.mapNotNull { task ->
            val start = (task.date - epochDay) * 1440L + task.startMinutes
            val end = start + task.durationMinutes
            if (start < 1440 && end > 0) Triple(task.id, maxOf(0L, start), minOf(1440L, end)) else null
        }
        return buildSet {
            intervals.forEachIndexed { index, a ->
                intervals.drop(index + 1).forEach { b ->
                    if (a.second < b.third && b.second < a.third) { add(a.first); add(b.first) }
                }
            }
        }
    }
}
