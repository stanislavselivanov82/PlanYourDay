package com.stanislavselivanov82.planner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.*
import java.util.Locale

fun clock(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", Math.floorMod(minutes, 1440) / 60, Math.floorMod(minutes, 60))
fun durationText(minutes: Long): String {
    val sign = if (minutes < 0) "−" else ""
    val absolute = kotlin.math.abs(minutes)
    return String.format(Locale.ROOT, "%s%d:%02d", sign, absolute / 60, absolute % 60)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTimeline(
    axis: List<DaySegment>,
    slices: List<TaskSlice>,
    categories: List<Category>,
    conflicts: Set<Long>,
    sleeping: Boolean = false,
    onEdit: (Task) -> Unit,
) {
    val minutes = axis.sumOf { it.duration }
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxWidth().height((minutes * .8f).dp).clip(RoundedCornerShape(8.dp))) {
        if (sleeping) Canvas(Modifier.matchParentSize()) {
            val step = 12.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(outline.copy(alpha = .22f), Offset(x, size.height), Offset(x + size.height, 0f), 2.dp.toPx())
                x += step
            }
        }
        axis.forEach { segment ->
            val ticks = buildList {
                add(segment.start)
                var tick = (segment.start / 60 + 1) * 60
                while (tick < segment.end) { add(tick); tick += 60 }
            }
            ticks.forEach { minute ->
                val y = (segment.offset + minute - segment.start) * .8f
                Row(Modifier.offset(y = y.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(clock(minute), Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider(Modifier.padding(top = 7.dp), color = outline)
                }
                if (minute == 0 && segment.offset > 0) {
                    Text(stringResource(R.string.midnight), Modifier.offset(x = 48.dp, y = y.dp)
                        .background(MaterialTheme.colorScheme.surface), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // Full-width blocks use real minute coordinates; simultaneous blocks remain marked as conflicts.
        slices.sortedByDescending { it.duration }.forEach { slice ->
            val task = slice.task
            val category = categories.firstOrNull { it.id == task.categoryId }
            val color = category?.let { Color(android.graphics.Color.parseColor(it.colorHex)) }
                ?: MaterialTheme.colorScheme.surfaceVariant
            val foreground = if (color.luminance() > .42f) Color(0xFF12151C) else Color.White
            val y = slice.offset * .8f
            val height = maxOf(26f, slice.duration * .8f).coerceAtMost(minutes * .8f - y)
            Box(Modifier.offset(x = 44.dp, y = y.dp).fillMaxWidth().padding(end = 44.dp)
                .height(height.dp).alpha(if (task.isDone) .45f else 1f)
                .clip(RoundedCornerShape(8.dp)).background(color)
                .combinedClickable(onClick = { onEdit(task) }, onLongClick = { onEdit(task) })) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text((if (task.isPinned) "★ " else "") + task.title +
                        (if (task.reminderEnabled) " 🔔" else "") + (if (task.id in conflicts) " ⚠" else ""),
                        color = foreground, fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    if (height >= 42) Text(
                        stringResource(R.string.task_metadata, clock(task.startMinutes), clock((task.endMinutes % 1440).toInt()),
                            category?.name ?: stringResource(R.string.category_none)),
                        color = foreground, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                if (task.id in conflicts) Box(Modifier.fillMaxHeight().width(4.dp)
                    .align(androidx.compose.ui.Alignment.CenterEnd).background(Color(0xFFFF8A3D)))
            }
        }
    }
}
