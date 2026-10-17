package com.stanislavselivanov82.planner.data.repository

import androidx.room.withTransaction
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.entity.toDomain
import com.stanislavselivanov82.planner.data.local.db.entity.toEntity
import com.stanislavselivanov82.planner.domain.model.Note
import com.stanislavselivanov82.planner.domain.repository.NoteRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(private val database: PlannerDatabase) : NoteRepository {
    private val dao get() = database.noteDao()
    override fun getAll() = dao.getAll().map { rows -> rows.map { it.toDomain() } }
    override fun search(query: String) = getAll().map { notes ->
        val text = query.trim()
        notes.filter { it.title?.contains(text, ignoreCase = true) == true || it.content.contains(text, ignoreCase = true) }
    }
    override suspend fun getById(id: Long) = dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()
    override suspend fun save(note: Note): Long = database.withTransaction {
        require(note.deletedAt == null)
        val old = if (note.id == 0L) null else requireNotNull(dao.getById(note.id))
        require(old?.deletedAt == null)
        val now = System.currentTimeMillis()
        val row = note.copy(title = note.title?.trim()?.takeIf { it.isNotEmpty() }, createdAt = old?.createdAt ?: now, updatedAt = now).toEntity()
        if (old == null) dao.insert(row) else {
            check(dao.update(row) == 1)
            row.id
        }
    }
    override suspend fun delete(id: Long) = dao.softDelete(id, System.currentTimeMillis()) == 1
}
