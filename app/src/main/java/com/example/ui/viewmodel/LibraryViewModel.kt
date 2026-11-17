package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookRepository
import com.example.data.db.BookDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryFilter(val label: String) {
  ALL("Все"),
  FAVORITES("Избранное"),
  EPUB("EPUB"),
  FB2("FB2"),
  PDF("PDF"),
  TXT("TXT / MD"),
}

data class LibraryUiState(
  val allBooks: List<BookEntity> = emptyList(),
  val filteredBooks: List<BookEntity> = emptyList(),
  val heroBook: BookEntity? = null,
  val searchQuery: String = "",
  val selectedFilter: LibraryFilter = LibraryFilter.ALL,
  val isImporting: Boolean = false,
  val isLoaded: Boolean = false,
  val message: String? = null,
  val totalBookmarks: Int = 0,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
  private val db = BookDatabase.getInstance(application)
  private val repository = BookRepository(application, db.bookDao())

  private val searchQuery = MutableStateFlow("")
  private val selectedFilter = MutableStateFlow(LibraryFilter.ALL)
  private val isImporting = MutableStateFlow(false)
  private val isLoaded = MutableStateFlow(false)
  private val message = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      repository.ensureSeeded()
      isLoaded.value = true
      withContext(Dispatchers.IO) {
        val books = repository.allBooks.first()
        val hero = books.maxByOrNull { it.lastReadTimestamp } ?: books.firstOrNull()
        if (hero != null) {
          runCatching { repository.loadParsedBook(hero) }
        }
      }
    }
  }

  val uiState: StateFlow<LibraryUiState> =
    combine(
        repository.allBooks,
        repository.totalBookmarkCount,
        searchQuery,
        selectedFilter,
        isImporting,
      ) { books, bookmarkCount, query, filter, importing ->
        val trimmed = query.trim().lowercase()
        val filtered =
          books.filter { book ->
            val matchesFilter =
              when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> book.isFavorite
                LibraryFilter.EPUB -> book.format == BookFormat.EPUB
                LibraryFilter.FB2 -> book.format == BookFormat.FB2
                LibraryFilter.PDF -> book.format == BookFormat.PDF
                LibraryFilter.TXT ->
                  book.format == BookFormat.TXT ||
                    book.format == BookFormat.MD ||
                    book.format == BookFormat.HTML
              }
            val matchesQuery =
              trimmed.isEmpty() ||
                book.title.lowercase().contains(trimmed) ||
                book.author.lowercase().contains(trimmed)
            matchesFilter && matchesQuery
          }
        val hero =
          books.maxByOrNull { it.lastReadTimestamp }
            ?: books.firstOrNull()

        LibraryUiState(
          allBooks = books,
          filteredBooks = filtered,
          heroBook = hero,
          searchQuery = query,
          selectedFilter = filter,
          isImporting = importing,
          isLoaded = isLoaded.value,
          message = message.value,
          totalBookmarks = bookmarkCount,
        )
      }
      .combine(message) { state, msg -> state.copy(message = msg) }
      .combine(isLoaded) { state, loaded -> state.copy(isLoaded = loaded) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(),
      )

  fun onSearchQueryChange(query: String) {
    searchQuery.value = query
  }

  fun onFilterSelected(filter: LibraryFilter) {
    selectedFilter.value = filter
  }

  fun toggleFavorite(bookId: Long) {
    viewModelScope.launch { repository.toggleFavorite(bookId) }
  }

  fun deleteBook(book: BookEntity) {
    viewModelScope.launch {
      repository.deleteBook(book)
      message.value = "Книга «${book.title}» удалена"
    }
  }

  fun importBook(uri: Uri, onImported: (BookEntity) -> Unit = {}) {
    viewModelScope.launch {
      isImporting.value = true
      val result = repository.importBookFromUri(uri)
      isImporting.value = false
      result
        .onSuccess { book ->
          message.value = "Добавлена книга «${book.title}» (${book.format.label})"
          onImported(book)
        }
        .onFailure { err ->
          message.value = err.localizedMessage ?: "Не удалось открыть файл"
        }
    }
  }

  fun clearMessage() {
    message.value = null
  }
}
