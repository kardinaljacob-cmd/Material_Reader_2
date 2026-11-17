package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PageFlipAnimation
import com.example.ui.theme.DarkModeSetting
import com.example.ui.theme.ExpressivePalette
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "listay_reader_prefs")

data class ReaderPreferences(
  val fontOption: ReaderFontOption = ReaderFontOption.LITERATA,
  val fontSizeSp: Float = 19f,
  val lineHeightMultiplier: Float = 1.55f,
  val letterSpacingSp: Float = 0.1f,
  val wordSpacingSp: Float = 0f,
  val horizontalPaddingDp: Float = 22f,
  val paragraphSpacingDp: Float = 4f,
  val firstLineIndentSp: Float = 16f,
  val textAlignJustify: Boolean = false,
  val readerTheme: ReaderTheme = ReaderTheme.DYNAMIC_WALLPAPER,
  val pageAnimation: PageFlipAnimation = PageFlipAnimation.DEPTH_SLIDE,
  val appPalette: ExpressivePalette = ExpressivePalette.WALLPAPER_DYNAMIC,
  val darkModeSetting: DarkModeSetting = DarkModeSetting.SYSTEM,
  val hapticsEnabled: Boolean = true,
  val keepScreenOn: Boolean = true,
)

class ReaderPreferencesRepository(private val context: Context) {
  private object Keys {
    val FONT = stringPreferencesKey("font_option")
    val FONT_SIZE = floatPreferencesKey("font_size_sp")
    val LINE_HEIGHT = floatPreferencesKey("line_height_mult")
    val LETTER_SPACING = floatPreferencesKey("letter_spacing_sp")
    val WORD_SPACING = floatPreferencesKey("word_spacing_sp")
    val H_PADDING = floatPreferencesKey("h_padding_dp")
    val P_SPACING = floatPreferencesKey("p_spacing_dp")
    val FIRST_LINE_INDENT = floatPreferencesKey("first_line_indent_sp")
    val JUSTIFY = booleanPreferencesKey("text_align_justify")
    val READER_THEME = stringPreferencesKey("reader_theme")
    val PAGE_ANIM = stringPreferencesKey("page_animation")
    val APP_PALETTE = stringPreferencesKey("app_palette")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val HAPTICS = booleanPreferencesKey("haptics_enabled")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
  }

  val preferencesFlow: Flow<ReaderPreferences> =
    context.readerDataStore.data.map { prefs ->
      ReaderPreferences(
        fontOption =
          prefs[Keys.FONT]?.let { runCatching { ReaderFontOption.valueOf(it) }.getOrNull() }
            ?: ReaderFontOption.LITERATA,
        fontSizeSp = prefs[Keys.FONT_SIZE] ?: 19f,
        lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.55f,
        letterSpacingSp = prefs[Keys.LETTER_SPACING] ?: 0.1f,
        wordSpacingSp = prefs[Keys.WORD_SPACING] ?: 0f,
        horizontalPaddingDp = prefs[Keys.H_PADDING] ?: 22f,
        paragraphSpacingDp = prefs[Keys.P_SPACING] ?: 4f,
        firstLineIndentSp = prefs[Keys.FIRST_LINE_INDENT] ?: 16f,
        textAlignJustify = prefs[Keys.JUSTIFY] ?: false,
        readerTheme =
          prefs[Keys.READER_THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
            ?: ReaderTheme.DYNAMIC_WALLPAPER,
        pageAnimation =
          prefs[Keys.PAGE_ANIM]?.let { runCatching { PageFlipAnimation.valueOf(it) }.getOrNull() }
            ?: PageFlipAnimation.DEPTH_SLIDE,
        appPalette =
          prefs[Keys.APP_PALETTE]?.let { runCatching { ExpressivePalette.valueOf(it) }.getOrNull() }
            ?: ExpressivePalette.WALLPAPER_DYNAMIC,
        darkModeSetting =
          prefs[Keys.DARK_MODE]?.let { runCatching { DarkModeSetting.valueOf(it) }.getOrNull() }
            ?: DarkModeSetting.SYSTEM,
        hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
        keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
      )
    }

  suspend fun updateFont(option: ReaderFontOption) {
    context.readerDataStore.edit { it[Keys.FONT] = option.name }
  }

  suspend fun updateFontSize(sizeSp: Float) {
    context.readerDataStore.edit { it[Keys.FONT_SIZE] = sizeSp.coerceIn(14f, 32f) }
  }

  suspend fun updateLineHeight(multiplier: Float) {
    context.readerDataStore.edit { it[Keys.LINE_HEIGHT] = multiplier.coerceIn(1.05f, 2.3f) }
  }

  suspend fun updateLetterSpacing(sp: Float) {
    context.readerDataStore.edit { it[Keys.LETTER_SPACING] = sp.coerceIn(-0.5f, 3.0f) }
  }

  suspend fun updateWordSpacing(sp: Float) {
    context.readerDataStore.edit { it[Keys.WORD_SPACING] = sp.coerceIn(0f, 8.0f) }
  }

  suspend fun updateHorizontalPadding(dp: Float) {
    context.readerDataStore.edit { it[Keys.H_PADDING] = dp.coerceIn(8f, 44f) }
  }

  suspend fun updateParagraphSpacing(dp: Float) {
    context.readerDataStore.edit { it[Keys.P_SPACING] = dp.coerceIn(0f, 28f) }
  }

  suspend fun updateFirstLineIndent(sp: Float) {
    context.readerDataStore.edit { it[Keys.FIRST_LINE_INDENT] = sp.coerceIn(0f, 36f) }
  }

  suspend fun updateJustify(justify: Boolean) {
    context.readerDataStore.edit { it[Keys.JUSTIFY] = justify }
  }

  suspend fun updateReaderTheme(theme: ReaderTheme) {
    context.readerDataStore.edit { it[Keys.READER_THEME] = theme.name }
  }

  suspend fun updatePageAnimation(animation: PageFlipAnimation) {
    context.readerDataStore.edit { it[Keys.PAGE_ANIM] = animation.name }
  }

  suspend fun updateAppPalette(palette: ExpressivePalette) {
    context.readerDataStore.edit { it[Keys.APP_PALETTE] = palette.name }
  }

  suspend fun updateDarkMode(setting: DarkModeSetting) {
    context.readerDataStore.edit { it[Keys.DARK_MODE] = setting.name }
  }

  suspend fun updateHaptics(enabled: Boolean) {
    context.readerDataStore.edit { it[Keys.HAPTICS] = enabled }
  }

  suspend fun updateKeepScreenOn(enabled: Boolean) {
    context.readerDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
  }

  suspend fun resetTypographyDefaults() {
    context.readerDataStore.edit {
      it[Keys.FONT_SIZE] = 19f
      it[Keys.LINE_HEIGHT] = 1.55f
      it[Keys.LETTER_SPACING] = 0.1f
      it[Keys.WORD_SPACING] = 0f
      it[Keys.H_PADDING] = 22f
      it[Keys.P_SPACING] = 4f
      it[Keys.FIRST_LINE_INDENT] = 16f
      it[Keys.JUSTIFY] = false
    }
  }
}
