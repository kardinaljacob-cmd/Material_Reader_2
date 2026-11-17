package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import com.example.data.db.BookDao
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ParsedBook
import com.example.data.parser.BookParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookRepository(
  private val context: Context,
  private val bookDao: BookDao,
) {
  companion object {
    private val parsedCache = LruCache<String, ParsedBook>(10)
  }

  val allBooks: Flow<List<BookEntity>> = bookDao.observeAllBooks()
  val totalBookmarkCount: Flow<Int> = bookDao.observeTotalBookmarkCount()

  fun observeBook(bookId: Long): Flow<BookEntity?> = bookDao.observeBook(bookId)

  fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>> = bookDao.observeBookmarks(bookId)

  fun observeNotes(bookId: Long): Flow<List<NoteEntity>> = bookDao.observeNotes(bookId)

  suspend fun ensureSeeded() {
    BundledLibrarySeeder.seedIfEmpty(context, bookDao)
  }

  suspend fun importBookFromUri(uri: Uri): Result<BookEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val displayName = resolveDisplayName(uri) ?: "book_${System.currentTimeMillis()}.txt"
        val format = BookFormat.fromFileName(displayName)
        val fallbackTitle = displayName.substringBeforeLast('.').ifBlank { "Без названия" }

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val safeExtension =
          if (displayName.lowercase().endsWith(".fb2.zip")) "fb2.zip"
          else displayName.substringAfterLast('.', format.extensions.first())
        val destFile = File(booksDir, "${UUID.randomUUID()}.$safeExtension")

        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: error("Не удалось открыть выбранный файл")

        val parsed = BookParser.parseBook(destFile, format, fallbackTitle)

        var coverPath: String? = null
        if (parsed.coverBytes != null && parsed.coverBytes.isNotEmpty()) {
          val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
          val coverFile = File(coversDir, "${UUID.randomUUID()}.jpg")
          coverFile.writeBytes(parsed.coverBytes)
          coverPath = coverFile.absolutePath
        }

        val accentPalette =
          listOf(0xFF6D28D9L, 0xFF006D5BL, 0xFFC2410CL, 0xFF2F5E9AL, 0xFFB45309L)
        val accentHex = accentPalette[(parsed.title.hashCode() and 0x7FFFFFFF) % accentPalette.size]

        val entity =
          BookEntity(
            title = parsed.title.ifBlank { fallbackTitle },
            author = parsed.author,
            formatName = format.name,
            filePath = destFile.absolutePath,
            coverPath = coverPath,
            totalChapters = parsed.chapters.size.coerceAtLeast(1),
            currentChapterIndex = 0,
            currentPageInChapter = 0,
            progressPercent = 0f,
            lastReadTimestamp = System.currentTimeMillis(),
            addedTimestamp = System.currentTimeMillis(),
            isFavorite = false,
            accentColorHex = accentHex,
            fileSizeBytes = destFile.length(),
            description = parsed.description,
          )
        val insertedId = bookDao.insertBook(entity)
        val saved = entity.copy(id = insertedId)
        parsedCache.put("${saved.id}_${destFile.length()}_${destFile.absolutePath.hashCode()}", parsed)
        saved
      }
    }

  suspend fun loadParsedBook(book: BookEntity): ParsedBook {
    val file = File(book.filePath)
    val cacheKey = "${book.id}_${file.length()}_${book.filePath.hashCode()}"
    parsedCache.get(cacheKey)?.let { return it }
    val parsed = BookParser.parseBook(file, book.format, book.title)
    parsedCache.put(cacheKey, parsed)
    return parsed
  }

  suspend fun updateProgress(
    bookId: Long,
    chapterIndex: Int,
    pageIndex: Int,
    progressPercent: Float,
  ) {
    bookDao.updateProgress(
      bookId = bookId,
      chapterIndex = chapterIndex,
      pageIndex = pageIndex,
      progressPercent = progressPercent.coerceIn(0f, 1f),
      timestamp = System.currentTimeMillis(),
    )
  }

  suspend fun toggleFavorite(bookId: Long) {
    bookDao.toggleFavorite(bookId)
  }

  suspend fun deleteBook(book: BookEntity) =
    withContext(Dispatchers.IO) {
      bookDao.deleteBookmarksForBook(book.id)
      bookDao.deleteNotesForBook(book.id)
      bookDao.deleteBookById(book.id)
      runCatching { File(book.filePath).delete() }
      book.coverPath?.let { path -> runCatching { File(path).delete() } }
    }

  suspend fun addBookmark(
    bookId: Long,
    chapterIndex: Int,
    chapterTitle: String,
    pageIndex: Int,
    excerpt: String,
    colorIndex: Int = 0,
  ) {
    bookDao.insertBookmark(
      BookmarkEntity(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        pageIndex = pageIndex,
        excerpt = excerpt.take(160),
        colorIndex = colorIndex,
      )
    )
  }

  suspend fun deleteBookmark(bookmarkId: Long) {
    bookDao.deleteBookmark(bookmarkId)
  }

  suspend fun addNote(
    bookId: Long,
    chapterIndex: Int,
    chapterTitle: String,
    pageIndex: Int,
    selectedText: String,
    noteText: String,
    colorIndex: Int = 0,
  ) {
    val cleanedQuote = selectedText.trim().take(600)
    if (cleanedQuote.isEmpty()) return
    bookDao.insertNote(
      NoteEntity(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        pageIndex = pageIndex,
        selectedText = cleanedQuote,
        noteText = noteText.trim().take(1200),
        colorIndex = colorIndex,
      )
    )
  }

  suspend fun updateNote(noteId: Long, noteText: String, colorIndex: Int) {
    bookDao.updateNote(noteId, noteText.trim().take(1200), colorIndex)
  }

  suspend fun deleteNote(noteId: Long) {
    bookDao.deleteNote(noteId)
  }

  private fun resolveDisplayName(uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) return cursor.getString(idx)
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }
}
