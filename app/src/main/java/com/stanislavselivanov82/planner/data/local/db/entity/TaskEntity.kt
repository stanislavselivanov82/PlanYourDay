package com.stanislavselivanov82.planner.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("date"), Index("categoryId"), Index("deletedAt")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val date: Long,
    val startMinutes: Int,
    val durationMinutes: Int,
    val categoryId: Long?,
    val isDone: Boolean,
    val isPinned: Boolean,
    val reminderEnabled: Boolean,
    val reminderOffsetMinutes: Int?,
    val reminderType: Int,
    val isRecurring: Boolean,
    val recurrenceRule: String?,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "NULL") val deletedAt: Long? = null,
)
