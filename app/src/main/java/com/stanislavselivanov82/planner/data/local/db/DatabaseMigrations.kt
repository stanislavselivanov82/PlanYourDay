package com.stanislavselivanov82.planner.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    /** Version 1 uses the same tables, without deletedAt or its indexes. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_deletedAt ON tasks (deletedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_deletedAt ON notes (deletedAt)")
        }
    }
}
