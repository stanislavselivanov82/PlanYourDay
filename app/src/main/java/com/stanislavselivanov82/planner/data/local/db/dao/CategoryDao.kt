package com.stanislavselivanov82.planner.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stanislavselivanov82.planner.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {
    @Insert
    abstract suspend fun insert(category: CategoryEntity): Long

    @Update
    abstract suspend fun update(category: CategoryEntity): Int

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    abstract fun getAllOrdered(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    abstract suspend fun getAllOrderedOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE categoryId = :id AND deletedAt IS NULL")
    abstract suspend fun countTasksInCategory(id: Long): Int

    @Query("SELECT COUNT(*) FROM categories")
    abstract suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM categories")
    abstract suspend fun nextSortOrder(): Int

    @Query("UPDATE tasks SET categoryId = NULL, updatedAt = :timestamp WHERE categoryId = :id")
    protected abstract suspend fun detachTasks(id: Long, timestamp: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    protected abstract suspend fun deleteById(id: Long): Int

    /** Returns active task count, or null when missing or the final category. */
    @Transaction
    open suspend fun delete(id: Long): Int? {
        if (getById(id) == null || count() <= 1) return null
        val affectedTasks = countTasksInCategory(id)
        detachTasks(id, System.currentTimeMillis())
        deleteById(id)
        return affectedTasks
    }
}
