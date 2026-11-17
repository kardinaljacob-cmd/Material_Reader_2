package com.example.data.parser

import com.example.data.model.BookChapter
import com.example.data.model.BookFootnote
import com.example.data.model.BookFormat
import com.example.data.model.BookParagraph
import com.example.data.model.InlineSpan
import com.example.data.model.InlineSpanType
import com.example.data.model.ParagraphStyle
import com.example.data.model.ParsedBook
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object TextFormatParser {

  private val HTML_TAG_REGEX = Regex("<[^>]+>")
  private val DECIMAL_ENTITY_REGEX = Regex("""&#(\d+);""")
  private val HEX_ENTITY_REGEX = Regex("""&#x([0-9a-fA-F]+);""")
  private val HTML_TITLE_REGEX = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
  private val CHAPTER_HEADING_REGEX =
    Regex("""^(глава|часть|раздел|chapter|part|book|пролог|эпилог)\b.*""", RegexOption.IGNORE_CASE)
  private val FOOTNOTE_DEF_REGEX = Regex("""^\[\^([^\]]+)]:\s*(.+)$""")
  private val MD_INLINE_REGEX =
    Regex("""(\[\^([^\]]+)]|\*\*([^*]+)\*\*|~~([^~]+)~~|`([^`]+)`|\*([^*]+)\*)""")

  fun parse(inputStream: InputStream, format: BookFormat, fallbackTitle: String): ParsedBook {
    val bytes = inputStream.readBytes()
    val text = decodeSmart(bytes)

    return when (format) {
      BookFormat.HTML -> {
        val known = CssStyleParser.extractFootnoteTargets(text)
        val result =
          CssStyleParser.parseHtmlWithCss(
            html = text,
            defaultTitle = fallbackTitle,
            knownNoteTargets = known.keys,
          )
        val mergedNotes = LinkedHashMap(known).apply { putAll(result.footnotes) }
        ParsedBook(
          title = extractHtmlTitle(text) ?: fallbackTitle,
          author = "Веб / HTML документ",
          description = "",
          chapters = listOf(result.chapter.copy(index = 0)),
          footnotes = mergedNotes,
        )
      }
      BookFormat.MD -> parseMarkdown(text, fallbackTitle)
      else -> parsePlainText(text, fallbackTitle)
    }
  }

  private fun parsePlainText(rawText: String, fallbackTitle: String): ParsedBook {
    val normalized = rawText.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.lines()
    val chapterHeadingRegex = CHAPTER_HEADING_REGEX

    val chapters = mutableListOf<BookChapter>()
    var currentTitle = "Начало"
    val currentParagraphs = mutableListOf<BookParagraph>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
      val p = paragraphBuffer.toString().trim()
      if (p.isNotEmpty()) {
        currentParagraphs.add(BookParagraph(p, ParagraphStyle.BODY))
      }
      paragraphBuffer.clear()
    }

    fun flushChapter() {
      flushParagraph()
      if (currentParagraphs.isNotEmpty()) {
        chapters.add(
          BookChapter(
            index = chapters.size,
            title = currentTitle,
            paragraphs = currentParagraphs.toList(),
          )
        )
        currentParagraphs.clear()
      }
    }

    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.isEmpty()) {
        flushParagraph()
      } else if (chapterHeadingRegex.matches(trimmed) && trimmed.length < 90) {
        if (currentParagraphs.isNotEmpty() || paragraphBuffer.isNotEmpty()) {
          flushChapter()
        }
        currentTitle = trimmed
        currentParagraphs.add(BookParagraph(trimmed, ParagraphStyle.HEADING))
      } else {
        if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(' ')
        paragraphBuffer.append(trimmed)
      }
    }
    flushChapter()

    if (chapters.size == 1 && chapters[0].paragraphs.size > 40) {
      val all = chapters[0].paragraphs
      chapters.clear()
      all.chunked(32).forEachIndexed { idx, chunk ->
        chapters.add(
          BookChapter(
            index = idx,
            title = "Часть ${idx + 1}",
            paragraphs = chunk,
          )
        )
      }
    }

    if (chapters.isEmpty()) {
      chapters.add(
        BookChapter(
          index = 0,
          title = fallbackTitle,
          paragraphs = listOf(BookParagraph("Пустой текстовый файл.", ParagraphStyle.BODY)),
        )
      )
    }

    return ParsedBook(
      title = fallbackTitle,
      author = "Текстовый документ",
      description = "",
      chapters = chapters,
    )
  }

  private fun parseMarkdown(rawText: String, fallbackTitle: String): ParsedBook {
    val lines = rawText.replace("\r\n", "\n").lines()
    val footnotes = linkedMapOf<String, BookFootnote>()
    val footnoteDefRegex = FOOTNOTE_DEF_REGEX

    // Pass 1: collect [^id]: footnote definitions so we don't print them inline
    val filteredLines = ArrayList<String>(lines.size)
    for (line in lines) {
      val defMatch = footnoteDefRegex.matchEntire(line.trim())
      if (defMatch != null) {
        val id = defMatch.groupValues[1].trim()
        val body = defMatch.groupValues[2].trim()
        footnotes[id] = BookFootnote(id = id, marker = id, text = body)
      } else {
        filteredLines.add(line)
      }
    }

    val chapters = mutableListOf<BookChapter>()
    var bookTitle = fallbackTitle
    var currentChapterTitle = "Введение"
    val paragraphs = mutableListOf<BookParagraph>()
    val buffer = StringBuilder()
    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    fun addMarkdownParagraph(raw: String, style: ParagraphStyle) {
      val (plain, spans) = parseMarkdownInline(raw)
      if (plain.isNotEmpty()) {
        paragraphs.add(BookParagraph(text = plain, style = style, spans = spans))
      }
    }

    fun flushBuffer() {
      val p = buffer.toString().trim()
      if (p.isNotEmpty()) addMarkdownParagraph(p, ParagraphStyle.BODY)
      buffer.clear()
    }

    fun flushChapter() {
      flushBuffer()
      if (paragraphs.isNotEmpty()) {
        chapters.add(
          BookChapter(
            index = chapters.size,
            title = currentChapterTitle,
            paragraphs = paragraphs.toList(),
          )
        )
        paragraphs.clear()
      }
    }

    for (line in filteredLines) {
      val trimmed = line.trim()
      if (trimmed.startsWith("```")) {
        flushBuffer()
        if (inCodeBlock) {
          if (codeBuffer.isNotEmpty()) {
            paragraphs.add(BookParagraph(codeBuffer.toString().trimEnd(), ParagraphStyle.CODE))
            codeBuffer.clear()
          }
          inCodeBlock = false
        } else {
          inCodeBlock = true
        }
        continue
      }
      if (inCodeBlock) {
        codeBuffer.appendLine(line)
        continue
      }

      when {
        trimmed.startsWith("# ") -> {
          val h = trimmed.removePrefix("# ").trim()
          if (bookTitle == fallbackTitle && chapters.isEmpty() && paragraphs.isEmpty()) {
            bookTitle = h
          }
          if (paragraphs.isNotEmpty()) flushChapter()
          currentChapterTitle = h
          addMarkdownParagraph(h, ParagraphStyle.HEADING)
        }
        trimmed.startsWith("## ") || trimmed.startsWith("### ") -> {
          flushBuffer()
          val h = trimmed.trimStart('#').trim()
          if (paragraphs.isEmpty()) currentChapterTitle = h
          addMarkdownParagraph(h, ParagraphStyle.SUBHEADING)
        }
        trimmed.startsWith("> ") -> {
          flushBuffer()
          addMarkdownParagraph(trimmed.removePrefix("> ").trim(), ParagraphStyle.QUOTE)
        }
        trimmed.isEmpty() -> flushBuffer()
        else -> {
          if (buffer.isNotEmpty()) buffer.append(' ')
          buffer.append(trimmed)
        }
      }
    }
    flushChapter()

    if (chapters.isEmpty()) {
      chapters.add(BookChapter(0, bookTitle, listOf(BookParagraph(rawText, ParagraphStyle.BODY))))
    }

    return ParsedBook(
      title = bookTitle,
      author = "Markdown",
      description = "",
      chapters = chapters,
      footnotes = footnotes,
    )
  }

  private fun parseMarkdownInline(raw: String): Pair<String, List<InlineSpan>> {
    val inlineRegex = MD_INLINE_REGEX
    val out = StringBuilder()
    val spans = mutableListOf<InlineSpan>()
    var cursor = 0
    for (m in inlineRegex.findAll(raw)) {
      if (m.range.first > cursor) {
        out.append(raw.substring(cursor, m.range.first))
      }
      val start = out.length
      when {
        m.groupValues[2].isNotEmpty() -> {
          val fnId = m.groupValues[2].trim()
          val marker = "[$fnId]"
          out.append(marker)
          spans.add(InlineSpan(start, out.length, InlineSpanType.FOOTNOTE_REF, targetId = fnId))
        }
        m.groupValues[3].isNotEmpty() -> {
          out.append(m.groupValues[3])
          spans.add(InlineSpan(start, out.length, InlineSpanType.BOLD))
        }
        m.groupValues[4].isNotEmpty() -> {
          out.append(m.groupValues[4])
          spans.add(InlineSpan(start, out.length, InlineSpanType.STRIKETHROUGH))
        }
        m.groupValues[5].isNotEmpty() -> {
          out.append(m.groupValues[5])
          spans.add(InlineSpan(start, out.length, InlineSpanType.CODE))
        }
        m.groupValues[6].isNotEmpty() -> {
          out.append(m.groupValues[6])
          spans.add(InlineSpan(start, out.length, InlineSpanType.ITALIC))
        }
      }
      cursor = m.range.last + 1
    }
    if (cursor < raw.length) out.append(raw.substring(cursor))
    return out.toString() to spans
  }

  fun parseHtmlChapter(
    html: String,
    defaultTitle: String,
    cssSheet: CssStyleParser.CssSheet = CssStyleParser.CssSheet(),
    chapterHref: String = "",
    knownNoteTargets: Set<String> = emptySet(),
  ): BookChapter {
    return CssStyleParser.parseHtmlWithCss(
      html = html,
      defaultTitle = defaultTitle,
      externalCss = cssSheet,
      chapterHref = chapterHref,
      knownNoteTargets = knownNoteTargets,
    ).chapter
  }

  private fun extractHtmlTitle(html: String): String? {
    val m = HTML_TITLE_REGEX.find(html)
    return m?.groupValues?.getOrNull(1)?.let { decodeEntities(stripHtmlTags(it)).trim() }?.takeIf {
      it.isNotBlank()
    }
  }

  fun stripHtmlTags(raw: String): String = raw.replace(HTML_TAG_REGEX, " ")

  fun decodeEntities(input: String): String {
    if (input.indexOf('&') < 0) return input
    var out =
      input
        .replace("&nbsp;", "\u00A0")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&hellip;", "…")
    if (out.indexOf("&#") >= 0) {
      out =
        DECIMAL_ENTITY_REGEX.replace(out) { m ->
          m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
      out =
        HEX_ENTITY_REGEX.replace(out) { m ->
          m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
    }
    return out
  }

  fun decodeSmart(bytes: ByteArray): String {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
      return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
      return String(bytes, Charsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
      return String(bytes, Charsets.UTF_16BE)
    }
    return if (isValidUtf8(bytes)) {
      String(bytes, Charsets.UTF_8)
    } else {
      String(bytes, Charset.forName("windows-1251"))
    }
  }

  fun isValidUtf8(bytes: ByteArray): Boolean {
    val decoder = Charsets.UTF_8.newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
    return runCatching {
      decoder.decode(ByteBuffer.wrap(bytes))
      true
    }.getOrDefault(false)
  }
}
