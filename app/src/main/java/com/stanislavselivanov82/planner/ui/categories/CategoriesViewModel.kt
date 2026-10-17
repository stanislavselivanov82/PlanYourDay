package com.stanislavselivanov82.planner.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stanislavselivanov82.planner.domain.model.Category
import com.stanislavselivanov82.planner.domain.repository.CategoryRepository
import com.stanislavselivanov82.planner.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(val category: Category, val taskCount: Int)
data class CategoriesState(val rows: List<CategoryRow> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)
sealed interface CategoryEvent {
    data object Saved : CategoryEvent
    data class Deleted(val name: String, val count: Int) : CategoryEvent
    data object DeleteRejected : CategoryEvent
    data object Error : CategoryEvent
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categories: CategoryRepository,
    tasks: TaskRepository,
) : ViewModel() {
    private val eventChannel = Channel<CategoryEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private val busyState = MutableStateFlow(false)
    val busy = busyState
    val state = combine(categories.getAllOrdered(), tasks.getInRange(Long.MIN_VALUE, Long.MAX_VALUE)) { list, taskList ->
        val counts = taskList.groupingBy { it.categoryId }.eachCount()
        CategoriesState(rows = list.map { CategoryRow(it, counts[it.id] ?: 0) }, loading = false)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(CategoriesState(loading = false, failed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesState())

    fun save(id: Long, name: String, colorHex: String) {
        if (busyState.value || name.isBlank()) return
        busyState.value = true
        viewModelScope.launch {
            try {
                val old = if (id == 0L) null else requireNotNull(categories.getById(id))
                categories.save((old ?: Category(name = name.trim(), colorHex = colorHex)).copy(name = name.trim(), colorHex = colorHex))
                eventChannel.send(CategoryEvent.Saved)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                eventChannel.send(CategoryEvent.Error)
            } finally { busyState.value = false }
        }
    }

    fun delete(id: Long) {
        if (busyState.value) return
        busyState.value = true
        viewModelScope.launch {
            try {
                val category = categories.getById(id)
                val count = categories.delete(id)
                eventChannel.send(if (category != null && count != null) CategoryEvent.Deleted(category.name, count)
                    else CategoryEvent.DeleteRejected)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                eventChannel.send(CategoryEvent.Error)
            } finally { busyState.value = false }
        }
    }
}
