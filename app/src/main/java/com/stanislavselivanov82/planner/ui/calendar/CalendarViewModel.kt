package com.stanislavselivanov82.planner.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

 data class CalendarState(
    val selected: Long = LocalDate.now().toEpochDay(),
    val anchor: Long = selected,
    val settings: PlannerSettings = PlannerSettings(),
    val categories: List<Category> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val taskRepository: TaskRepository,
    categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val initial = LocalDate.now().toEpochDay()
    private val selected = savedState.getStateFlow("calendarSelected", initial)
    private val anchor = savedState.getStateFlow("calendarAnchor", initial)
    private val errorsChannel = Channel<Unit>(Channel.BUFFERED)
    val errors = errorsChannel.receiveAsFlow()
    private val busyValue = MutableStateFlow(false)
    val busy = busyValue.asStateFlow()
    val state = combine(selected, anchor, settingsRepository.settings, categoryRepository.getAllOrdered(),
        taskRepository.getInRange(Long.MIN_VALUE, Long.MAX_VALUE)) { day, visible, prefs, categories, tasks ->
        CalendarState(day, visible, prefs, categories, tasks, loading = false)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(CalendarState(selected.value, anchor.value, loading = false, failed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState(initial))

    fun select(day: Long) { savedState["calendarSelected"] = day }
    fun navigate(direction: Int) {
        savedState["calendarAnchor"] = CalendarLogic.shift(LocalDate.ofEpochDay(anchor.value),
            state.value.settings.calendarViewMode, direction).toEpochDay()
    }
    fun today() {
        val day = LocalDate.now(state.value.settings.zoneId).toEpochDay()
        savedState["calendarSelected"] = day
        savedState["calendarAnchor"] = day
    }
    fun setMode(mode: CalendarViewMode) = write {
        savedState["calendarAnchor"] = selected.value
        settingsRepository.setCalendarViewMode(mode)
    }
    fun setDone(task: Task, done: Boolean) = write { check(taskRepository.setDone(task.id, done)) }
    fun tomorrow(task: Task) = write { check(taskRepository.move(task.id, task.date + 1, task.startMinutes)) }
    private fun write(action: suspend () -> Unit) {
        if (busyValue.value) return
        busyValue.value = true
        viewModelScope.launch {
            try { action() } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorsChannel.send(Unit)
            } finally { busyValue.value = false }
        }
    }
}
