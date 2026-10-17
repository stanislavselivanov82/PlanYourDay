package com.stanislavselivanov82.planner.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stanislavselivanov82.planner.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {
    @Insert
    abstract suspend fun insert(task: TaskEntity): Long

    @Update
    abstract suspend fun update(task: TaskEntity): Int

    @Query("SELECT * FROM tasks WHERE date = :epochDay AND deletedAt IS NULL ORDER BY startMinutes, isPinned DESC, id")
    abstract fun getByDate(epochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :start AND :end AND deletedAt IS NULL ORDER BY date, startMinutes, id")
    abstract fun getInRange(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE date = :epochDay AND deletedAt IS NULL ORDER BY startMinutes, id")
    abstract suspend fun getByDateOnce(epochDay: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND (instr(lower(title), lower(:query)) > 0 OR instr(lower(COALESCE(description, '')), lower(:query)) > 0) ORDER BY date, startMinutes, id")
    abstract fun search(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND isDone = 0 AND deletedAt IS NULL ORDER BY date, startMinutes, id")
    abstract suspend fun getActiveReminders(): List<TaskEntity>

    @Query("UPDATE tasks SET isDone = :done, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NULL")
    abstract suspend fun setDone(id: Long, done: Boolean, timestamp: Long): Int

    @Query("UPDATE tasks SET date = :epochDay, startMinutes = :startMinutes, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NULL")
    abstract suspend fun move(id: Long, epochDay: Long, startMinutes: Int, timestamp: Long): Int

    @Query("UPDATE tasks SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NULL")
    abstract suspend fun softDelete(id: Long, timestamp: Long): Int

    @Transaction
    open suspend fun delete(task: TaskEntity): Int = softDelete(task.id, System.currentTimeMillis())

    @Query("UPDATE tasks SET deletedAt = NULL, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NOT NULL")
    abstract suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC, id DESC")
    abstract fun getDeleted(): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :timestamp")
    abstract suspend fun purgeDeletedBefore(timestamp: Long): Int
}
