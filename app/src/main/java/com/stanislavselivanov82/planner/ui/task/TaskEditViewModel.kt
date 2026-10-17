package com.stanislavselivanov82.planner.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TaskEditViewModel @Inject constructor(private val tasks: TaskRepository) : ViewModel() {
    private val busyValue = MutableStateFlow(false)
    val busy = busyValue.asStateFlow()
    private val eventsChannel = Channel<Boolean>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private fun write(action: suspend () -> Unit) {
        if (busyValue.value) return
        busyValue.value = true
        viewModelScope.launch {
            try { action(); eventsChannel.send(true) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                eventsChannel.send(false)
            } finally { busyValue.value = false }
        }
    }
    fun save(original: Task?, title: String, description: String, date: Long, start: Int,
        duration: Int, categoryId: Long?, reminder: Boolean, offset: Int, pinned: Boolean, done: Boolean) = write {
        tasks.save((original ?: Task(title = title)).copy(
            title = title.trim(), description = description.trim().takeIf { it.isNotEmpty() },
            date = date, startMinutes = start, durationMinutes = duration, categoryId = categoryId,
            reminderEnabled = reminder, reminderOffsetMinutes = if (reminder) offset else null,
            isPinned = pinned, isDone = done,
        ))
    }
    fun delete(task: Task) = write { check(tasks.delete(task.id)) }
}
