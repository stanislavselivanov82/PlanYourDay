package com.stanislavselivanov82.planner.ui.calendar

import androidx.compose.foundation.layout.height
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.domain.model.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stanislavselivanov82.planner.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onOpenDay: (Long) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.calendar_error)
    LaunchedEffect(viewModel) { viewModel.errors.collect { snackbar.showSnackbar(errorText) } }
    val base = LocalViewConfiguration.current
    val configuration = remember(base) { object : ViewConfiguration by base { override val longPressTimeoutMillis = 500L } }
    CompositionLocalProvider(LocalViewConfiguration provides configuration) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            when {
                state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.failed -> Text(errorText, Modifier.padding(padding).padding(18.dp))
                else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        CalendarViewMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = state.settings.calendarViewMode == mode, enabled = !busy,
                                onClick = { viewModel.setMode(mode) }, shape = SegmentedButtonDefaults.itemShape(index, 2)) {
                                Text(stringResource(if (mode == CalendarViewMode.MONTH) R.string.calendar_month else R.string.calendar_week))
                            }
                        }
                    }
                    if (state.settings.calendarViewMode == CalendarViewMode.MONTH) {
                        MonthCalendar(state, viewModel::select, viewModel::navigate, viewModel::today, onOpenDay)
                    } else {
                        WeekCalendar(state, viewModel::select, viewModel::navigate, viewModel::today, onOpenDay)
                    }
                    CategoryLegend(state.categories)
                    CalendarSummary(state, busy, onOpenDay, viewModel::setDone, viewModel::tomorrow)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthCalendar(state: CalendarState, onSelect: (Long) -> Unit, onNavigate: (Int) -> Unit,
    onToday: () -> Unit, onOpenDay: (Long) -> Unit) {
    val today = LocalDate.now(state.settings.zoneId)
    val selectedEpochDay = state.selected
    val month = YearMonth.from(LocalDate.ofEpochDay(state.anchor))
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val locale = Locale.forLanguageTag("ru-RU")
    val monthLabel = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val leadingDays = month.atDay(1).dayOfWeek.value - 1
    val rowCount = (leadingDays + month.lengthOfMonth() + 6) / 7

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium)
        Text(monthLabel, style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onNavigate(-1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month),
                )
            }
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.today))
            }
            IconButton(onClick = { onNavigate(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rowCount) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(7) { column ->
                        val dayNumber = week * 7 + column - leadingDays + 1
                        if (dayNumber !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.atDay(dayNumber)
                            val selected = date == selectedDate
                            val shape = MaterialTheme.shapes.small
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        shape,
                                    )
                                    .then(
                                        if (date == today) Modifier.border(
                                            1.dp, MaterialTheme.colorScheme.primary, shape,
                                        ) else Modifier,
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (CalendarLogic.opensOnTap(selectedEpochDay, date.toEpochDay())) onOpenDay(selectedEpochDay)
                                            else onSelect(date.toEpochDay())
                                        },
                                        onLongClick = { onSelect(date.toEpochDay()); onOpenDay(date.toEpochDay()) },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                val dayTasks = state.tasks.filter { it.date == date.toEpochDay() }
                                CalendarDots(dayTasks, state.categories)
                                }
                                val completed = state.tasks.filter { it.date == date.toEpochDay() }
                                if (completed.isNotEmpty() && completed.all { it.isDone }) Text(
                                    stringResource(R.string.calendar_complete), modifier = Modifier.align(Alignment.TopEnd),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

    }
}
