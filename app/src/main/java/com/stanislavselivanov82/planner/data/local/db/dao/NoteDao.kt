package com.stanislavselivanov82.planner.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stanislavselivanov82.planner.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {
    @Insert
    abstract suspend fun insert(note: NoteEntity): Long

    @Update
    abstract suspend fun update(note: NoteEntity): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC, id DESC")
    abstract fun getAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (instr(lower(COALESCE(title, '')), lower(:query)) > 0 OR instr(lower(content), lower(:query)) > 0) ORDER BY isPinned DESC, updatedAt DESC, id DESC")
    abstract fun search(query: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NULL")
    abstract suspend fun softDelete(id: Long, timestamp: Long): Int

    @Transaction
    open suspend fun delete(note: NoteEntity): Int = softDelete(note.id, System.currentTimeMillis())

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :timestamp WHERE id = :id AND deletedAt IS NOT NULL")
    abstract suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC, id DESC")
    abstract fun getDeleted(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :timestamp")
    abstract suspend fun purgeDeletedBefore(timestamp: Long): Int
}
