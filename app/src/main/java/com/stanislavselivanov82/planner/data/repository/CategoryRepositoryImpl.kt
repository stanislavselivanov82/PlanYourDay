package com.stanislavselivanov82.planner.data.repository

import androidx.room.withTransaction
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.entity.toDomain
import com.stanislavselivanov82.planner.data.local.db.entity.toEntity
import com.stanislavselivanov82.planner.domain.model.Category
import com.stanislavselivanov82.planner.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(private val database: PlannerDatabase,
    private val scheduler: com.stanislavselivanov82.planner.service.ReminderScheduler) : CategoryRepository {
    private val dao get() = database.categoryDao()
    override fun getAllOrdered() = dao.getAllOrdered().map { rows -> rows.map { it.toDomain() } }
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun countTasksInCategory(id: Long) = dao.countTasksInCategory(id)
    override suspend fun save(category: Category): Long = database.withTransaction {
        if (category.id == 0L) {
            dao.insert(category.copy(name = category.name.trim(), sortOrder = dao.nextSortOrder(), isSystem = false).toEntity())
        } else {
            val old = requireNotNull(dao.getById(category.id))
            check(dao.update(category.copy(name = category.name.trim(), sortOrder = old.sortOrder, isSystem = old.isSystem).toEntity()) == 1)
            category.id
        }
    }
    override suspend fun delete(id: Long): Int? {
        val count = dao.delete(id)
        if (count != null) scheduler.rescheduleAll()
        return count
    }
}
