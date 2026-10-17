package com.stanislavselivanov82.planner.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.ui.components.clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    original: Task?, date: Long, categories: List<Category>, onDismiss: () -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf(original?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(original?.description.orEmpty()) }
    var epochDay by rememberSaveable { mutableLongStateOf(original?.date ?: date) }
    var start by rememberSaveable { mutableIntStateOf(original?.startMinutes ?: 540) }
    var duration by rememberSaveable { mutableStateOf((original?.durationMinutes ?: 60).toString()) }
    var categoryId by rememberSaveable { mutableStateOf(original?.categoryId) }
    var reminder by rememberSaveable { mutableStateOf(original?.reminderEnabled ?: false) }
    var offset by rememberSaveable { mutableIntStateOf(original?.reminderOffsetMinutes ?: 10) }
    var pinned by rememberSaveable { mutableStateOf(original?.isPinned ?: false) }
    var done by rememberSaveable { mutableStateOf(original?.isDone ?: false) }
    var error by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val required = categories.any { it.id == categoryId && it.id == Category.HIGH_SYSTEM_ID && it.isSystem }
    val effectiveCategory = categoryId?.takeIf { id -> categories.any { it.id == id } }
    val minutes = duration.toIntOrNull()
    LaunchedEffect(viewModel) { viewModel.events.collect { success -> if (success) onDismiss() else error = true } }
    fun chooseDate() {
        val current = LocalDate.ofEpochDay(epochDay)
        DatePickerDialog(context, { _, year, month, day -> epochDay = LocalDate.of(year, month + 1, day).toEpochDay() },
            current.year, current.monthValue - 1, current.dayOfMonth).show()
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (original == null) R.string.task_new else R.string.task_edit), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.task_title)) },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.task_description)) },
                minLines = 2, maxLines = 5, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !busy, onClick = { chooseDate() }) {
                    Text(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))))
                }
                TextButton(enabled = !busy, onClick = {
                    TimePickerDialog(context, { _, h, m -> start = h * 60 + m }, start / 60, start % 60, true).show()
                }) { Text(stringResource(R.string.task_start_value, clock(start))) }
            }
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.task_duration)) }, singleLine = true, enabled = !busy,
                isError = minutes == null || minutes <= 0, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(30, 60, 90, 120).forEach { value ->
                    FilterChip(selected = minutes == value, enabled = !busy, onClick = { duration = value.toString() },
                        label = { Text(stringResource(R.string.minutes_value, value)) })
                }
            }
            Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = effectiveCategory == null, enabled = !busy, onClick = { categoryId = null },
                    label = { Text(stringResource(R.string.category_none)) })
                categories.forEach { category ->
                    FilterChip(selected = effectiveCategory == category.id, enabled = !busy,
                        onClick = { categoryId = category.id },
                        leadingIcon = { Box(Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(category.colorHex)), CircleShape)) },
                        label = { Text(category.name) })
                }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = reminder || required, onCheckedChange = { reminder = it }, enabled = !busy && !required)
                Text(stringResource(if (required) R.string.reminder_required else R.string.task_reminder))
            }
            if (reminder || required) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15, 30, 60).forEach { value ->
                    FilterChip(selected = offset == value, enabled = !busy, onClick = { offset = value },
                        label = { Text(stringResource(R.string.reminder_offset, value)) })
                }
            }
            if (reminder || required) {
                Text(stringResource(R.string.reminder_access_hint), style = MaterialTheme.typography.bodySmall)
                com.stanislavselivanov82.planner.ui.components.ReminderPermissions()
            }
            Text(stringResource(R.string.repeat_later), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(pinned, { pinned = it }, enabled = !busy); Text(stringResource(R.string.task_pinned))
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(done, { done = it }, enabled = !busy); Text(stringResource(R.string.task_done))
            }
            if (error) Text(stringResource(R.string.task_error), color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (original != null) TextButton(enabled = !busy, onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                TextButton(enabled = !busy, onClick = { chooseDate() }) { Text(stringResource(R.string.task_move)) }
                Button(enabled = !busy && title.isNotBlank() && minutes != null && minutes > 0, onClick = {
                    viewModel.save(original, title, description, epochDay, start, requireNotNull(minutes), effectiveCategory,
                        reminder || required, offset, pinned, done)
                }) { Text(stringResource(R.string.save)) }
            }
        }
    }
    if (confirmDelete && original != null) AlertDialog(
        onDismissRequest = { if (!busy) confirmDelete = false }, title = { Text(stringResource(R.string.task_delete_confirm)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.delete(original) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
