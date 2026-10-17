package com.stanislavselivanov82.planner.domain.repository

import com.stanislavselivanov82.planner.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAll(): Flow<List<Note>>
    fun search(query: String): Flow<List<Note>>
    suspend fun getById(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long): Boolean
}
