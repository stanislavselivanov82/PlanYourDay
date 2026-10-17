package com.stanislavselivanov82.planner.data.local.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stanislavselivanov82.planner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class InitialCategoriesCallback @Inject constructor(
    @ApplicationContext private val context: Context,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val categories = listOf(
            Triple(R.string.category_high, "#ff5c5c", 0),
            Triple(R.string.category_medium, "#ffb84d", 1),
            Triple(R.string.category_low, "#4dd68a", 2),
            Triple(R.string.category_development, "#5db6ff", 3),
        )
        categories.forEach { (nameRes, color, order) ->
            db.execSQL(
                "INSERT INTO categories (id, name, colorHex, sortOrder, isSystem) VALUES (?, ?, ?, ?, 1)",
                arrayOf<Any>(order.toLong() + 1L, context.getString(nameRes), color, order),
            )
        }
    }
}
