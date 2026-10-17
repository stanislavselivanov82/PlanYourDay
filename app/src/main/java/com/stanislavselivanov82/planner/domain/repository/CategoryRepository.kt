package com.stanislavselivanov82.planner.domain.repository

import com.stanislavselivanov82.planner.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllOrdered(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
    suspend fun countTasksInCategory(id: Long): Int
    suspend fun save(category: Category): Long
    /** Null means missing or the final category; otherwise active task count. */
    suspend fun delete(id: Long): Int?
}
