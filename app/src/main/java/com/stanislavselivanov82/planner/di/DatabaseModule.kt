package com.stanislavselivanov82.planner.di

import android.content.Context
import androidx.room.Room
import com.stanislavselivanov82.planner.data.local.db.DatabaseMigrations
import com.stanislavselivanov82.planner.data.local.db.InitialCategoriesCallback
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.dao.CategoryDao
import com.stanislavselivanov82.planner.data.local.db.dao.NoteDao
import com.stanislavselivanov82.planner.data.local.db.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        callback: InitialCategoriesCallback,
    ): PlannerDatabase = Room.databaseBuilder(context, PlannerDatabase::class.java, PlannerDatabase.NAME)
        .addCallback(callback)
        .addMigrations(DatabaseMigrations.MIGRATION_1_2)
        .build()

    @Provides
    fun provideTaskDao(database: PlannerDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideCategoryDao(database: PlannerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideNoteDao(database: PlannerDatabase): NoteDao = database.noteDao()
}
