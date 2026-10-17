package com.stanislavselivanov82.planner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stanislavselivanov82.planner.data.local.db.dao.CategoryDao
import com.stanislavselivanov82.planner.data.local.db.dao.NoteDao
import com.stanislavselivanov82.planner.data.local.db.dao.TaskDao
import com.stanislavselivanov82.planner.data.local.db.entity.CategoryEntity
import com.stanislavselivanov82.planner.data.local.db.entity.NoteEntity
import com.stanislavselivanov82.planner.data.local.db.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, CategoryEntity::class, NoteEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "planner.db"
    }
}
