package com.stanislavselivanov82.planner.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
    val isSystem: Boolean,
)
