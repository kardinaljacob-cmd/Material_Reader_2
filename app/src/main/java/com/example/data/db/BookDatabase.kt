package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.NoteEntity

@Database(
  entities = [BookEntity::class, BookmarkEntity::class, NoteEntity::class],
  version = 2,
  exportSchema = false,
)
abstract class BookDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao

  companion object {
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `bookId` INTEGER NOT NULL,
              `chapterIndex` INTEGER NOT NULL,
              `chapterTitle` TEXT NOT NULL,
              `pageIndex` INTEGER NOT NULL,
              `selectedText` TEXT NOT NULL,
              `noteText` TEXT NOT NULL,
              `colorIndex` INTEGER NOT NULL,
              `createdTimestamp` INTEGER NOT NULL
            )
            """.trimIndent()
          )
        }
      }

    @Volatile private var INSTANCE: BookDatabase? = null

    fun getInstance(context: Context): BookDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                BookDatabase::class.java,
                "listay_library.db",
              )
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { INSTANCE = it }
        }
  }
}
