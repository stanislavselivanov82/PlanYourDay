package com.stanislavselivanov82.planner.domain.model

data class Note(
    val id: Long = 0,
    val title: String? = null,
    val content: String = "",
    val colorTag: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null,
) {
    init {
        require(colorTag == null || colorTag in NoteColors.tags)
    }
}
