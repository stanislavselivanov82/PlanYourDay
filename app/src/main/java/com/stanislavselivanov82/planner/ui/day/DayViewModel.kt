package com.stanislavselivanov82.planner.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

 data class DayState(
    val date: Long = LocalDate.now().toEpochDay(),
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: PlannerSettings = PlannerSettings(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DayViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val tasks: TaskRepository,
    categories: CategoryRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val date = savedState.getStateFlow("epochDay", LocalDate.now().toEpochDay())
    private val errorsChannel = Channel<Unit>(Channel.BUFFERED)
    val errors = errorsChannel.receiveAsFlow()
    val state = date.flatMapLatest { day ->
        combine(tasks.getInRange(Long.MIN_VALUE, day), categories.getAllOrdered(), settings.settings) { list, cats, prefs ->
            DayState(day, list.filter {
                (it.date - day) * 1440L + it.startMinutes + it.durationMinutes > 0
            }, cats, prefs, loading = false)
        }.onStart { emit(DayState(date = day)) }.catch { error ->
            if (error is CancellationException) throw error
            emit(DayState(date = day, loading = false, failed = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayState(date = date.value))

    fun navigate(delta: Int) { savedState["epochDay"] = date.value + delta }
    fun today() { savedState["epochDay"] = LocalDate.now(state.value.settings.zoneId).toEpochDay() }
    fun setDone(task: Task, done: Boolean) {
        viewModelScope.launch {
            try { tasks.setDone(task.id, done) } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorsChannel.send(Unit)
            }
        }
    }
}
