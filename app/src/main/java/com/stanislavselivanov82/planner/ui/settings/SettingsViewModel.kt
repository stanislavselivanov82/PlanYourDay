package com.stanislavselivanov82.planner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.AppTheme
import com.stanislavselivanov82.planner.domain.model.PlannerSettings
import com.stanislavselivanov82.planner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository,
    private val scheduler: com.stanislavselivanov82.planner.service.ReminderScheduler) : ViewModel() {
    private val errorChannel = Channel<Unit>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    val settings = repository.settings.catch { error ->
        if (error is CancellationException) throw error
        errorChannel.send(Unit)
        emit(PlannerSettings())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlannerSettings())

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorChannel.send(Unit)
            }
        }
    }
    fun setSleep(start: Int, end: Int) = write { repository.setSleep(start, end) }
    fun setTheme(theme: AppTheme) = write { repository.setTheme(theme) }
    fun setTimeZone(zoneId: String?) = write { repository.setTimeZone(zoneId); scheduler.rescheduleAll() }
    fun setReminderSound(uri: String?) = write { repository.setReminderSound(uri) }
}
