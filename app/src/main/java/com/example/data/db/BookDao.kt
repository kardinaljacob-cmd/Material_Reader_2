package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
  @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC, addedTimestamp DESC")
  fun observeAllBooks(): Flow<List<BookEntity>>

  @Query("SELECT * FROM books WHERE id = :bookId")
  fun observeBook(bookId: Long): Flow<BookEntity?>

  @Query("SELECT * FROM books WHERE id = :bookId")
  suspend fun getBookById(bookId: Long): BookEntity?

  @Query("SELECT COUNT(*) FROM books")
  suspend fun getBookCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBook(book: BookEntity): Long

  @Query(
    """
    UPDATE books 
    SET currentChapterIndex = :chapterIndex, 
        currentPageInChapter = :pageIndex, 
        progressPercent = :progressPercent, 
        lastReadTimestamp = :timestamp 
    WHERE id = :bookId
    """
  )
  suspend fun updateProgress(
    bookId: Long,
    chapterIndex: Int,
    pageIndex: Int,
    progressPercent: Float,
    timestamp: Long,
  )

  @Query("UPDATE books SET isFavorite = NOT isFavorite WHERE id = :bookId")
  suspend fun toggleFavorite(bookId: Long)

  @Query("DELETE FROM books WHERE id = :bookId")
  suspend fun deleteBookById(bookId: Long)

  @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdTimestamp DESC")
  fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

  @Query("SELECT COUNT(*) FROM bookmarks")
  fun observeTotalBookmarkCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: BookmarkEntity): Long

  @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
  suspend fun deleteBookmark(bookmarkId: Long)

  @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
  suspend fun deleteBookmarksForBook(bookId: Long)

  @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createdTimestamp DESC")
  fun observeNotes(bookId: Long): Flow<List<NoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity): Long

  @Query("UPDATE notes SET noteText = :noteText, colorIndex = :colorIndex WHERE id = :noteId")
  suspend fun updateNote(noteId: Long, noteText: String, colorIndex: Int)

  @Query("DELETE FROM notes WHERE id = :noteId")
  suspend fun deleteNote(noteId: Long)

  @Query("DELETE FROM notes WHERE bookId = :bookId")
  suspend fun deleteNotesForBook(bookId: Long)
}
