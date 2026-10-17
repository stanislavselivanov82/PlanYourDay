package com.stanislavselivanov82.planner.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.ui.components.clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun calendarColor(categoryId: Long?, categories: List<Category>): Color =
    categories.firstOrNull { it.id == categoryId }?.let { Color(android.graphics.Color.parseColor(it.colorHex)) }
        ?: MaterialTheme.colorScheme.outline

@Composable
internal fun CalendarDots(tasks: List<Task>, categories: List<Category>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tasks.map { it.categoryId }.distinct().take(4).forEach { id ->
            Box(Modifier.size(4.dp).background(calendarColor(id, categories), CircleShape))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryLegend(categories: List<Category>) {
    Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(calendarColor(category.id, categories), CircleShape))
                Text(category.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WeekCalendar(state: CalendarState, onSelect: (Long) -> Unit, onNavigate: (Int) -> Unit,
    onToday: () -> Unit, onOpenDay: (Long) -> Unit) {
    val start = CalendarLogic.weekStart(LocalDate.ofEpochDay(state.anchor))
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))
    Text(stringResource(R.string.calendar_week_range, start.format(formatter), start.plusDays(6).format(formatter)),
        style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onNavigate(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_week)) }
        TextButton(onClick = onToday) { Text(stringResource(R.string.today)) }
        IconButton(onClick = { onNavigate(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_week)) }
    }
    repeat(7) { index ->
        val date = start.plusDays(index.toLong())
        val day = date.toEpochDay()
        val tasks = state.tasks.filter { it.date == day }.sortedBy { it.startMinutes }
        val selected = state.selected == day
        Card(Modifier.fillMaxWidth().then(if (date == LocalDate.now(state.settings.zoneId))
            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier)
            .combinedClickable(onClick = {
                if (CalendarLogic.opensOnTap(state.selected, day)) onOpenDay(day) else onSelect(day)
            }, onLongClick = { onSelect(day); onOpenDay(day) }),
            colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru"))),
                    style = MaterialTheme.typography.titleMedium)
                if (tasks.isEmpty()) Text(stringResource(R.string.calendar_no_tasks), style = MaterialTheme.typography.bodySmall)
                tasks.forEach { task ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(3.dp).height(20.dp).background(calendarColor(task.categoryId, state.categories)))
                        Text(stringResource(R.string.task_list_item, clock(task.startMinutes), task.title),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(stringResource(R.string.calendar_sleep_short, clock(state.settings.sleepStart), clock(state.settings.sleepEnd)),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun readableDuration(minutes: Long): String {
    val absolute = kotlin.math.abs(minutes)
    return stringResource(R.string.calendar_duration, if (minutes < 0) "−" else "", absolute / 60, absolute % 60)
}

@Composable
internal fun CalendarSummary(state: CalendarState, busy: Boolean, onOpenDay: (Long) -> Unit,
    onDone: (Task, Boolean) -> Unit, onTomorrow: (Task) -> Unit) {
    val summary = summarizeCalendarDay(state.selected, state.tasks, state.settings)
    val totals = summary.totals.sortedBy { total -> state.categories.indexOfFirst { it.id == total.categoryId }.let { if (it < 0) Int.MAX_VALUE else it } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(LocalDate.ofEpochDay(state.selected).format(DateTimeFormatter.ofPattern("d MMMM, EEE", Locale.forLanguageTag("ru"))),
                style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { onOpenDay(state.selected) }) { Text(stringResource(R.string.open_day)) }
            totals.forEach { total ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(calendarColor(total.categoryId, state.categories), CircleShape))
                    Text(stringResource(R.string.calendar_category_total,
                        state.categories.firstOrNull { it.id == total.categoryId }?.name ?: stringResource(R.string.category_none),
                        pluralStringResource(R.plurals.category_task_count, total.count, total.count), readableDuration(total.minutes)))
                }
            }
            Text(stringResource(R.string.calendar_sleep_total, clock(state.settings.sleepStart), clock(state.settings.sleepEnd),
                readableDuration(state.settings.sleepMinutes.toLong())))
            HorizontalDivider()
            Text(stringResource(R.string.day_capacity, readableDuration(summary.busyMinutes), readableDuration(summary.freeMinutes)))
            Text(stringResource(R.string.calendar_waking_total, readableDuration(state.settings.wakingMinutes.toLong())),
                style = MaterialTheme.typography.bodySmall)
            if (summary.hasConflicts) Text(stringResource(R.string.day_conflicts), color = MaterialTheme.colorScheme.error)
            // Normalize total planned load if overbooked; numeric free time retains its negative sign.
            val totalWeight = maxOf(1440L, summary.busyMinutes + state.settings.sleepMinutes).toFloat()
            Row(Modifier.fillMaxWidth().height(10.dp)) {
                totals.filter { it.minutes > 0 }.forEach { total ->
                    Box(Modifier.weight(total.minutes.toFloat() / totalWeight).fillMaxHeight()
                        .background(calendarColor(total.categoryId, state.categories)))
                }
                Box(Modifier.weight(state.settings.sleepMinutes.toFloat() / totalWeight).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary))
                val free = 1440L - summary.busyMinutes - state.settings.sleepMinutes
                if (free > 0) Box(Modifier.weight(free.toFloat() / totalWeight).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
            }
            if (summary.tasks.isEmpty()) Text(stringResource(R.string.calendar_no_tasks))
            summary.tasks.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.isDone, { onDone(task, it) }, enabled = !busy)
                    Column(Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.task_metadata, clock(task.startMinutes), clock((task.endMinutes % 1440).toInt()),
                            state.categories.firstOrNull { it.id == task.categoryId }?.name ?: stringResource(R.string.category_none)),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(enabled = !busy, onClick = { onTomorrow(task) }) { Text(stringResource(R.string.calendar_tomorrow)) }
                }
            }
        }
    }
}
