package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookRepository
import com.example.data.db.BookDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import com.example.data.model.NoteEntity
import com.example.data.model.PageFlipAnimation
import com.example.data.model.ParsedBook
import com.example.data.parser.PdfBookHelper
import com.example.data.prefs.ReaderPreferences
import com.example.data.prefs.ReaderPreferencesRepository
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReaderLoadState {
  data object Loading : ReaderLoadState
  data class Ready(
    val book: BookEntity,
    val parsedBook: ParsedBook,
  ) : ReaderLoadState
  data class Error(val message: String) : ReaderLoadState
}

data class ReaderScreenState(
  val loadState: ReaderLoadState = ReaderLoadState.Loading,
  val preferences: ReaderPreferences = ReaderPreferences(),
  val bookmarks: List<BookmarkEntity> = emptyList(),
  val notes: List<NoteEntity> = emptyList(),
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
  private val db = BookDatabase.getInstance(application)
  private val repository = BookRepository(application, db.bookDao())
  private val prefsRepo = ReaderPreferencesRepository(application)

  private val activeBookId = MutableStateFlow<Long?>(null)
  private val loadState = MutableStateFlow<ReaderLoadState>(ReaderLoadState.Loading)
  private val bookmarksFlow = MutableStateFlow<List<BookmarkEntity>>(emptyList())
  private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
  private var observeBookSubJobs: Job? = null

  // Immediate in-memory preference overlay for zero-lag real-time slider updates
  private val livePrefsOverride = MutableStateFlow<ReaderPreferences?>(null)
  private val persistJobs = mutableMapOf<String, Job>()

  val uiState: StateFlow<ReaderScreenState> =
    combine(
        loadState,
        prefsRepo.preferencesFlow,
        livePrefsOverride,
        bookmarksFlow,
        notesFlow,
      ) { load, persistedPrefs, overridePrefs, marks, notes ->
        ReaderScreenState(
          loadState = load,
          preferences = overridePrefs ?: persistedPrefs,
          bookmarks = marks,
          notes = notes,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderScreenState(),
      )

  fun loadBook(bookId: Long) {
    if (activeBookId.value == bookId && loadState.value is ReaderLoadState.Ready) return
    activeBookId.value = bookId
    loadState.value = ReaderLoadState.Loading

    observeBookSubJobs?.cancel()
    observeBookSubJobs =
      viewModelScope.launch {
        launch {
          repository.observeBookmarks(bookId).collect { list ->
            bookmarksFlow.value = list
          }
        }
        launch {
          repository.observeNotes(bookId).collect { list ->
            notesFlow.value = list
          }
        }
        val book = withContext(Dispatchers.IO) { db.bookDao().getBookById(bookId) }
        if (book == null) {
          loadState.value = ReaderLoadState.Error("Книга не найдена в библиотеке")
          return@launch
        }
        runCatching { withContext(Dispatchers.IO) { repository.loadParsedBook(book) } }
          .onSuccess { parsed ->
            loadState.value = ReaderLoadState.Ready(book = book, parsedBook = parsed)
          }
          .onFailure { err ->
            loadState.value =
              ReaderLoadState.Error(err.localizedMessage ?: "Ошибка чтения файла")
          }
      }
  }

  suspend fun loadPdfPage(filePath: String, pageIndex: Int, widthPx: Int): Bitmap? =
    PdfBookHelper.renderPage(filePath, pageIndex, widthPx)

  fun saveReadingPosition(
    bookId: Long,
    chapterIndex: Int,
    pageInChapter: Int,
    progressPercent: Float,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateProgress(
        bookId = bookId,
        chapterIndex = chapterIndex,
        pageIndex = pageInChapter,
        progressPercent = progressPercent,
      )
    }
  }

  fun toggleBookmark(
    bookId: Long,
    chapterIndex: Int,
    chapterTitle: String,
    pageIndex: Int,
    excerpt: String,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val existing =
        bookmarksFlow.value.firstOrNull {
          it.chapterIndex == chapterIndex && it.pageIndex == pageIndex
        }
      if (existing != null) {
        repository.deleteBookmark(existing.id)
      } else {
        repository.addBookmark(
          bookId = bookId,
          chapterIndex = chapterIndex,
          chapterTitle = chapterTitle,
          pageIndex = pageIndex,
          excerpt = excerpt,
          colorIndex = (chapterIndex % 4),
        )
      }
    }
  }

  fun deleteBookmark(bookmarkId: Long) {
    viewModelScope.launch(Dispatchers.IO) { repository.deleteBookmark(bookmarkId) }
  }

  fun addBookmarkWithExcerpt(
    bookId: Long,
    chapterIndex: Int,
    chapterTitle: String,
    pageIndex: Int,
    excerpt: String,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.addBookmark(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        pageIndex = pageIndex,
        excerpt = excerpt,
        colorIndex = (chapterIndex % 4),
      )
    }
  }

  fun addNote(
    bookId: Long,
    chapterIndex: Int,
    chapterTitle: String,
    pageIndex: Int,
    selectedText: String,
    noteText: String,
    colorIndex: Int,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.addNote(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        pageIndex = pageIndex,
        selectedText = selectedText,
        noteText = noteText,
        colorIndex = colorIndex,
      )
    }
  }

  fun updateNote(noteId: Long, noteText: String, colorIndex: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateNote(noteId = noteId, noteText = noteText, colorIndex = colorIndex)
    }
  }

  fun deleteNote(noteId: Long) {
    viewModelScope.launch(Dispatchers.IO) { repository.deleteNote(noteId) }
  }

  private inline fun updateLiveAndPersist(
    key: String,
    debounceMs: Long,
    transform: (ReaderPreferences) -> ReaderPreferences,
    crossinline persist: suspend () -> Unit,
  ) {
    val current = livePrefsOverride.value ?: uiState.value.preferences
    livePrefsOverride.value = transform(current)
    persistJobs[key]?.cancel()
    persistJobs[key] =
      viewModelScope.launch(Dispatchers.IO) {
        if (debounceMs > 0L) delay(debounceMs)
        persist()
      }
  }

  fun setFontOption(option: ReaderFontOption) {
    updateLiveAndPersist("font", 0L, { it.copy(fontOption = option) }) {
      prefsRepo.updateFont(option)
    }
  }

  fun setFontSize(sizeSp: Float) {
    val clamped = sizeSp.coerceIn(14f, 32f)
    updateLiveAndPersist("fontSize", 180L, { it.copy(fontSizeSp = clamped) }) {
      prefsRepo.updateFontSize(clamped)
    }
  }

  fun setLineHeight(mult: Float) {
    val clamped = mult.coerceIn(1.05f, 2.3f)
    updateLiveAndPersist("lineHeight", 180L, { it.copy(lineHeightMultiplier = clamped) }) {
      prefsRepo.updateLineHeight(clamped)
    }
  }

  fun setLetterSpacing(sp: Float) {
    val clamped = sp.coerceIn(-0.5f, 3.0f)
    updateLiveAndPersist("letterSpacing", 180L, { it.copy(letterSpacingSp = clamped) }) {
      prefsRepo.updateLetterSpacing(clamped)
    }
  }

  fun setWordSpacing(sp: Float) {
    val clamped = sp.coerceIn(0f, 8.0f)
    updateLiveAndPersist("wordSpacing", 180L, { it.copy(wordSpacingSp = clamped) }) {
      prefsRepo.updateWordSpacing(clamped)
    }
  }

  fun setHorizontalPadding(dp: Float) {
    val clamped = dp.coerceIn(8f, 44f)
    updateLiveAndPersist("hPadding", 180L, { it.copy(horizontalPaddingDp = clamped) }) {
      prefsRepo.updateHorizontalPadding(clamped)
    }
  }

  fun setParagraphSpacing(dp: Float) {
    val clamped = dp.coerceIn(0f, 28f)
    updateLiveAndPersist("pSpacing", 180L, { it.copy(paragraphSpacingDp = clamped) }) {
      prefsRepo.updateParagraphSpacing(clamped)
    }
  }

  fun setFirstLineIndent(sp: Float) {
    val clamped = sp.coerceIn(0f, 36f)
    updateLiveAndPersist("indent", 180L, { it.copy(firstLineIndentSp = clamped) }) {
      prefsRepo.updateFirstLineIndent(clamped)
    }
  }

  fun setJustify(justify: Boolean) {
    updateLiveAndPersist("justify", 0L, { it.copy(textAlignJustify = justify) }) {
      prefsRepo.updateJustify(justify)
    }
  }

  fun setReaderTheme(theme: ReaderTheme) {
    updateLiveAndPersist("theme", 0L, { it.copy(readerTheme = theme) }) {
      prefsRepo.updateReaderTheme(theme)
    }
  }

  fun setPageAnimation(animation: PageFlipAnimation) {
    updateLiveAndPersist("anim", 0L, { it.copy(pageAnimation = animation) }) {
      prefsRepo.updatePageAnimation(animation)
    }
  }

  fun resetTypographyDefaults() {
    updateLiveAndPersist(
      key = "reset",
      debounceMs = 0L,
      transform = {
        it.copy(
          fontSizeSp = 19f,
          lineHeightMultiplier = 1.55f,
          letterSpacingSp = 0.1f,
          wordSpacingSp = 0f,
          horizontalPaddingDp = 22f,
          paragraphSpacingDp = 4f,
          firstLineIndentSp = 16f,
          textAlignJustify = false,
        )
      },
    ) {
      prefsRepo.resetTypographyDefaults()
    }
  }

  fun isPdfReady(): Boolean {
    val state = loadState.value as? ReaderLoadState.Ready ?: return false
    return state.book.format == BookFormat.PDF
  }
}
