package com.example.data.parser

import com.example.data.model.BookChapter
import com.example.data.model.BookFormat
import com.example.data.model.BookParagraph
import com.example.data.model.ParagraphStyle
import com.example.data.model.ParsedBook
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BookParser {

  suspend fun parseBook(file: File, format: BookFormat, fallbackTitle: String): ParsedBook =
    withContext(Dispatchers.IO) {
      when (format) {
        BookFormat.EPUB -> FileInputStream(file).use { EpubParser.parse(it, fallbackTitle) }
        BookFormat.FB2 -> FileInputStream(file).use { Fb2Parser.parse(it, fallbackTitle) }
        BookFormat.PDF -> {
          val meta = PdfBookHelper.inspectPdf(file)
          val chapters =
            List(meta.pageCount) { pageIdx ->
              BookChapter(
                index = pageIdx,
                title = "Страница ${pageIdx + 1}",
                paragraphs = listOf(BookParagraph("PDF страница ${pageIdx + 1}", ParagraphStyle.BODY)),
              )
            }
          ParsedBook(
            title = fallbackTitle,
            author = "PDF документ",
            description = "Страниц: ${meta.pageCount}",
            chapters = chapters,
            coverBytes = meta.coverBytes,
          )
        }
        BookFormat.TXT, BookFormat.HTML, BookFormat.MD -> {
          FileInputStream(file).use { TextFormatParser.parse(it, format, fallbackTitle) }
        }
      }
    }
}
