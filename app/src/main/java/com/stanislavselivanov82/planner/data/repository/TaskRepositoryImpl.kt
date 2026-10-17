package com.stanislavselivanov82.planner.data.repository

import androidx.room.withTransaction
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.entity.toDomain
import com.stanislavselivanov82.planner.data.local.db.entity.toEntity
import com.stanislavselivanov82.planner.domain.model.Category
import com.stanislavselivanov82.planner.domain.model.Task
import com.stanislavselivanov82.planner.domain.repository.TaskRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(private val database: PlannerDatabase,
    private val scheduler: com.stanislavselivanov82.planner.service.ReminderScheduler) : TaskRepository {
    private val dao get() = database.taskDao()
    override fun getByDate(epochDay: Long) = dao.getByDate(epochDay).map { rows -> rows.map { it.toDomain() } }
    override fun getInRange(start: Long, end: Long) = dao.getInRange(start, end).map { rows -> rows.map { it.toDomain() } }
    // Kotlin filtering supports Cyrillic case folding, unlike SQLite's built-in lower().
    override fun search(query: String) = getInRange(Long.MIN_VALUE, Long.MAX_VALUE).map { tasks ->
        val text = query.trim()
        tasks.filter { it.title.contains(text, ignoreCase = true) || it.description?.contains(text, ignoreCase = true) == true }
    }
    override suspend fun getById(id: Long) = dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()
    override suspend fun getByDateOnce(epochDay: Long) = dao.getByDateOnce(epochDay).map { it.toDomain() }
    override suspend fun getActiveReminders() = dao.getActiveReminders().map { it.toDomain() }

    override suspend fun save(task: Task): Long {
        val id = database.withTransaction {
        require(task.deletedAt == null)
        val old = if (task.id == 0L) null else requireNotNull(dao.getById(task.id))
        require(old?.deletedAt == null)
        val category = task.categoryId?.let { requireNotNull(database.categoryDao().getById(it)) }
        val requiredReminder = category?.id == Category.HIGH_SYSTEM_ID && category.isSystem
        val now = System.currentTimeMillis()
        val row = task.copy(
            title = task.title.trim(),
            reminderEnabled = task.reminderEnabled || requiredReminder,
            reminderOffsetMinutes = if (task.reminderEnabled || requiredReminder) task.reminderOffsetMinutes ?: 10 else null,
            isRecurring = old?.isRecurring ?: false,
            recurrenceRule = old?.recurrenceRule,
            createdAt = old?.createdAt ?: now,
            updatedAt = now,
        ).toEntity()
        if (old == null) dao.insert(row) else {
            check(dao.update(row) == 1)
            row.id
        }
        }
        scheduler.refresh(id)
        return id
    }
    override suspend fun setDone(id: Long, done: Boolean): Boolean {
        val changed = dao.setDone(id, done, System.currentTimeMillis()) == 1
        if (changed) scheduler.refresh(id)
        return changed
    }
    override suspend fun move(id: Long, epochDay: Long, startMinutes: Int): Boolean {
        require(startMinutes in 0..1439)
        val changed = dao.move(id, epochDay, startMinutes, System.currentTimeMillis()) == 1
        if (changed) scheduler.refresh(id)
        return changed
    }
    override suspend fun delete(id: Long): Boolean {
        val changed = dao.softDelete(id, System.currentTimeMillis()) == 1
        if (changed) scheduler.refresh(id)
        return changed
    }
}
