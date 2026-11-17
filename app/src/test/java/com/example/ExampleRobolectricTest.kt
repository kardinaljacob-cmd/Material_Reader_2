package com.example

import android.content.Context
import androidx.room.Room
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.BookDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.NoteEntity
import com.example.data.model.PageFlipAnimation
import com.example.data.model.ParagraphStyle
import com.example.data.parser.Fb2Parser
import com.example.data.parser.TextFormatParser
import com.example.data.prefs.ReaderPreferences
import com.example.ui.components.MainNavTab
import com.example.ui.reader.PageParagraph
import com.example.ui.reader.TextPaginator
import com.example.ui.screens.attachNoteHighlights
import com.example.ui.theme.ReaderFontOption
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Material Reader", appName)
  }

  @Test
  fun `fb2 parser decodes windows-1251 cyrillic and chapters`() {
    val fb2Xml =
      """
      <?xml version="1.0" encoding="windows-1251"?>
      <FictionBook>
        <description>
          <title-info>
            <author><first-name>Иван</first-name><last-name>Бунин</last-name></author>
            <book-title>Антоновские яблоки</book-title>
          </title-info>
        </description>
        <body>
          <section>
            <title><p>Глава 1</p></title>
            <p>Вспоминается мне ранняя погожая осень.</p>
          </section>
        </body>
      </FictionBook>
      """
        .trimIndent()
    val bytes = fb2Xml.toByteArray(Charset.forName("windows-1251"))
    val parsed = Fb2Parser.parse(ByteArrayInputStream(bytes), "Fallback")
    assertEquals("Антоновские яблоки", parsed.title)
    assertEquals("Иван Бунин", parsed.author)
    assertEquals(1, parsed.chapters.size)
    assertTrue(parsed.chapters[0].paragraphs.any { it.text.contains("погожая осень") })
  }

  @Test
  fun `markdown parser extracts title headings and quote`() {
    val md =
      """
      # Весенний дневник
      > Тишина звучит громче слов.
      ## День первый
      Солнце взошло над садом.
      """
        .trimIndent()
    val parsed =
      TextFormatParser.parse(
        ByteArrayInputStream(md.toByteArray(Charsets.UTF_8)),
        BookFormat.MD,
        "Fallback",
      )
    assertEquals("Весенний дневник", parsed.title)
    assertTrue(parsed.chapters.isNotEmpty())
  }

  @Test
  fun `bottom bar only shows library and appearance tabs`() {
    assertEquals(listOf(MainNavTab.LIBRARY, MainNavTab.APPEARANCE), MainNavTab.entries)
    assertEquals(4f, ReaderPreferences().paragraphSpacingDp, 0.01f)
    assertEquals(PageFlipAnimation.DEPTH_SLIDE, ReaderPreferences().pageAnimation)
  }

  @Test
  fun `page flip animations omit morph card and fonts include new cyrillic families`() {
    assertEquals(3, PageFlipAnimation.entries.size)
    assertFalse(PageFlipAnimation.entries.any { it.name == "MORPH_CARD" })

    assertEquals(26, ReaderFontOption.entries.size)
    assertTrue(ReaderFontOption.entries.any { it == ReaderFontOption.BALSAMIQ_SANS })
    assertTrue(ReaderFontOption.entries.any { it == ReaderFontOption.CAVEAT })
    assertTrue(ReaderFontOption.entries.any { it == ReaderFontOption.TINY5 })
    assertTrue(ReaderFontOption.entries.any { it == ReaderFontOption.PLAYFAIR_DISPLAY })
  }

  @Test
  fun `word spacing applies tracking spans only on spaces`() {
    val styled =
      TextPaginator.applyWordSpacing(
        text = "Привет мир чтения",
        letterSpacingSp = 0.2f,
        wordSpacingSp = 3.0f,
      )
    assertEquals("Привет мир чтения", styled.text)
    assertEquals(2, styled.spanStyles.size)
    styled.spanStyles.forEach { range ->
      assertEquals(" ", styled.text.substring(range.start, range.end))
    }

    val live =
      TextPaginator.applyLiveStylingToParagraph(
        paragraph =
          PageParagraph(
            content = AnnotatedString("Два слова"),
            style = ParagraphStyle.BODY,
          ),
        prefs = ReaderPreferences(wordSpacingSp = 2.5f, letterSpacingSp = 0.1f),
        secondaryColor = Color.Gray,
        accentColor = Color.Blue,
      )
    assertEquals(1, live.spanStyles.size)
  }

  @Test
  fun `css parser resolves classes align indent and epub footnotes`() {
    val css =
      """
      .epigraph { text-align: right; font-style: italic; text-indent: 0; }
      .noteref { vertical-align: super; font-weight: bold; }
      """.trimIndent()
    val html =
      """
      <html><body>
        <p class="epigraph">Море зовёт.</p>
        <p>Текст сноски<a epub:type="noteref" class="noteref" href="#fn1">[1]</a> и <strong>жирный</strong>.</p>
        <aside epub:type="footnote" id="fn1"><p>[1] Пояснение к сноске.</p></aside>
      </body></html>
      """.trimIndent()

    val sheet = com.example.data.parser.CssStyleParser.parseCss(css)
    val known = com.example.data.parser.CssStyleParser.extractFootnoteTargets(html)
    val parsed =
      com.example.data.parser.CssStyleParser.parseHtmlWithCss(
        html = html,
        defaultTitle = "Глава",
        externalCss = sheet,
        knownNoteTargets = known.keys,
      )

    assertEquals(2, parsed.chapter.paragraphs.size)
    val epigraph = parsed.chapter.paragraphs[0]
    assertEquals(com.example.data.model.CssTextAlign.RIGHT, epigraph.css.textAlign)
    assertEquals(false, epigraph.css.hasFirstLineIndent)
    assertEquals(true, epigraph.css.isItalic)

    val body = parsed.chapter.paragraphs[1]
    assertTrue(
      body.spans.any {
        it.type == com.example.data.model.InlineSpanType.FOOTNOTE_REF && it.targetId == "fn1"
      }
    )
    assertTrue(body.spans.any { it.type == com.example.data.model.InlineSpanType.BOLD })
    assertEquals("Пояснение к сноске.", parsed.footnotes["fn1"]?.text)
  }

  @Test
  fun `fb2 parser extracts notes body and note links`() {
    val fb2 =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <FictionBook xmlns:l="http://www.w3.org/1999/xlink">
        <description><title-info><book-title>Тест</book-title></title-info></description>
        <body>
          <section>
            <title><p>Глава 1</p></title>
            <p>Фраза с <emphasis>акцентом</emphasis> и ссылкой<a l:href="#n1" type="note">[1]</a>.</p>
          </section>
        </body>
        <body name="notes">
          <section id="n1">
            <title><p>[1]</p></title>
            <p>Текст примечания FB2.</p>
          </section>
        </body>
      </FictionBook>
      """.trimIndent()
    val parsed = Fb2Parser.parse(ByteArrayInputStream(fb2.toByteArray(Charsets.UTF_8)), "Fallback")
    assertEquals("Текст примечания FB2.", parsed.footnotes["n1"]?.text)
    val p = parsed.chapters[0].paragraphs[1]
    assertTrue(p.spans.any { it.type == com.example.data.model.InlineSpanType.ITALIC })
    assertTrue(
      p.spans.any {
        it.type == com.example.data.model.InlineSpanType.FOOTNOTE_REF && it.targetId == "n1"
      }
    )
  }

  @Test
  fun `markdown parser extracts inline footnotes`() {
    val md =
      """
      # Заголовок

      Обычный текст[^1] со **словом**.

      [^1]: Подробная сноска Markdown.
      """.trimIndent()
    val parsed =
      TextFormatParser.parse(
        ByteArrayInputStream(md.toByteArray(Charsets.UTF_8)),
        BookFormat.MD,
        "Fallback",
      )
    assertEquals("Подробная сноска Markdown.", parsed.footnotes["1"]?.text)
    assertTrue(
      parsed.chapters[0].paragraphs.any { para ->
        para.spans.any {
          it.type == com.example.data.model.InlineSpanType.FOOTNOTE_REF && it.targetId == "1"
        }
      }
    )
  }

  @Test
  fun `css descendant selectors do not leak and inline styles with lists parse accurately`() {
    val css =
      """
      .epigraph p { text-align: right; font-style: italic; margin-left: 2em; }
      .highlight { color: #1D4ED8; background-color: #FEF08A; font-size: 1.25em; }
      """.trimIndent()
    val html =
      """
      <html><body>
        <div class="epigraph"><p>Цитата справа.</p></div>
        <p>Обычный <span class="highlight">цветной</span> текст.</p>
        <ol><li>Первый пункт</li><li>Второй пункт</li></ol>
      </body></html>
      """.trimIndent()

    val sheet = com.example.data.parser.CssStyleParser.parseCss(css)
    val parsed =
      com.example.data.parser.CssStyleParser.parseHtmlWithCss(
        html = html,
        defaultTitle = "Тест",
        externalCss = sheet,
      )

    assertEquals(4, parsed.chapter.paragraphs.size)
    val epigraphPara = parsed.chapter.paragraphs[0]
    assertEquals(com.example.data.model.CssTextAlign.RIGHT, epigraphPara.css.textAlign)
    assertEquals(true, epigraphPara.css.isItalic)
    assertTrue(epigraphPara.css.marginStartDp > 0f)

    val regularPara = parsed.chapter.paragraphs[1]
    assertEquals(null, regularPara.css.textAlign)
    val styledSpan =
      regularPara.spans.firstOrNull {
        it.type == com.example.data.model.InlineSpanType.STYLED
      }
    assertTrue(styledSpan != null)
    assertEquals(0xFF1D4ED8L, styledSpan?.colorHex)
    assertEquals(0xFFFEF08AL, styledSpan?.backgroundColorHex)
    assertEquals(1.25f, styledSpan?.fontSizeScale ?: 0f, 0.01f)

    assertTrue(parsed.chapter.paragraphs[2].text.startsWith("1. "))
    assertTrue(parsed.chapter.paragraphs[3].text.startsWith("2. "))
  }

  @Test
  fun `epub parser indexes chapter links and anchor targets across files`() {
    val baos = java.io.ByteArrayOutputStream()
    java.util.zip.ZipOutputStream(baos).use { zos ->
      fun addEntry(name: String, content: String) {
        zos.putNextEntry(java.util.zip.ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
      }
      addEntry(
        "META-INF/container.xml",
        """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
      )
      addEntry(
        "OEBPS/content.opf",
        """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Книга</dc:title><dc:creator>Автор</dc:creator></metadata><manifest><item id="c1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""",
      )
      addEntry(
        "OEBPS/text/ch1.xhtml",
        """<html><body><h1>Оглавление</h1><p>Перейти к <a href="ch2.xhtml#sec_two">Разделу 2</a>.</p></body></html>""",
      )
      addEntry(
        "OEBPS/text/ch2.xhtml",
        """<html><body><h1>Глава 2</h1><p>Вступление.</p><h2 id="sec_two">Раздел 2</h2><p>Содержимое раздела.</p></body></html>""",
      )
    }

    val parsed =
      com.example.data.parser.EpubParser.parse(ByteArrayInputStream(baos.toByteArray()), "Fallback")
    assertEquals(2, parsed.chapters.size)

    val tocPara = parsed.chapters[0].paragraphs[1]
    val linkSpan =
      tocPara.spans.firstOrNull { it.type == com.example.data.model.InlineSpanType.CHAPTER_LINK }
    assertEquals("OEBPS/text/ch2.xhtml#sec_two", linkSpan?.targetId)

    val resolvedTarget =
      TextPaginator.resolveAnchorTarget(linkSpan!!.targetId!!, parsed.anchorTargets)
    assertEquals(1, resolvedTarget?.chapterIndex)
    assertEquals(2, resolvedTarget?.paragraphIndex)
  }

  @Test
  fun `notes dao inserts updates deletes and cascades cleanup`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val dao = db.bookDao()
      val bookId =
        dao.insertBook(
          BookEntity(
            title = "Тестовая книга",
            author = "Автор",
            formatName = BookFormat.TXT.name,
            filePath = "/tmp/test.txt",
          )
        )
      val noteId =
        dao.insertNote(
          NoteEntity(
            bookId = bookId,
            chapterIndex = 0,
            chapterTitle = "Глава 1",
            pageIndex = 2,
            selectedText = "важная цитата",
            noteText = "Первая мысль",
            colorIndex = 1,
          )
        )
      var notes = dao.observeNotes(bookId).first()
      assertEquals(1, notes.size)
      assertEquals("Первая мысль", notes[0].noteText)

      dao.updateNote(noteId, "Обновлённая мысль", 2)
      notes = dao.observeNotes(bookId).first()
      assertEquals("Обновлённая мысль", notes[0].noteText)
      assertEquals(2, notes[0].colorIndex)

      dao.deleteNotesForBook(bookId)
      notes = dao.observeNotes(bookId).first()
      assertTrue(notes.isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun `attachNoteHighlights highlights matching quote and adds link`() {
    val note =
      NoteEntity(
        id = 7L,
        bookId = 1L,
        chapterIndex = 0,
        chapterTitle = "Глава 1",
        pageIndex = 0,
        selectedText = "ранняя погожая осень",
        noteText = "Любимое место",
        colorIndex = 0,
      )
    val highlighted =
      attachNoteHighlights(
        source = AnnotatedString("Вспоминается мне ранняя погожая осень в саду."),
        chapterNotes = listOf(note),
        onNoteClick = {},
      )
    assertEquals(1, highlighted.spanStyles.size)
    assertEquals(1, highlighted.getLinkAnnotations(0, highlighted.length).size)
  }
}
