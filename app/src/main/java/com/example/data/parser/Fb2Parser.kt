package com.example.data.parser

import android.util.Base64
import android.util.Xml
import com.example.data.model.BookChapter
import com.example.data.model.BookFootnote
import com.example.data.model.BookParagraph
import com.example.data.model.InlineSpan
import com.example.data.model.InlineSpanType
import com.example.data.model.ParagraphStyle
import com.example.data.model.ParsedBook
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

object Fb2Parser {

  private val WHITESPACE_REGEX = Regex("\\s+")
  private val CRLF_TAB_REGEX = Regex("[\\r\\n\\t]+")
  private val MULTI_SPACE_REGEX = Regex(" {2,}")
  private val ENCODING_ATTR_REGEX = Regex("""encoding=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

  private data class RichBlock(
    val text: String,
    val spans: List<InlineSpan>,
  )

  fun parse(inputStream: InputStream, fallbackTitle: String): ParsedBook {
    val rawBytes = inputStream.readBytes()
    val xmlBytes =
      if (rawBytes.size >= 2 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
        unzipFirstFb2(rawBytes) ?: rawBytes
      } else {
        rawBytes
      }

    val encoding = detectXmlEncoding(xmlBytes)

    var title = fallbackTitle
    val authors = mutableListOf<String>()
    var description = ""
    var coverBinaryId: String? = null
    var resolvedCoverBytes: ByteArray? = null
    var fallbackBinaryBytes: ByteArray? = null
    val chapters = mutableListOf<BookChapter>()
    val footnotes = linkedMapOf<String, BookFootnote>()

    runCatching {
      val parser = Xml.newPullParser()
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
      parser.setInput(ByteArrayInputStream(xmlBytes), encoding.name())

      var inTitleInfo = false
      var inAuthor = false
      var inCoverPage = false
      var inBody = false
      var inNotesBody = false
      var currentNoteId: String? = null
      var currentNoteTitle = ""
      val currentNoteParagraphs = mutableListOf<String>()
      var currentFirst = ""
      var currentMiddle = ""
      var currentLast = ""
      var sectionDepth = 0
      var currentChapterTitle = ""
      val currentParagraphs = mutableListOf<BookParagraph>()

      fun flushNote() {
        val noteId = currentNoteId
        if (!noteId.isNullOrBlank() && currentNoteParagraphs.isNotEmpty()) {
          val marker = currentNoteTitle.trim().ifBlank { noteId }
          footnotes[noteId] =
            BookFootnote(
              id = noteId,
              marker = marker,
              text = currentNoteParagraphs.joinToString("\n").trim(),
            )
        }
        currentNoteId = null
        currentNoteTitle = ""
        currentNoteParagraphs.clear()
      }

      fun flushChapter() {
        if (currentParagraphs.isNotEmpty()) {
          val resolvedTitle =
            currentChapterTitle.trim().ifBlank { "Глава ${chapters.size + 1}" }
          chapters.add(
            BookChapter(
              index = chapters.size,
              title = resolvedTitle,
              paragraphs = currentParagraphs.toList(),
            )
          )
          currentParagraphs.clear()
          currentChapterTitle = ""
        }
      }

      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
          XmlPullParser.START_TAG -> {
            val tag = parser.name.substringAfterLast(':').lowercase()
            when (tag) {
              "title-info" -> inTitleInfo = true
              "author" -> if (inTitleInfo) {
                inAuthor = true
                currentFirst = ""
                currentMiddle = ""
                currentLast = ""
              }
              "first-name" -> if (inAuthor) currentFirst = readTagText(parser).trim()
              "middle-name" -> if (inAuthor) currentMiddle = readTagText(parser).trim()
              "last-name" -> if (inAuthor) currentLast = readTagText(parser).trim()
              "book-title" -> if (inTitleInfo) {
                val t = readTagText(parser).trim()
                if (t.isNotEmpty()) title = t
              }
              "annotation" -> if (inTitleInfo) {
                description = readTagText(parser).replace(WHITESPACE_REGEX, " ").trim()
              }
              "coverpage" -> if (inTitleInfo) inCoverPage = true
              "image" -> {
                if (inCoverPage && coverBinaryId == null) {
                  for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i).endsWith("href")) {
                      coverBinaryId = parser.getAttributeValue(i).trimStart('#')
                    }
                  }
                }
              }
              "binary" -> {
                val id = parser.getAttributeValue(null, "id")
                val needDecode =
                  !id.isNullOrBlank() &&
                    (id == coverBinaryId || (coverBinaryId == null && fallbackBinaryBytes == null))
                if (needDecode) {
                  val b64 = readTagText(parser).replace(WHITESPACE_REGEX, "")
                  if (b64.isNotEmpty()) {
                    val decoded = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                    if (decoded != null) {
                      if (id == coverBinaryId) resolvedCoverBytes = decoded
                      else fallbackBinaryBytes = decoded
                    }
                  }
                } else {
                  skipTag(parser)
                }
              }
              "body" -> {
                val nameAttr = parser.getAttributeValue(null, "name")
                if (nameAttr.equals("notes", ignoreCase = true) ||
                  nameAttr.equals("comments", ignoreCase = true)
                ) {
                  inNotesBody = true
                } else {
                  inBody = true
                }
              }
              "section" -> {
                if (inNotesBody) {
                  flushNote()
                  currentNoteId = parser.getAttributeValue(null, "id")
                } else if (inBody) {
                  if (sectionDepth > 0 && currentParagraphs.isNotEmpty()) {
                    flushChapter()
                  }
                  sectionDepth++
                }
              }
              "title" -> {
                if (inNotesBody && currentNoteId != null) {
                  currentNoteTitle = readTagText(parser).replace(WHITESPACE_REGEX, " ").trim()
                } else if (inBody) {
                  val t = readTagText(parser).replace(WHITESPACE_REGEX, " ").trim()
                  if (t.isNotEmpty()) {
                    if (currentChapterTitle.isEmpty()) currentChapterTitle = t
                    currentParagraphs.add(BookParagraph(t, ParagraphStyle.HEADING))
                  }
                }
              }
              "subtitle" -> if (inBody) {
                val block = readRichBlock(parser)
                if (block.text.isNotEmpty()) {
                  currentParagraphs.add(BookParagraph(block.text, ParagraphStyle.SUBHEADING, block.spans))
                }
              }
              "epigraph" -> if (inBody) {
                val block = readRichBlock(parser)
                if (block.text.isNotEmpty()) {
                  currentParagraphs.add(BookParagraph(block.text, ParagraphStyle.EPIGRAPH, block.spans))
                }
              }
              "cite" -> if (inBody) {
                val block = readRichBlock(parser)
                if (block.text.isNotEmpty()) {
                  currentParagraphs.add(BookParagraph(block.text, ParagraphStyle.QUOTE, block.spans))
                }
              }
              "poem" -> if (inBody) {
                val block = readPoemRichBlock(parser)
                if (block.text.isNotEmpty()) {
                  currentParagraphs.add(BookParagraph(block.text, ParagraphStyle.POEM, block.spans))
                }
              }
              "p" -> {
                if (inNotesBody && currentNoteId != null) {
                  val t = readTagText(parser).replace(WHITESPACE_REGEX, " ").trim()
                  if (t.isNotEmpty()) currentNoteParagraphs.add(t)
                } else if (inBody) {
                  val block = readRichBlock(parser)
                  if (block.text.isNotEmpty()) {
                    currentParagraphs.add(BookParagraph(block.text, ParagraphStyle.BODY, block.spans))
                  }
                }
              }
            }
          }
          XmlPullParser.END_TAG -> {
            val tag = parser.name.substringAfterLast(':').lowercase()
            when (tag) {
              "title-info" -> inTitleInfo = false
              "coverpage" -> inCoverPage = false
              "author" -> if (inAuthor) {
                inAuthor = false
                val full = listOf(currentFirst, currentMiddle, currentLast)
                  .filter { it.isNotBlank() }
                  .joinToString(" ")
                if (full.isNotBlank()) authors.add(full)
              }
              "section" -> {
                if (inNotesBody) {
                  flushNote()
                } else if (inBody) {
                  sectionDepth = (sectionDepth - 1).coerceAtLeast(0)
                  if (sectionDepth == 0) flushChapter()
                }
              }
              "body" -> {
                if (inNotesBody) {
                  flushNote()
                  inNotesBody = false
                } else if (inBody) {
                  flushChapter()
                  inBody = false
                }
              }
            }
          }
        }
        event = parser.next()
      }
      flushNote()
      flushChapter()
    }

    if (chapters.isEmpty()) {
      chapters.add(
        BookChapter(
          index = 0,
          title = title,
          paragraphs = listOf(BookParagraph("Текст FB2 не найден или повреждён.", ParagraphStyle.BODY)),
        )
      )
    }

    return ParsedBook(
      title = title,
      author = authors.joinToString(", ").ifBlank { "Неизвестный автор" },
      description = description,
      chapters = chapters,
      coverBytes = resolvedCoverBytes ?: fallbackBinaryBytes,
      footnotes = footnotes,
    )
  }

  private fun skipTag(parser: XmlPullParser) {
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.START_TAG -> depth++
        XmlPullParser.END_TAG -> depth--
        XmlPullParser.END_DOCUMENT -> break
      }
    }
  }

  private fun unzipFirstFb2(bytes: ByteArray): ByteArray? {
    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".fb2", ignoreCase = true)) {
          return zis.readBytes()
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
    return null
  }

  private fun detectXmlEncoding(bytes: ByteArray): Charset {
    val header = String(bytes.copyOfRange(0, minOf(bytes.size, 256)), Charsets.ISO_8859_1)
    val match = ENCODING_ATTR_REGEX.find(header)
    val name = match?.groupValues?.getOrNull(1)
    if (!name.isNullOrBlank()) {
      runCatching { return Charset.forName(name) }
    }
    return if (TextFormatParser.isValidUtf8(bytes)) Charsets.UTF_8 else Charset.forName("windows-1251")
  }

  private fun readTagText(parser: XmlPullParser): String {
    val sb = StringBuilder()
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.TEXT -> sb.append(parser.text)
        XmlPullParser.START_TAG -> {
          val tag = parser.name.substringAfterLast(':').lowercase()
          if (tag == "p" || tag == "br" || tag == "empty-line") sb.append("\n")
          depth++
        }
        XmlPullParser.END_TAG -> depth--
        XmlPullParser.END_DOCUMENT -> break
      }
    }
    return sb.toString()
  }

  private fun readRichBlock(parser: XmlPullParser): RichBlock {
    data class OpenTag(val tag: String, val start: Int, val noteTargetId: String?)
    val sb = StringBuilder()
    val spans = mutableListOf<InlineSpan>()
    val stack = mutableListOf<OpenTag>()
    var depth = 1

    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.TEXT -> {
          val chunk = parser.text.replace(CRLF_TAB_REGEX, " ").replace(MULTI_SPACE_REGEX, " ")
          if (chunk.isNotEmpty()) {
            val trimmedChunk =
              if (sb.isEmpty() || sb.endsWith(" ")) chunk.trimStart() else chunk
            sb.append(trimmedChunk)
          }
        }
        XmlPullParser.START_TAG -> {
          depth++
          val tag = parser.name.substringAfterLast(':').lowercase()
          if ((tag == "p" || tag == "br" || tag == "empty-line") && sb.isNotEmpty() && !sb.endsWith("\n")) {
            sb.append(' ')
          }
          var hrefTarget: String? = null
          if (tag == "a") {
            val typeAttr = parser.getAttributeValue(null, "type")?.lowercase()
            var href: String? = null
            for (i in 0 until parser.attributeCount) {
              if (parser.getAttributeName(i).endsWith("href")) {
                href = parser.getAttributeValue(i)
                break
              }
            }
            if (!href.isNullOrBlank() && (typeAttr == "note" || href.startsWith("#"))) {
              hrefTarget = href.substringAfter('#').trimStart('#')
            }
          }
          stack.add(OpenTag(tag = tag, start = sb.length, noteTargetId = hrefTarget))
        }
        XmlPullParser.END_TAG -> {
          depth--
          if (depth == 0) break
          val tag = parser.name.substringAfterLast(':').lowercase()
          val open = stack.removeLastOrNull() ?: continue
          val end = sb.length
          if (end > open.start) {
            when (open.tag) {
              "emphasis" -> spans.add(InlineSpan(open.start, end, InlineSpanType.ITALIC))
              "strong" -> spans.add(InlineSpan(open.start, end, InlineSpanType.BOLD))
              "strikethrough" -> spans.add(InlineSpan(open.start, end, InlineSpanType.STRIKETHROUGH))
              "sup" -> spans.add(InlineSpan(open.start, end, InlineSpanType.SUPERSCRIPT))
              "sub" -> spans.add(InlineSpan(open.start, end, InlineSpanType.SUBSCRIPT))
              "code" -> spans.add(InlineSpan(open.start, end, InlineSpanType.CODE))
              "a" ->
                if (!open.noteTargetId.isNullOrBlank()) {
                  spans.add(
                    InlineSpan(
                      start = open.start,
                      end = end,
                      type = InlineSpanType.FOOTNOTE_REF,
                      targetId = open.noteTargetId,
                    )
                  )
                }
            }
          }
        }
        XmlPullParser.END_DOCUMENT -> break
      }
    }

    val raw = sb.toString()
    val lead = raw.indexOfFirst { !it.isWhitespace() }
    if (lead < 0) return RichBlock("", emptyList())
    val trail = raw.indexOfLast { !it.isWhitespace() } + 1
    val finalText = raw.substring(lead, trail)
    val adjustedSpans =
      spans.mapNotNull { sp ->
        val ns = (sp.start - lead).coerceIn(0, finalText.length)
        val ne = (sp.end - lead).coerceIn(0, finalText.length)
        if (ne > ns) sp.copy(start = ns, end = ne) else null
      }
    return RichBlock(finalText, adjustedSpans)
  }

  private fun readPoemRichBlock(parser: XmlPullParser): RichBlock {
    val sb = StringBuilder()
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.TEXT -> sb.append(parser.text)
        XmlPullParser.START_TAG -> depth++
        XmlPullParser.END_TAG -> {
          val tag = parser.name.substringAfterLast(':').lowercase()
          if (tag == "v" || tag == "stanza") sb.append("\n")
          depth--
        }
        XmlPullParser.END_DOCUMENT -> break
      }
    }
    return RichBlock(sb.toString().trim(), emptyList())
  }
}
