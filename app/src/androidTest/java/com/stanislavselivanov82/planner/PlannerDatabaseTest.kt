package com.stanislavselivanov82.planner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stanislavselivanov82.planner.data.local.db.InitialCategoriesCallback
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.entity.CategoryEntity
import com.stanislavselivanov82.planner.data.local.db.entity.toEntity
import com.stanislavselivanov82.planner.domain.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerDatabaseTest {
    private lateinit var database: PlannerDatabase

    @Before fun openDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java)
            .addCallback(InitialCategoriesCallback(context))
            .build()
    }

    @After fun closeDatabase() { database.close() }

    @Test fun seedsFourCategoriesOnlyOnce() = runBlocking {
        val first = database.categoryDao().getAllOrderedOnce()
        assertEquals(listOf(1L, 2L, 3L, 4L), first.map { it.id })
        assertEquals(listOf("#ff5c5c", "#ffb84d", "#4dd68a", "#5db6ff"), first.map { it.colorHex })
        assertTrue(first.all { it.isSystem })
        assertEquals(first, database.categoryDao().getAllOrderedOnce())
    }

    @Test fun softDeleteHidesTaskAndRestoreReturnsIt() = runBlocking {
        val dao = database.taskDao()
        val id = dao.insert(Task(title = "Task", date = 20000, categoryId = 1).toEntity())
        assertEquals(1, dao.getByDate(20000).first().size)
        dao.softDelete(id, 123)
        assertTrue(dao.getByDate(20000).first().isEmpty())
        assertEquals(123L, dao.getById(id)?.deletedAt)
        dao.restore(id, 124)
        assertNull(dao.getById(id)?.deletedAt)
        assertEquals(1, dao.getByDate(20000).first().size)
    }

    @Test fun deletingCategoryDetachesTasksAndRejectsFinalCategory() = runBlocking {
        val categories = database.categoryDao()
        val tasks = database.taskDao()
        val id = tasks.insert(Task(title = "Task", categoryId = 1).toEntity())
        assertEquals(1, categories.delete(1))
        assertNull(tasks.getById(id)?.categoryId)
        categories.delete(2)
        categories.delete(3)
        assertNull(categories.delete(4))
        assertEquals(1, categories.count())
        val newId = categories.insert(CategoryEntity(name = "Custom", colorHex = "#5db6ff", sortOrder = 4, isSystem = false))
        assertTrue(newId > 4)
    }

    @Test fun purgeRemovesOnlyExpiredDeletedTasks() = runBlocking {
        val dao = database.taskDao()
        val oldId = dao.insert(Task(title = "Old").toEntity())
        val recentId = dao.insert(Task(title = "Recent").toEntity())
        val activeId = dao.insert(Task(title = "Active").toEntity())
        dao.softDelete(oldId, 10)
        dao.softDelete(recentId, 30)
        assertEquals(1, dao.purgeDeletedBefore(20))
        assertNull(dao.getById(oldId))
        assertEquals(30L, dao.getById(recentId)?.deletedAt)
        assertNull(dao.getById(activeId)?.deletedAt)
    }
}
