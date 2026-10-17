package com.stanislavselivanov82.planner.ui.day

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.ui.components.*
import com.stanislavselivanov82.planner.ui.task.TaskEditSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(onBack: () -> Unit, viewModel: DayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(0L) }
    val snack = remember { SnackbarHostState() }
    val error = stringResource(R.string.task_error)
    LaunchedEffect(viewModel) { viewModel.errors.collect { snack.showSnackbar(error) } }
    val sleepAxis = DaySchedule.sleep(state.settings)
    val awakeAxis = DaySchedule.waking(state.settings)
    val asleep = DaySchedule.slices(state.tasks, state.date, sleepAxis)
    val awake = DaySchedule.slices(state.tasks, state.date, awakeAxis)
    val conflicts = DaySchedule.conflicts(state.tasks, state.date)
    val dateTasks = state.tasks.filter { it.date == state.date }
    val busyMinutes = dateTasks.sumOf { it.durationMinutes.toLong() }
    val freeMinutes = state.settings.wakingMinutes - busyMinutes
    val label = LocalDate.ofEpochDay(state.date).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru")))
    fun edit(task: Task) { editingId = task.id; editing = true }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(label.replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.day_counts, dateTasks.size, dateTasks.count { it.isDone }),
                        style = MaterialTheme.typography.bodySmall)
                }
            }, actions = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, stringResource(R.string.back)) } })
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingId = 0L; editing = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.task_new))
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            state.failed -> Text(error, Modifier.padding(padding).padding(18.dp))
            else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { viewModel.navigate(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_day))
                    }
                    TextButton(onClick = { viewModel.today() }) { Text(stringResource(R.string.today)) }
                    IconButton(onClick = { viewModel.navigate(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day))
                    }
                }
                Text(stringResource(R.string.day_capacity, durationText(busyMinutes), durationText(freeMinutes)))
                if (conflicts.isNotEmpty()) Text(stringResource(R.string.day_conflicts), color = MaterialTheme.colorScheme.error)
                // Sleep is a separate card ABOVE the waking timeline, never a timeline row.
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(stringResource(R.string.sleep_bar, clock(state.settings.sleepStart), clock(state.settings.sleepEnd),
                                durationText(state.settings.sleepMinutes.toLong()), if (expanded) "▴" else "▾"))
                        }
                        asleep.map { it.task }.distinctBy { it.id }.forEach { task ->
                            TextButton(onClick = { edit(task) }) {
                                Text(stringResource(R.string.sleep_task_warning, clock(task.startMinutes), task.title))
                            }
                        }
                        if (expanded) DayTimeline(sleepAxis, asleep, state.categories, conflicts, sleeping = true, onEdit = ::edit)
                    }
                }
                Text(stringResource(R.string.waking_title), style = MaterialTheme.typography.titleMedium)
                DayTimeline(awakeAxis, awake, state.categories, conflicts, onEdit = ::edit)
                HorizontalDivider()
                Text(stringResource(R.string.day_tasks), style = MaterialTheme.typography.titleMedium)
                if (state.tasks.isEmpty()) Text(stringResource(R.string.day_empty))
                // An accessible list also keeps every task editable when timeline blocks overlap.
                state.tasks.sortedWith(compareBy<Task> { it.date }.thenBy { it.startMinutes }).forEach { task ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(task.isDone, { viewModel.setDone(task, it) })
                        TextButton(onClick = { edit(task) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.task_list_item, clock(task.startMinutes), task.title))
                        }
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }
    val task = state.tasks.firstOrNull { it.id == editingId }
    if (editing && !state.loading && (editingId == 0L || task != null)) {
        key(editingId) {
            TaskEditSheet(task, state.date, state.categories, onDismiss = { editing = false })
        }
    }
}
