package com.stanislavselivanov82.planner.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes", indices = [Index("deletedAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val content: String,
    val colorTag: String?,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "NULL") val deletedAt: Long? = null,
)
