package com.stanislavselivanov82.planner.di

import com.stanislavselivanov82.planner.data.local.prefs.SettingsDataStore
import com.stanislavselivanov82.planner.data.repository.CategoryRepositoryImpl
import com.stanislavselivanov82.planner.data.repository.NoteRepositoryImpl
import com.stanislavselivanov82.planner.data.repository.TaskRepositoryImpl
import com.stanislavselivanov82.planner.domain.repository.CategoryRepository
import com.stanislavselivanov82.planner.domain.repository.NoteRepository
import com.stanislavselivanov82.planner.domain.repository.SettingsRepository
import com.stanislavselivanov82.planner.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton abstract fun bindTasks(implementation: TaskRepositoryImpl): TaskRepository
    @Binds @Singleton abstract fun bindCategories(implementation: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindNotes(implementation: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun bindSettings(implementation: SettingsDataStore): SettingsRepository
}
