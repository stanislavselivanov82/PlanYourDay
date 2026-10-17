package com.stanislavselivanov82.planner.domain.repository

import com.stanislavselivanov82.planner.domain.model.Task
import kotlinx.coroutines.flow.Flow

/** Persistence only; reminder scheduling belongs to the task use cases. */
interface TaskRepository {
    fun getByDate(epochDay: Long): Flow<List<Task>>
    fun getInRange(start: Long, end: Long): Flow<List<Task>>
    fun search(query: String): Flow<List<Task>>
    suspend fun getById(id: Long): Task?
    suspend fun getByDateOnce(epochDay: Long): List<Task>
    suspend fun getActiveReminders(): List<Task>
    suspend fun save(task: Task): Long
    suspend fun setDone(id: Long, done: Boolean): Boolean
    suspend fun move(id: Long, epochDay: Long, startMinutes: Int): Boolean
    suspend fun delete(id: Long): Boolean
}
