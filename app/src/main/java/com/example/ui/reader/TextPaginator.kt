package com.example.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle as ComposeParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.model.BookAnchorTarget
import com.example.data.model.BookChapter
import com.example.data.model.BookFootnote
import com.example.data.model.BookParagraph
import com.example.data.model.CssTextAlign
import com.example.data.model.InlineSpan
import com.example.data.model.InlineSpanType
import com.example.data.model.ParagraphCss
import com.example.data.model.ParagraphStyle
import com.example.data.prefs.ReaderPreferences
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UnboundedFamily
import kotlin.math.ceil

data class PageParagraph(
  val content: AnnotatedString,
  val style: ParagraphStyle,
  val isContinuation: Boolean = false,
  val css: ParagraphCss = ParagraphCss(),
  val footnoteIds: List<String> = emptyList(),
  val paragraphIndex: Int = 0,
)

data class ReaderPage(
  val globalIndex: Int,
  val chapterIndex: Int,
  val chapterTitle: String,
  val pageInChapter: Int,
  val totalPagesInChapter: Int,
  val paragraphs: List<PageParagraph>,
  val previewExcerpt: String,
  val footnotes: List<BookFootnote> = emptyList(),
)

object TextPaginator {

  const val TAG_FOOTNOTE_REF = "footnote_ref"
  const val TAG_CHAPTER_LINK = "chapter_link"
  private const val ROLE_TAG = "role"
  private const val ROLE_ACCENT = "accent"
  private const val ROLE_SECONDARY = "secondary"
  private val WHITESPACE_COLLAPSE_REGEX = Regex("\\s+")

  fun resolveFootnote(id: String, footnotes: Map<String, BookFootnote>): BookFootnote? {
    return footnotes[id] ?: footnotes[id.substringAfterLast('#')]
  }

  fun resolveAnchorTarget(
    targetId: String,
    anchorTargets: Map<String, BookAnchorTarget>,
  ): BookAnchorTarget? {
    if (anchorTargets.isEmpty() || targetId.isBlank()) return null
    anchorTargets[targetId]?.let { return it }
    anchorTargets[targetId.lowercase()]?.let { return it }

    val fileAndFrag = targetId.substringAfterLast('/')
    anchorTargets[fileAndFrag]?.let { return it }
    anchorTargets[fileAndFrag.lowercase()]?.let { return it }

    val frag = targetId.substringAfter('#', "")
    if (frag.isNotEmpty()) {
      anchorTargets[frag]?.let { return it }
      anchorTargets[frag.lowercase()]?.let { return it }
    }

    val filePath = targetId.substringBefore('#')
    val fileOnly = filePath.substringAfterLast('/')
    return anchorTargets[filePath]
      ?: anchorTargets[filePath.lowercase()]
      ?: anchorTargets[fileOnly]
      ?: anchorTargets[fileOnly.lowercase()]
  }

  /**
   * Theme-color-agnostic pagination producing discrete [PageParagraph] blocks + reserving
   * exact height for footnotes rendered at the bottom of paged screens.
   */
  fun paginate(
    chapters: List<BookChapter>,
    textMeasurer: TextMeasurer,
    widthPx: Int,
    heightPx: Int,
    prefs: ReaderPreferences,
    paragraphSpacingPx: Int,
    footnotes: Map<String, BookFootnote> = emptyMap(),
    footnoteChromePx: Int = 28,
    startingGlobalIndex: Int = 0,
  ): List<ReaderPage> {
    if (widthPx <= 64 || heightPx <= 64 || chapters.isEmpty()) return emptyList()

    val pages = mutableListOf<ReaderPage>()
    val constraints = Constraints(maxWidth = widthPx)
    val baseLineHeight = (prefs.fontSizeSp * prefs.lineHeightMultiplier).sp
    val bodyAlign = if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start

    val bodyStyle =
      TextStyle(
        fontFamily = prefs.fontOption.fontFamily,
        fontSize = prefs.fontSizeSp.sp,
        lineHeight = baseLineHeight,
        letterSpacing = prefs.letterSpacingSp.sp,
        textAlign = bodyAlign,
      )

    val footnoteStyle =
      TextStyle(
        fontFamily = prefs.fontOption.fontFamily,
        fontSize = (prefs.fontSizeSp * 0.80f).coerceIn(11f, 16f).sp,
        lineHeight = ((prefs.fontSizeSp * 0.80f).coerceIn(11f, 16f) * 1.38f).sp,
      )

    val singleNoteHeightCache = HashMap<String, Int>()
    fun measureFootnotesHeight(notes: Collection<BookFootnote>): Int {
      if (notes.isEmpty()) return 0
      var total = footnoteChromePx
      notes.forEachIndexed { index, fn ->
        if (index > 0) total += (footnoteChromePx / 3).coerceAtLeast(4)
        val noteHeight =
          singleNoteHeightCache.getOrPut(fn.id) {
            val markerPrefix =
              if (fn.marker.startsWith("[")) fn.marker else "[${fn.marker}]"
            val layout =
              textMeasurer.measure(
                text = AnnotatedString("$markerPrefix ${fn.text}"),
                style = footnoteStyle,
                constraints = constraints,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            layout.size.height
          }
        total += noteHeight
      }
      return total.coerceAtMost((heightPx * 0.36f).toInt())
    }

    for (chapter in chapters) {
      val chapterPages = mutableListOf<Pair<List<PageParagraph>, List<BookFootnote>>>()
      var currentParagraphs = mutableListOf<PageParagraph>()
      var currentPageFootnotes = LinkedHashMap<String, BookFootnote>()
      var currentFootnotesHeight = 0
      var usedHeight = 0

      fun commitPage() {
        if (currentParagraphs.isNotEmpty()) {
          chapterPages.add(currentParagraphs to currentPageFootnotes.values.toList())
          currentParagraphs = mutableListOf()
          currentPageFootnotes = LinkedHashMap()
          currentFootnotesHeight = 0
        }
        usedHeight = 0
      }

      chapter.paragraphs.forEachIndexed { pIdx, paragraph ->
        val marginTopPx =
          if (paragraph.css.marginTopDp > 0f)
            (paragraph.css.marginTopDp * (paragraphSpacingPx.coerceAtLeast(4) / prefs.paragraphSpacingDp.coerceAtLeast(1f))).toInt()
              .coerceAtMost(paragraphSpacingPx * 2 + 24)
          else 0

        var remainingStart = 0
        var isFirstSliceOfParagraph = true

        while (remainingStart < paragraph.text.length) {
          val remainingText = paragraph.text.substring(remainingStart)
          val remainingSpans =
            paragraph.spans.mapNotNull { sp ->
              val ns = (sp.start - remainingStart).coerceIn(0, remainingText.length)
              val ne = (sp.end - remainingStart).coerceIn(0, remainingText.length)
              if (ne > ns) sp.copy(start = ns, end = ne) else null
            }

          val isContinuation = !isFirstSliceOfParagraph
          val (styledBlock, blockStyle) =
            buildParagraphAnnotated(
              text = remainingText,
              style = paragraph.style,
              spans = remainingSpans,
              css = paragraph.css,
              prefs = prefs,
              bodyAlign = bodyAlign,
              isContinuation = isContinuation,
            )

          val layout =
            textMeasurer.measure(
              text = styledBlock,
              style = bodyStyle.merge(blockStyle),
              constraints = constraints,
            )

          val gapBefore =
            if (currentParagraphs.isNotEmpty()) (paragraphSpacingPx + if (isFirstSliceOfParagraph) marginTopPx else 0)
            else 0
          val firstLineBottom =
            if (layout.lineCount > 0) ceil(layout.getLineBottom(0)).toInt() else 0
          val remainingAvailable = heightPx - currentFootnotesHeight - usedHeight - gapBefore

          if (currentParagraphs.isNotEmpty() && firstLineBottom > remainingAvailable) {
            commitPage()
            continue
          }

          fun fittingLineForAvailable(availPx: Int): Int {
            val maxBottom = availPx.coerceAtLeast(firstLineBottom)
            var last = 0
            for (line in 0 until layout.lineCount) {
              if (ceil(layout.getLineBottom(line)).toInt() <= maxBottom) {
                last = line
              } else {
                break
              }
            }
            return last
          }

          var lastFittingLine = fittingLineForAvailable(remainingAvailable)
          var endOffset =
            layout.getLineEnd(lastFittingLine, visibleEnd = true).coerceAtLeast(1)

          // Check if this slice introduces footnote references that need footer space at bottom
          if (footnotes.isNotEmpty()) {
            val refAnnotations = styledBlock.getStringAnnotations(TAG_FOOTNOTE_REF, 0, endOffset)
            val newNotes =
              refAnnotations
                .mapNotNull { ann -> resolveFootnote(ann.item, footnotes) }
                .filter { fn -> !currentPageFootnotes.containsKey(fn.id) }
            if (newNotes.isNotEmpty()) {
              val combinedNotes = LinkedHashMap(currentPageFootnotes)
              newNotes.forEach { combinedNotes[it.id] = it }
              val combinedFooterHeight = measureFootnotesHeight(combinedNotes.values)
              val availWithNotes = heightPx - combinedFooterHeight - usedHeight - gapBefore
              if (currentParagraphs.isNotEmpty() && firstLineBottom > availWithNotes) {
                commitPage()
                continue
              }
              lastFittingLine = fittingLineForAvailable(availWithNotes)
              endOffset =
                layout.getLineEnd(lastFittingLine, visibleEnd = true).coerceAtLeast(1)
            }
          }

          val slice = styledBlock.subSequence(0, endOffset)
          val sliceHeight = ceil(layout.getLineBottom(lastFittingLine)).toInt()
          val sliceFootnoteIds =
            if (footnotes.isEmpty()) emptyList()
            else slice.getStringAnnotations(TAG_FOOTNOTE_REF, 0, slice.length)
              .mapNotNull { ann -> resolveFootnote(ann.item, footnotes)?.id }
              .distinct()

          sliceFootnoteIds.forEach { fnId ->
            resolveFootnote(fnId, footnotes)?.let { fn ->
              currentPageFootnotes[fn.id] = fn
            }
          }
          if (sliceFootnoteIds.isNotEmpty()) {
            currentFootnotesHeight = measureFootnotesHeight(currentPageFootnotes.values)
          }

          currentParagraphs.add(
            PageParagraph(
              content = slice,
              style = paragraph.style,
              isContinuation = isContinuation,
              css = paragraph.css,
              footnoteIds = sliceFootnoteIds,
              paragraphIndex = pIdx,
            )
          )
          usedHeight += gapBefore + sliceHeight

          val addedQuotePrefix =
            (paragraph.style == ParagraphStyle.QUOTE || paragraph.style == ParagraphStyle.EPIGRAPH) &&
              isFirstSliceOfParagraph &&
              !remainingText.startsWith("«")
          val rawConsumed =
            if (addedQuotePrefix) (endOffset - 1).coerceAtLeast(0) else endOffset
          remainingStart += rawConsumed
          isFirstSliceOfParagraph = false

          while (remainingStart < paragraph.text.length && paragraph.text[remainingStart].isWhitespace()) {
            remainingStart++
          }

          val minNextLineNeeded = paragraphSpacingPx + firstLineBottom
          if (remainingStart < paragraph.text.length ||
            usedHeight + minNextLineNeeded > (heightPx - currentFootnotesHeight)
          ) {
            commitPage()
          }
        }
      }

      commitPage()

      if (chapterPages.isEmpty()) {
        chapterPages.add(
          listOf(
            PageParagraph(
              content = AnnotatedString(chapter.title),
              style = ParagraphStyle.HEADING,
              isContinuation = false,
            )
          ) to emptyList()
        )
      }

      chapterPages.forEachIndexed { pageIdx, (paraList, pageNotes) ->
        val joined = paraList.joinToString(" ") { it.content.text }
        val cleanPreview = joined.replace(WHITESPACE_COLLAPSE_REGEX, " ").trim().take(120)
        pages.add(
          ReaderPage(
            globalIndex = startingGlobalIndex + pages.size,
            chapterIndex = chapter.index,
            chapterTitle = chapter.title,
            pageInChapter = pageIdx,
            totalPagesInChapter = chapterPages.size,
            paragraphs = paraList,
            previewExcerpt = cleanPreview,
            footnotes = pageNotes,
          )
        )
      }
    }

    return pages
  }

  /**
   * Fast per-paragraph live styling pass (<0.1 ms) so theme color swaps, word spacing,
   * alignment, and first-line indents update immediately while preserving CSS & footnote annotations.
   */
  fun applyLiveStylingToParagraph(
    paragraph: PageParagraph,
    prefs: ReaderPreferences,
    secondaryColor: Color,
    accentColor: Color,
  ): AnnotatedString {
    val content = paragraph.content
    val cssAlign =
      when (paragraph.css.textAlign) {
        CssTextAlign.LEFT -> TextAlign.Start
        CssTextAlign.CENTER -> TextAlign.Center
        CssTextAlign.RIGHT -> TextAlign.End
        CssTextAlign.JUSTIFY -> TextAlign.Justify
        null -> null
      }
    val bodyAlign = cssAlign ?: if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start
    val indentSp =
      when {
        paragraph.isContinuation -> 0.sp
        paragraph.css.hasFirstLineIndent == false -> 0.sp
        paragraph.css.firstLineIndentEm != null ->
          (paragraph.css.firstLineIndentEm * prefs.fontSizeSp).coerceIn(0f, 40f).sp
        else -> prefs.firstLineIndentSp.sp
      }

    val wordSpacingActive =
      prefs.wordSpacingSp > 0.01f &&
        paragraph.style != ParagraphStyle.HEADING &&
        paragraph.style != ParagraphStyle.CODE
    val spaceTracking = (prefs.letterSpacingSp + prefs.wordSpacingSp).sp

    return buildAnnotatedString {
      append(content.text)

      content.spanStyles.forEach { range ->
        if (range.item.letterSpacing != androidx.compose.ui.unit.TextUnit.Unspecified &&
          range.end - range.start == 1 &&
          content.text[range.start] == ' '
        ) {
          // Skip stale baked space tracking; re-apply fresh below if active
        } else {
          addStyle(range.item, range.start, range.end)
        }
      }

      content.getStringAnnotations(TAG_FOOTNOTE_REF, 0, content.length).forEach { ann ->
        addStringAnnotation(TAG_FOOTNOTE_REF, ann.item, ann.start, ann.end)
      }
      content.getStringAnnotations(TAG_CHAPTER_LINK, 0, content.length).forEach { ann ->
        addStringAnnotation(TAG_CHAPTER_LINK, ann.item, ann.start, ann.end)
      }

      content.getStringAnnotations(ROLE_TAG, 0, content.length).forEach { ann ->
        when (ann.item) {
          ROLE_ACCENT -> addStyle(SpanStyle(color = accentColor), ann.start, ann.end)
          ROLE_SECONDARY -> addStyle(SpanStyle(color = secondaryColor), ann.start, ann.end)
        }
      }

      when (paragraph.style) {
        ParagraphStyle.BODY ->
          addStyle(
            ComposeParagraphStyle(
              textAlign = bodyAlign,
              textIndent = TextIndent(firstLine = indentSp),
            ),
            0,
            content.length,
          )
        else ->
          content.paragraphStyles.forEach { range ->
            addStyle(range.item, range.start, range.end)
          }
      }

      if (wordSpacingActive) {
        val text = content.text
        var i = 0
        while (i < text.length) {
          if (text[i] == ' ') {
            addStyle(SpanStyle(letterSpacing = spaceTracking), i, i + 1)
          }
          i++
        }
      }
    }
  }

  /**
   * Builds a rich styled [AnnotatedString] directly from a [BookParagraph] for Scroll ("Свиток") mode.
   */
  fun buildScrollParagraphAnnotated(
    paragraph: BookParagraph,
    prefs: ReaderPreferences,
    secondaryColor: Color,
    accentColor: Color,
  ): AnnotatedString {
    val bodyAlign = if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start
    val (rawAnnotated, _) =
      buildParagraphAnnotated(
        text = paragraph.text,
        style = paragraph.style,
        spans = paragraph.spans,
        css = paragraph.css,
        prefs = prefs,
        bodyAlign = bodyAlign,
        isContinuation = false,
      )
    return applyLiveStylingToParagraph(
      paragraph =
        PageParagraph(
          content = rawAnnotated,
          style = paragraph.style,
          isContinuation = false,
          css = paragraph.css,
        ),
      prefs = prefs,
      secondaryColor = secondaryColor,
      accentColor = accentColor,
    )
  }

  fun applyWordSpacing(
    text: String,
    letterSpacingSp: Float,
    wordSpacingSp: Float,
  ): AnnotatedString {
    if (wordSpacingSp <= 0.01f) return AnnotatedString(text)
    val spaceTracking = (letterSpacingSp + wordSpacingSp).sp
    return buildAnnotatedString {
      append(text)
      var i = 0
      while (i < text.length) {
        if (text[i] == ' ') {
          addStyle(SpanStyle(letterSpacing = spaceTracking), i, i + 1)
        }
        i++
      }
    }
  }

  private fun buildParagraphAnnotated(
    text: String,
    style: ParagraphStyle,
    spans: List<InlineSpan>,
    css: ParagraphCss,
    prefs: ReaderPreferences,
    bodyAlign: TextAlign,
    isContinuation: Boolean,
  ): Pair<AnnotatedString, TextStyle> {
    val cssAlign =
      when (css.textAlign) {
        CssTextAlign.LEFT -> TextAlign.Start
        CssTextAlign.CENTER -> TextAlign.Center
        CssTextAlign.RIGHT -> TextAlign.End
        CssTextAlign.JUSTIFY -> TextAlign.Justify
        null -> null
      }

    fun AnnotatedString.Builder.applyInlineSpans(offsetShift: Int, baseLength: Int) {
      for (sp in spans) {
        val s = (sp.start + offsetShift).coerceIn(0, baseLength)
        val e = (sp.end + offsetShift).coerceIn(0, baseLength)
        if (e <= s) continue
        when (sp.type) {
          InlineSpanType.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
          InlineSpanType.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, e)
          InlineSpanType.UNDERLINE ->
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), s, e)
          InlineSpanType.STRIKETHROUGH ->
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), s, e)
          InlineSpanType.CODE ->
            addStyle(SpanStyle(fontFamily = JetBrainsMonoFamily, fontSize = 0.88.em), s, e)
          InlineSpanType.SUPERSCRIPT ->
            addStyle(
              SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em),
              s,
              e,
            )
          InlineSpanType.SUBSCRIPT ->
            addStyle(
              SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em),
              s,
              e,
            )
          InlineSpanType.SMALL_CAPS ->
            addStyle(SpanStyle(fontFeatureSettings = "smcp"), s, e)
          InlineSpanType.FOOTNOTE_REF -> {
            val target = sp.targetId
            if (!target.isNullOrBlank()) {
              addStringAnnotation(TAG_FOOTNOTE_REF, target, s, e)
            }
            addStringAnnotation(ROLE_TAG, ROLE_ACCENT, s, e)
            addStyle(
              SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 0.76.em,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
              ),
              s,
              e,
            )
          }
          InlineSpanType.CHAPTER_LINK -> {
            val target = sp.targetId
            if (!target.isNullOrBlank()) {
              addStringAnnotation(TAG_CHAPTER_LINK, target, s, e)
            }
            addStringAnnotation(ROLE_TAG, ROLE_ACCENT, s, e)
            addStyle(
              SpanStyle(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
              ),
              s,
              e,
            )
          }
          InlineSpanType.STYLED -> {
            val spColor = sp.colorHex?.takeIf { !isNeutralDarkOrLight(it) }?.let { Color(it) } ?: Color.Unspecified
            val spBg = sp.backgroundColorHex?.let { Color(it) } ?: Color.Unspecified
            val spSize = sp.fontSizeScale?.coerceIn(0.6f, 2.2f)?.em ?: TextUnit.Unspecified
            addStyle(
              SpanStyle(
                color = spColor,
                background = spBg,
                fontSize = spSize,
                fontFamily = if (sp.isMonospace) JetBrainsMonoFamily else null,
              ),
              s,
              e,
            )
          }
        }
      }
    }

    return when (style) {
      ParagraphStyle.HEADING -> {
        val align = cssAlign ?: TextAlign.Start
        val ts =
          TextStyle(
            fontFamily = UnboundedFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (prefs.fontSizeSp * 1.32f).coerceAtMost(36f).sp,
            lineHeight = (prefs.fontSizeSp * 1.6f).coerceAtMost(44f).sp,
            textAlign = align,
          )
        val ann = buildAnnotatedString {
          pushStringAnnotation(ROLE_TAG, ROLE_ACCENT)
          pushStyle(ComposeParagraphStyle(textAlign = align))
          pushStyle(
            SpanStyle(
              fontFamily = UnboundedFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 1.32.em,
            )
          )
          append(text)
          pop()
          pop()
          pop()
          applyInlineSpans(0, length)
        }
        ann to ts
      }
      ParagraphStyle.SUBHEADING -> {
        val align = cssAlign ?: TextAlign.Start
        val ts =
          TextStyle(
            fontFamily = prefs.fontOption.fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (prefs.fontSizeSp * 1.15f).sp,
            lineHeight = (prefs.fontSizeSp * 1.45f).sp,
            textAlign = align,
          )
        val ann = buildAnnotatedString {
          pushStyle(ComposeParagraphStyle(textAlign = align))
          pushStyle(
            SpanStyle(
              fontWeight = FontWeight.SemiBold,
              fontSize = 1.15.em,
            )
          )
          append(text)
          pop()
          pop()
          applyInlineSpans(0, length)
        }
        ann to ts
      }
      ParagraphStyle.QUOTE, ParagraphStyle.EPIGRAPH -> {
        val align = cssAlign ?: if (style == ParagraphStyle.EPIGRAPH) TextAlign.End else TextAlign.Start
        val addedPrefix = !isContinuation && !text.startsWith("«")
        val display = if (addedPrefix) "«$text" else text
        val indentVal =
          if (align == TextAlign.End) TextIndent.None else TextIndent(firstLine = 14.sp, restLine = 14.sp)
        val ts =
          TextStyle(
            fontFamily = prefs.fontOption.fontFamily,
            fontStyle = FontStyle.Italic,
            fontSize = (prefs.fontSizeSp * 0.95f).sp,
            lineHeight = (prefs.fontSizeSp * prefs.lineHeightMultiplier).sp,
            textAlign = align,
          )
        val ann = buildAnnotatedString {
          pushStringAnnotation(ROLE_TAG, ROLE_SECONDARY)
          pushStyle(ComposeParagraphStyle(textAlign = align, textIndent = indentVal))
          pushStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = 0.95.em))
          appendWordSpacedText(display, prefs.letterSpacingSp, prefs.wordSpacingSp)
          pop()
          pop()
          pop()
          applyInlineSpans(if (addedPrefix) 1 else 0, length)
        }
        ann to ts
      }
      ParagraphStyle.POEM -> {
        val align = cssAlign ?: TextAlign.Start
        val ts =
          TextStyle(
            fontFamily = prefs.fontOption.fontFamily,
            fontStyle = FontStyle.Italic,
            fontSize = prefs.fontSizeSp.sp,
            lineHeight = (prefs.fontSizeSp * prefs.lineHeightMultiplier).sp,
            textAlign = align,
          )
        val ann = buildAnnotatedString {
          pushStyle(
            ComposeParagraphStyle(
              textAlign = align,
              textIndent = TextIndent(firstLine = 20.sp, restLine = 20.sp),
            )
          )
          pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
          appendWordSpacedText(text, prefs.letterSpacingSp, prefs.wordSpacingSp)
          pop()
          pop()
          applyInlineSpans(0, length)
        }
        ann to ts
      }
      ParagraphStyle.CODE -> {
        val ts =
          TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = (prefs.fontSizeSp * 0.85f).sp,
            lineHeight = (prefs.fontSizeSp * 1.32f).sp,
            textAlign = TextAlign.Start,
          )
        val ann = buildAnnotatedString {
          pushStringAnnotation(ROLE_TAG, ROLE_ACCENT)
          pushStyle(
            ComposeParagraphStyle(
              textAlign = TextAlign.Start,
              textIndent = TextIndent(firstLine = 12.sp, restLine = 12.sp),
            )
          )
          pushStyle(SpanStyle(fontFamily = JetBrainsMonoFamily, fontSize = 0.85.em))
          append(text)
          pop()
          pop()
          pop()
        }
        ann to ts
      }
      ParagraphStyle.BODY -> {
        val align = cssAlign ?: bodyAlign
        val indent =
          when {
            isContinuation -> 0.sp
            css.hasFirstLineIndent == false -> 0.sp
            css.firstLineIndentEm != null -> (css.firstLineIndentEm * prefs.fontSizeSp).coerceIn(0f, 40f).sp
            else -> prefs.firstLineIndentSp.sp
          }
        val scale = css.fontScale.coerceIn(0.7f, 1.8f)
        val explicitColor = css.colorHex?.takeIf { !isNeutralDarkOrLight(it) }?.let { Color(it) } ?: Color.Unspecified
        val ts =
          TextStyle(
            fontFamily = if (css.isMonospace) JetBrainsMonoFamily else prefs.fontOption.fontFamily,
            fontSize = (prefs.fontSizeSp * scale).sp,
            lineHeight = (prefs.fontSizeSp * scale * prefs.lineHeightMultiplier).sp,
            letterSpacing = prefs.letterSpacingSp.sp,
            fontWeight = if (css.isBold == true) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (css.isItalic == true) FontStyle.Italic else FontStyle.Normal,
            textAlign = align,
          )
        val ann = buildAnnotatedString {
          pushStyle(ComposeParagraphStyle(textAlign = align, textIndent = TextIndent(firstLine = indent)))
          if (css.isBold == true ||
            css.isItalic == true ||
            scale != 1f ||
            css.isSmallCaps ||
            css.isMonospace ||
            explicitColor != Color.Unspecified
          ) {
            pushStyle(
              SpanStyle(
                color = explicitColor,
                fontFamily = if (css.isMonospace) JetBrainsMonoFamily else null,
                fontWeight = if (css.isBold == true) FontWeight.Bold else null,
                fontStyle = if (css.isItalic == true) FontStyle.Italic else null,
                fontSize = if (scale != 1f) scale.em else TextUnit.Unspecified,
                fontFeatureSettings = if (css.isSmallCaps) "smcp" else null,
              )
            )
            appendWordSpacedText(text, prefs.letterSpacingSp, prefs.wordSpacingSp)
            pop()
          } else {
            appendWordSpacedText(text, prefs.letterSpacingSp, prefs.wordSpacingSp)
          }
          pop()
          applyInlineSpans(0, length)
        }
        ann to ts
      }
    }
  }

  private fun isNeutralDarkOrLight(argb: Long): Boolean {
    val r = ((argb ushr 16) and 0xFF).toInt()
    val g = ((argb ushr 8) and 0xFF).toInt()
    val b = (argb and 0xFF).toInt()
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val isGray = (max - min) <= 18
    return isGray && (max <= 55 || min >= 220)
  }

  private fun AnnotatedString.Builder.appendWordSpacedText(
    text: String,
    letterSpacingSp: Float,
    wordSpacingSp: Float,
  ) {
    val start = length
    append(text)
    if (wordSpacingSp > 0.01f) {
      val spaceTracking = (letterSpacingSp + wordSpacingSp).sp
      var i = 0
      while (i < text.length) {
        if (text[i] == ' ') {
          addStyle(SpanStyle(letterSpacing = spaceTracking), start + i, start + i + 1)
        }
        i++
      }
    }
  }
}
