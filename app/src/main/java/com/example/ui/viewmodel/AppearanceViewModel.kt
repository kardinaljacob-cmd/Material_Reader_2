package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookRepository
import com.example.data.db.BookDatabase
import com.example.data.model.PageFlipAnimation
import com.example.data.prefs.ReaderPreferences
import com.example.data.prefs.ReaderPreferencesRepository
import com.example.ui.theme.DarkModeSetting
import com.example.ui.theme.ExpressivePalette
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(
  val preferences: ReaderPreferences = ReaderPreferences(),
  val totalBooks: Int = 0,
  val totalBookmarks: Int = 0,
  val favoriteBooks: Int = 0,
  val averageProgressPercent: Int = 0,
)

class AppearanceViewModel(application: Application) : AndroidViewModel(application) {
  private val db = BookDatabase.getInstance(application)
  private val bookRepo = BookRepository(application, db.bookDao())
  private val prefsRepo = ReaderPreferencesRepository(application)

  val uiState: StateFlow<AppearanceUiState> =
    combine(
        prefsRepo.preferencesFlow,
        bookRepo.allBooks,
        bookRepo.totalBookmarkCount,
      ) { prefs, books, bookmarks ->
        val avg =
          if (books.isEmpty()) 0
          else ((books.sumOf { it.progressPercent.toDouble() } / books.size) * 100).toInt()
        AppearanceUiState(
          preferences = prefs,
          totalBooks = books.size,
          totalBookmarks = bookmarks,
          favoriteBooks = books.count { it.isFavorite },
          averageProgressPercent = avg,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceUiState(),
      )

  fun setAppPalette(palette: ExpressivePalette) {
    viewModelScope.launch { prefsRepo.updateAppPalette(palette) }
  }

  fun setDarkMode(setting: DarkModeSetting) {
    viewModelScope.launch { prefsRepo.updateDarkMode(setting) }
  }

  fun setReaderTheme(theme: ReaderTheme) {
    viewModelScope.launch { prefsRepo.updateReaderTheme(theme) }
  }

  fun setFontOption(option: ReaderFontOption) {
    viewModelScope.launch { prefsRepo.updateFont(option) }
  }

  fun setPageAnimation(animation: PageFlipAnimation) {
    viewModelScope.launch { prefsRepo.updatePageAnimation(animation) }
  }

  fun setHapticsEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.updateHaptics(enabled) }
  }

  fun setKeepScreenOn(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.updateKeepScreenOn(enabled) }
  }
}
