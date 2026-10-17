package com.stanislavselivanov82.planner.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isSystem: Boolean = false,
) {
    init {
        require(name.isNotBlank())
        require(Regex("^#[0-9a-fA-F]{6}$").matches(colorHex))
    }

    companion object {
        const val HIGH_SYSTEM_ID = 1L
    }
}
