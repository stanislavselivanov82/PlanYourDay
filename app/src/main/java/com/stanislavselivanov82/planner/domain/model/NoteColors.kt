package com.stanislavselivanov82.planner.domain.model

object NoteColors {
    val tags = listOf("red", "orange", "yellow", "green", "blue", "purple")
}

fun filterNotes(notes: List<Note>, query: String, color: String?): List<Note> = notes
    .filter { it.deletedAt == null && (color == null || it.colorTag == color) &&
        (it.title.orEmpty().contains(query.trim(), ignoreCase = true) || it.content.contains(query.trim(), ignoreCase = true)) }
    .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }.thenByDescending { it.id })
