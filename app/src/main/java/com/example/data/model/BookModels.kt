package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookFormat(val label: String, val extensions: List<String>) {
  EPUB("EPUB", listOf("epub")),
  FB2("FB2", listOf("fb2", "fb2.zip")),
  PDF("PDF", listOf("pdf")),
  TXT("TXT", listOf("txt")),
  HTML("HTML", listOf("html", "htm", "xhtml")),
  MD("MD", listOf("md", "markdown"));

  companion object {
    fun fromFileName(name: String): BookFormat {
      val lower = name.lowercase()
      return when {
        lower.endsWith(".fb2.zip") || lower.endsWith(".fb2") -> FB2
        lower.endsWith(".epub") -> EPUB
        lower.endsWith(".pdf") -> PDF
        lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xhtml") -> HTML
        lower.endsWith(".md") || lower.endsWith(".markdown") -> MD
        else -> TXT
      }
    }
  }
}

enum class PageFlipAnimation(val title: String, val subtitle: String) {
  CURL_3D("3D Листание", "Объёмный перегиб книжной страницы"),
  DEPTH_SLIDE("Глубина", "Плавный параллакс с глубиной"),
  VERTICAL_SCROLL("Свиток", "Непрерывная вертикальная лента"),
}

enum class ParagraphStyle {
  BODY,
  HEADING,
  SUBHEADING,
  QUOTE,
  EPIGRAPH,
  POEM,
  CODE,
}

enum class InlineSpanType {
  BOLD,
  ITALIC,
  UNDERLINE,
  STRIKETHROUGH,
  CODE,
  SUPERSCRIPT,
  SUBSCRIPT,
  SMALL_CAPS,
  FOOTNOTE_REF,
  CHAPTER_LINK,
  STYLED,
}

data class InlineSpan(
  val start: Int,
  val end: Int,
  val type: InlineSpanType,
  val targetId: String? = null,
  val colorHex: Long? = null,
  val backgroundColorHex: Long? = null,
  val fontSizeScale: Float? = null,
  val isMonospace: Boolean = false,
)

enum class CssTextAlign {
  LEFT,
  CENTER,
  RIGHT,
  JUSTIFY,
}

data class ParagraphCss(
  val textAlign: CssTextAlign? = null,
  val hasFirstLineIndent: Boolean? = null,
  val firstLineIndentEm: Float? = null,
  val marginTopDp: Float = 0f,
  val marginBottomDp: Float = 0f,
  val marginStartDp: Float = 0f,
  val fontScale: Float = 1f,
  val isBold: Boolean? = null,
  val isItalic: Boolean? = null,
  val isSmallCaps: Boolean = false,
  val isMonospace: Boolean = false,
  val colorHex: Long? = null,
)

data class BookAnchorTarget(
  val chapterIndex: Int,
  val paragraphIndex: Int = 0,
)

data class BookFootnote(
  val id: String,
  val marker: String,
  val text: String,
)

data class BookParagraph(
  val text: String,
  val style: ParagraphStyle = ParagraphStyle.BODY,
  val spans: List<InlineSpan> = emptyList(),
  val css: ParagraphCss = ParagraphCss(),
)

data class BookChapter(
  val index: Int,
  val title: String,
  val paragraphs: List<BookParagraph>,
)

data class ParsedBook(
  val title: String,
  val author: String,
  val description: String,
  val chapters: List<BookChapter>,
  val coverBytes: ByteArray? = null,
  val footnotes: Map<String, BookFootnote> = emptyMap(),
  val anchorTargets: Map<String, BookAnchorTarget> = emptyMap(),
)

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val author: String,
  val formatName: String,
  val filePath: String,
  val coverPath: String? = null,
  val bundledCoverKey: String? = null,
  val totalChapters: Int = 1,
  val currentChapterIndex: Int = 0,
  val currentPageInChapter: Int = 0,
  val progressPercent: Float = 0f,
  val lastReadTimestamp: Long = 0L,
  val addedTimestamp: Long = System.currentTimeMillis(),
  val isFavorite: Boolean = false,
  val accentColorHex: Long = 0xFF6D28D9,
  val fileSizeBytes: Long = 0L,
  val description: String = "",
) {
  val format: BookFormat
    get() = runCatching { BookFormat.valueOf(formatName) }.getOrDefault(BookFormat.TXT)
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: Long,
  val chapterIndex: Int,
  val chapterTitle: String,
  val pageIndex: Int,
  val excerpt: String,
  val colorIndex: Int = 0,
  val createdTimestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: Long,
  val chapterIndex: Int,
  val chapterTitle: String,
  val pageIndex: Int,
  val selectedText: String,
  val noteText: String = "",
  val colorIndex: Int = 0,
  val createdTimestamp: Long = System.currentTimeMillis(),
)
