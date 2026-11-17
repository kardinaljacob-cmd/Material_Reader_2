package com.example.data.parser

import com.example.data.model.BookChapter
import com.example.data.model.BookFootnote
import com.example.data.model.BookParagraph
import com.example.data.model.CssTextAlign
import com.example.data.model.InlineSpan
import com.example.data.model.InlineSpanType
import com.example.data.model.ParagraphCss
import com.example.data.model.ParagraphStyle
import java.net.URLDecoder

/**
 * Full-featured, zero-dependency CSS + XHTML/HTML parser supporting EPUB/HTML stylesheets,
 * descendant & compound selectors, inline/block CSS styles, lists, semantic formatting,
 * EPUB2/EPUB3/HTML footnotes, and internal chapter/anchor hyperlinks.
 */
object CssStyleParser {

  private val CSS_COMMENT_REGEX = Regex("""/\*[\s\S]*?\*/""")
  private val CSS_BLOCK_REGEX = Regex("""([^{}@]+)\{([^{}]*)\}""")
  private val WHITESPACE_REGEX = Regex("\\s+")
  private val MULTI_SPACE_REGEX = Regex(" {2,}")
  private val CRLF_TAB_REGEX = Regex("[\\r\\n\\t]+")
  private val STYLE_TAG_REGEX = Regex("""<style[^>]*>([\s\S]*?)</style>""", RegexOption.IGNORE_CASE)
  private val BODY_TAG_REGEX = Regex("""<body[^>]*>([\s\S]*?)</body>""", RegexOption.IGNORE_CASE)
  private val HIDDEN_ELEMENTS_REGEX =
    Regex("""<(script|style|head|svg)[^>]*>[\s\S]*?</\1>""", RegexOption.IGNORE_CASE)
  private val HTML_TOKEN_REGEX = Regex("""<(/?)([a-zA-Z0-9:_-]+)([^>]*)>|([^<]+)""")
  private val BLOCK_NOTE_REGEX =
    Regex("""<(aside|div|section|li|p)([^>]*)>([\s\S]*?)</\1>""", RegexOption.IGNORE_CASE)
  private val LEADING_MARKER_REGEX = Regex("""^(\[\d+]|[*†‡]+|\d+[.)]|\d+\b)""")
  private val TITLE_TAG_REGEX = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
  private val NOTEREF_LABEL_REGEX = Regex("""\[?\d+\]?|\*+|†+|‡+""")
  private val COMBINATOR_SPLIT_REGEX = Regex("""[\s>+~]+""")
  private val RGB_COLOR_REGEX = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)""")

  private val ATTR_ID_REGEX = Regex("""(?:^|\s)id\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_CLASS_REGEX = Regex("""(?:^|\s)class\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_STYLE_REGEX = Regex("""(?:^|\s)style\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_EPUB_TYPE_REGEX = Regex("""(?:^|\s)epub:type\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_TYPE_REGEX = Regex("""(?:^|\s)type\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_HREF_REGEX = Regex("""(?:^|\s)href\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
  private val ATTR_LHREF_REGEX = Regex("""(?:^|\s)l:href\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

  private val NAMED_COLORS =
    mapOf(
      "black" to 0xFF000000L,
      "white" to 0xFFFFFFFFL,
      "red" to 0xFFB91C1CL,
      "crimson" to 0xFFDC143CL,
      "darkred" to 0xFF8B0000L,
      "maroon" to 0xFF800000L,
      "blue" to 0xFF1D4ED8L,
      "navy" to 0xFF000080L,
      "royalblue" to 0xFF2563EBL,
      "green" to 0xFF15803DL,
      "darkgreen" to 0xFF006400L,
      "teal" to 0xFF0F766EL,
      "purple" to 0xFF7E22CEL,
      "indigo" to 0xFF4338CAL,
      "orange" to 0xFFC2410CL,
      "brown" to 0xFF78350FL,
      "olive" to 0xFF65A30DL,
      "gray" to 0xFF6B7280L,
      "grey" to 0xFF6B7280L,
      "darkgray" to 0xFF4B5563L,
      "darkgrey" to 0xFF4B5563L,
      "silver" to 0xFF9CA3AFL,
      "gold" to 0xFFD97706L,
      "yellow" to 0xFFFACC15L,
    )

  data class CssRule(
    val fontWeightBold: Boolean? = null,
    val fontStyleItalic: Boolean? = null,
    val underline: Boolean? = null,
    val lineThrough: Boolean? = null,
    val textAlign: CssTextAlign? = null,
    val textIndentEm: Float? = null,
    val hasTextIndent: Boolean? = null,
    val fontScale: Float? = null,
    val verticalAlignSuper: Boolean = false,
    val verticalAlignSub: Boolean = false,
    val smallCaps: Boolean = false,
    val uppercase: Boolean = false,
    val lowercase: Boolean = false,
    val isMonospace: Boolean? = null,
    val marginTopDp: Float? = null,
    val marginBottomDp: Float? = null,
    val marginStartDp: Float? = null,
    val displayNone: Boolean = false,
    val colorHex: Long? = null,
    val backgroundColorHex: Long? = null,
  ) {
    fun merge(other: CssRule): CssRule =
      CssRule(
        fontWeightBold = other.fontWeightBold ?: fontWeightBold,
        fontStyleItalic = other.fontStyleItalic ?: fontStyleItalic,
        underline = other.underline ?: underline,
        lineThrough = other.lineThrough ?: lineThrough,
        textAlign = other.textAlign ?: textAlign,
        textIndentEm = other.textIndentEm ?: textIndentEm,
        hasTextIndent = other.hasTextIndent ?: hasTextIndent,
        fontScale = other.fontScale ?: fontScale,
        verticalAlignSuper = other.verticalAlignSuper || verticalAlignSuper,
        verticalAlignSub = other.verticalAlignSub || verticalAlignSub,
        smallCaps = other.smallCaps || smallCaps,
        uppercase = other.uppercase || uppercase,
        lowercase = other.lowercase || lowercase,
        isMonospace = other.isMonospace ?: isMonospace,
        marginTopDp = other.marginTopDp ?: marginTopDp,
        marginBottomDp = other.marginBottomDp ?: marginBottomDp,
        marginStartDp = other.marginStartDp ?: marginStartDp,
        displayNone = other.displayNone || displayNone,
        colorHex = other.colorHex ?: colorHex,
        backgroundColorHex = other.backgroundColorHex ?: backgroundColorHex,
      )
  }

  data class DescendantRule(
    val ancestorChain: List<String>,
    val targetSelector: String,
    val rule: CssRule,
  )

  data class SelectorMatchNode(
    val tag: String,
    val classes: List<String>,
    val id: String?,
  )

  data class CssSheet(
    val rules: Map<String, CssRule> = emptyMap(),
    val descendantRules: List<DescendantRule> = emptyList(),
  ) {
    fun mergeWith(other: CssSheet): CssSheet {
      if (other.rules.isEmpty() && other.descendantRules.isEmpty()) return this
      if (rules.isEmpty() && descendantRules.isEmpty()) return other
      val mergedRules = LinkedHashMap(rules)
      other.rules.forEach { (k, v) -> mergedRules[k] = mergedRules[k]?.merge(v) ?: v }
      val mergedDesc =
        if (descendantRules.isEmpty()) other.descendantRules
        else if (other.descendantRules.isEmpty()) descendantRules
        else descendantRules + other.descendantRules
      return CssSheet(mergedRules, mergedDesc)
    }

    fun resolve(
      tag: String,
      classes: List<String>,
      id: String?,
      inlineStyle: String?,
      ancestors: List<SelectorMatchNode> = emptyList(),
    ): CssRule {
      val lowerTag = tag.lowercase()
      var result = rules[lowerTag] ?: CssRule()
      for (cls in classes) {
        val lowerCls = cls.lowercase()
        rules[".$lowerCls"]?.let { result = result.merge(it) }
        rules["$lowerTag.$lowerCls"]?.let { result = result.merge(it) }
      }
      if (!id.isNullOrBlank()) {
        rules["#${id.lowercase()}"]?.let { result = result.merge(it) }
      }

      if (descendantRules.isNotEmpty() && ancestors.isNotEmpty()) {
        for (dRule in descendantRules) {
          if (matchesSimpleSelector(dRule.targetSelector, lowerTag, classes, id) &&
            matchesAncestorChain(dRule.ancestorChain, ancestors)
          ) {
            result = result.merge(dRule.rule)
          }
        }
      }

      if (!inlineStyle.isNullOrBlank()) {
        result = result.merge(parseDeclarationBlock(inlineStyle))
      }
      return result
    }

    private fun matchesSimpleSelector(
      sel: String,
      tag: String,
      classes: List<String>,
      id: String?,
    ): Boolean {
      if (sel == "*") return true
      if (sel.startsWith("#")) return id != null && sel.substring(1).equals(id, ignoreCase = true)
      val dotIdx = sel.indexOf('.')
      if (dotIdx < 0) return sel == tag
      val tagPart = sel.substring(0, dotIdx)
      if (tagPart.isNotEmpty() && tagPart != tag) return false
      val classParts = sel.substring(dotIdx + 1).split('.')
      return classParts.all { req -> classes.any { it.equals(req, ignoreCase = true) } }
    }

    private fun matchesAncestorChain(
      chain: List<String>,
      ancestors: List<SelectorMatchNode>,
    ): Boolean {
      var chainIdx = chain.lastIndex
      for (i in ancestors.lastIndex downTo 0) {
        val node = ancestors[i]
        if (matchesSimpleSelector(chain[chainIdx], node.tag, node.classes, node.id)) {
          chainIdx--
          if (chainIdx < 0) return true
        }
      }
      return false
    }
  }

  data class HtmlParseResult(
    val chapter: BookChapter,
    val footnotes: Map<String, BookFootnote>,
    val anchorParagraphIndices: Map<String, Int> = emptyMap(),
  )

  fun parseCss(cssText: String): CssSheet {
    if (cssText.isBlank()) return CssSheet()
    val noComments = if (cssText.contains("/*")) cssText.replace(CSS_COMMENT_REGEX, " ") else cssText
    val rules = LinkedHashMap<String, CssRule>()
    val descendantRules = mutableListOf<DescendantRule>()

    for (match in CSS_BLOCK_REGEX.findAll(noComments)) {
      val selectorGroup = match.groupValues[1].trim()
      val declarations = match.groupValues[2].trim()
      if (selectorGroup.isEmpty() || declarations.isEmpty()) continue
      val parsedRule = parseDeclarationBlock(declarations)
      for (rawSel in selectorGroup.split(',')) {
        val sel = rawSel.trim().lowercase()
        if (sel.isEmpty()) continue
        val parts =
          sel
            .split(COMBINATOR_SPLIT_REGEX)
            .map { it.substringBefore('[').substringBefore(':').trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) continue
        if (parts.size == 1) {
          val token = parts[0]
          rules[token] = rules[token]?.merge(parsedRule) ?: parsedRule
        } else {
          descendantRules.add(
            DescendantRule(
              ancestorChain = parts.dropLast(1),
              targetSelector = parts.last(),
              rule = parsedRule,
            )
          )
        }
      }
    }
    return CssSheet(rules, descendantRules)
  }

  fun parseDeclarationBlock(block: String): CssRule {
    var bold: Boolean? = null
    var italic: Boolean? = null
    var underline: Boolean? = null
    var lineThrough: Boolean? = null
    var textAlign: CssTextAlign? = null
    var textIndentEm: Float? = null
    var hasTextIndent: Boolean? = null
    var fontScale: Float? = null
    var vSuper = false
    var vSub = false
    var smallCaps = false
    var uppercase = false
    var lowercase = false
    var isMonospace: Boolean? = null
    var marginTop: Float? = null
    var marginBottom: Float? = null
    var marginStart: Float? = null
    var displayNone = false
    var colorHex: Long? = null
    var backgroundColorHex: Long? = null

    for (decl in block.split(';')) {
      val colon = decl.indexOf(':')
      if (colon <= 0) continue
      val prop = decl.substring(0, colon).trim().lowercase()
      val value = decl.substring(colon + 1).replace("!important", "", ignoreCase = true).trim().lowercase()
      when (prop) {
        "display" -> if (value == "none") displayNone = true
        "visibility" -> if (value == "hidden" || value == "collapse") displayNone = true
        "font-weight" -> {
          val num = value.toIntOrNull()
          bold =
            when {
              value == "bold" || value == "bolder" -> true
              value == "normal" || value == "lighter" -> false
              num != null -> num >= 600
              else -> bold
            }
        }
        "font-style" -> {
          italic =
            when (value) {
              "italic", "oblique" -> true
              "normal" -> false
              else -> italic
            }
        }
        "font-family" -> {
          if (value.contains("monospace") ||
            value.contains("courier") ||
            value.contains("consolas") ||
            value.contains("menlo") ||
            value.contains("monaco")
          ) {
            isMonospace = true
          }
        }
        "text-decoration", "text-decoration-line" -> {
          if (value == "none") {
            underline = false
            lineThrough = false
          } else {
            if (value.contains("underline")) underline = true
            if (value.contains("line-through")) lineThrough = true
          }
        }
        "text-align" -> {
          textAlign =
            when (value) {
              "center" -> CssTextAlign.CENTER
              "right", "end" -> CssTextAlign.RIGHT
              "justify" -> CssTextAlign.JUSTIFY
              "left", "start" -> CssTextAlign.LEFT
              else -> textAlign
            }
        }
        "text-indent" -> {
          val emVal = parseEmSize(value)
          if (emVal != null) {
            textIndentEm = emVal
            hasTextIndent = emVal > 0.05f
          }
        }
        "font-size" -> {
          fontScale = parseFontScale(value) ?: fontScale
        }
        "vertical-align" -> {
          if (value == "super" || value == "top") vSuper = true
          if (value == "sub") vSub = true
        }
        "font-variant" -> if (value.contains("small-caps")) smallCaps = true
        "text-transform" -> {
          when (value) {
            "uppercase" -> uppercase = true
            "lowercase" -> lowercase = true
          }
        }
        "margin-top", "padding-top" -> marginTop = parseDpSpacing(value) ?: marginTop
        "margin-bottom", "padding-bottom" -> marginBottom = parseDpSpacing(value) ?: marginBottom
        "margin-left", "padding-left", "margin-inline-start", "padding-inline-start" ->
          marginStart = parseDpSpacing(value) ?: marginStart
        "margin", "padding" -> {
          val parts = value.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
          when (parts.size) {
            1 -> {
              val v = parseDpSpacing(parts[0])
              if (v != null) {
                marginTop = v
                marginBottom = v
                if (prop == "padding" && v > 0f) marginStart = v
              }
            }
            2 -> {
              marginTop = parseDpSpacing(parts[0]) ?: marginTop
              marginBottom = parseDpSpacing(parts[0]) ?: marginBottom
              val horiz = parseDpSpacing(parts[1])
              if (horiz != null && horiz > 0f) marginStart = horiz
            }
            3 -> {
              marginTop = parseDpSpacing(parts[0]) ?: marginTop
              val horiz = parseDpSpacing(parts[1])
              if (horiz != null && horiz > 0f) marginStart = horiz
              marginBottom = parseDpSpacing(parts[2]) ?: marginBottom
            }
            4 -> {
              marginTop = parseDpSpacing(parts[0]) ?: marginTop
              marginBottom = parseDpSpacing(parts[2]) ?: marginBottom
              val left = parseDpSpacing(parts[3])
              if (left != null && left > 0f) marginStart = left
            }
          }
        }
        "color" -> colorHex = parseCssColor(value) ?: colorHex
        "background-color", "background" -> {
          val parsedBg = parseCssColor(value.substringBefore(' '))
          if (parsedBg != null) backgroundColorHex = parsedBg
        }
      }
    }

    return CssRule(
      fontWeightBold = bold,
      fontStyleItalic = italic,
      underline = underline,
      lineThrough = lineThrough,
      textAlign = textAlign,
      textIndentEm = textIndentEm,
      hasTextIndent = hasTextIndent,
      fontScale = fontScale,
      verticalAlignSuper = vSuper,
      verticalAlignSub = vSub,
      smallCaps = smallCaps,
      uppercase = uppercase,
      lowercase = lowercase,
      isMonospace = isMonospace,
      marginTopDp = marginTop,
      marginBottomDp = marginBottom,
      marginStartDp = marginStart,
      displayNone = displayNone,
      colorHex = colorHex,
      backgroundColorHex = backgroundColorHex,
    )
  }

  fun parseHtmlWithCss(
    html: String,
    defaultTitle: String,
    externalCss: CssSheet = CssSheet(),
    chapterHref: String = "",
    knownNoteTargets: Set<String> = emptySet(),
  ): HtmlParseResult {
    val inlineStyleBlocks =
      if (html.contains("<style", ignoreCase = true)) {
        STYLE_TAG_REGEX.findAll(html).joinToString("\n") { it.groupValues[1] }
      } else ""
    val sheet =
      if (inlineStyleBlocks.isBlank()) externalCss
      else externalCss.mergeWith(parseCss(inlineStyleBlocks))

    val htmlTitle = extractTitle(html)
    val body = BODY_TAG_REGEX.find(html)?.groupValues?.getOrNull(1) ?: html
    val cleanedBody = body.replace(HIDDEN_ELEMENTS_REGEX, "")

    val paragraphs = mutableListOf<BookParagraph>()
    val footnotes = linkedMapOf<String, BookFootnote>()
    val anchorParagraphIndices = linkedMapOf<String, Int>()
    var chapterTitle = ""

    val tokenRegex = HTML_TOKEN_REGEX
    val blockTags =
      setOf(
        "h1", "h2", "h3", "h4", "h5", "h6",
        "p", "blockquote", "pre", "li", "aside", "section", "div", "article", "header", "footer", "dt", "dd",
      )
    val inlineTags =
      setOf(
        "b", "strong", "i", "em", "cite", "u", "ins", "s", "del", "strike",
        "code", "tt", "kbd", "samp", "sup", "sub", "span", "a", "small", "mark", "big",
      )

    data class OpenElement(
      val tag: String,
      val id: String?,
      val classes: List<String>,
      val epubType: String,
      val css: CssRule,
      val isHidden: Boolean,
      val isFootnoteContainer: Boolean,
      val inlineStart: Int,
      val href: String?,
    )

    val stack = mutableListOf<OpenElement>()
    val listCounterStack = mutableListOf<Int>() // -1 for <ul>, >= 0 for <ol>
    var currentBlockTag: String? = null
    var currentBlockCss = CssRule()
    var currentBlockStyle = ParagraphStyle.BODY
    var currentText = StringBuilder()
    var currentSpans = mutableListOf<InlineSpan>()

    var activeFootnoteId: String? = null
    var activeFootnoteDepth = -1
    var activeFootnoteText = StringBuilder()
    var hiddenDepth = 0

    fun currentStyleForBlock(tag: String, classes: List<String>, epubType: String, css: CssRule): ParagraphStyle {
      val clsJoined = classes.joinToString(" ").lowercase()
      return when {
        tag == "h1" || tag == "h2" || clsJoined.contains("chapter-title") || (clsJoined.contains("title") && tag.startsWith("h")) ->
          ParagraphStyle.HEADING
        tag == "h3" || tag == "h4" || tag == "h5" || tag == "h6" || clsJoined.contains("subtitle") ->
          ParagraphStyle.SUBHEADING
        epubType.contains("epigraph") || clsJoined.contains("epigraph") -> ParagraphStyle.EPIGRAPH
        tag == "blockquote" || clsJoined.contains("quote") || clsJoined.contains("citation") ->
          ParagraphStyle.QUOTE
        clsJoined.contains("poem") || clsJoined.contains("verse") || clsJoined.contains("stanza") || clsJoined.contains("poetry") ->
          ParagraphStyle.POEM
        tag == "pre" || clsJoined.contains("code") || css.isMonospace == true -> ParagraphStyle.CODE
        else -> ParagraphStyle.BODY
      }
    }

    fun flushCurrentBlock() {
      val raw = currentText.toString()
      val trimmedLeading = raw.indexOfFirst { !it.isWhitespace() }
      if (trimmedLeading < 0) {
        currentText = StringBuilder()
        currentSpans = mutableListOf()
        currentBlockTag = null
        return
      }
      val trimmedTrailing = raw.indexOfLast { !it.isWhitespace() } + 1
      val finalText = raw.substring(trimmedLeading, trimmedTrailing)
      val adjustedSpans =
        currentSpans.mapNotNull { sp ->
          val ns = (sp.start - trimmedLeading).coerceIn(0, finalText.length)
          val ne = (sp.end - trimmedLeading).coerceIn(0, finalText.length)
          if (ne > ns) sp.copy(start = ns, end = ne) else null
        }

      if (finalText.isNotBlank()) {
        if (currentBlockStyle == ParagraphStyle.HEADING && chapterTitle.isEmpty()) {
          chapterTitle = finalText
        }
        paragraphs.add(
          BookParagraph(
            text = finalText,
            style = currentBlockStyle,
            spans = adjustedSpans,
            css =
              ParagraphCss(
                textAlign = currentBlockCss.textAlign,
                hasFirstLineIndent = currentBlockCss.hasTextIndent,
                firstLineIndentEm = currentBlockCss.textIndentEm,
                marginTopDp = currentBlockCss.marginTopDp ?: 0f,
                marginBottomDp = currentBlockCss.marginBottomDp ?: 0f,
                marginStartDp = currentBlockCss.marginStartDp ?: 0f,
                fontScale = currentBlockCss.fontScale ?: 1f,
                isBold = currentBlockCss.fontWeightBold,
                isItalic = currentBlockCss.fontStyleItalic,
                isSmallCaps = currentBlockCss.smallCaps,
                isMonospace = currentBlockCss.isMonospace == true,
                colorHex = currentBlockCss.colorHex,
              ),
          )
        )
      }
      currentText = StringBuilder()
      currentSpans = mutableListOf()
      currentBlockTag = null
      currentBlockCss = CssRule()
      currentBlockStyle = ParagraphStyle.BODY
    }

    fun startBlock(tag: String, classes: List<String>, epubType: String, css: CssRule) {
      if (currentText.isNotBlank()) flushCurrentBlock()
      var inheritedCss = CssRule()
      var inheritedSemantic = ParagraphStyle.BODY
      for (ancestor in stack) {
        if (ancestor.tag in blockTags) {
          inheritedCss = inheritedCss.merge(ancestor.css)
          val ancestorStyle =
            currentStyleForBlock(ancestor.tag, ancestor.classes, ancestor.epubType, ancestor.css)
          if (ancestorStyle != ParagraphStyle.BODY) {
            inheritedSemantic = ancestorStyle
          }
        }
      }
      val mergedCss = inheritedCss.merge(css)
      val ownSemantic = currentStyleForBlock(tag, classes, epubType, mergedCss)
      currentBlockTag = tag
      currentBlockCss = mergedCss
      currentBlockStyle =
        if (ownSemantic != ParagraphStyle.BODY) ownSemantic else inheritedSemantic
    }

    for (token in tokenRegex.findAll(cleanedBody)) {
      val textChunk = token.groupValues[4]
      if (textChunk.isNotEmpty()) {
        if (hiddenDepth > 0) continue
        val decoded = TextFormatParser.decodeEntities(textChunk).replace(CRLF_TAB_REGEX, " ")
        val collapsed =
          if (currentBlockStyle == ParagraphStyle.CODE) decoded
          else decoded.replace(MULTI_SPACE_REGEX, " ")

        if (activeFootnoteId != null) {
          if (collapsed.isNotBlank() || activeFootnoteText.isNotEmpty()) {
            if (activeFootnoteText.isNotEmpty() &&
              !activeFootnoteText.endsWith(" ") &&
              !collapsed.startsWith(" ")
            ) {
              activeFootnoteText.append(' ')
            }
            activeFootnoteText.append(collapsed.trim())
          }
        } else {
          if (currentBlockTag == null && collapsed.isNotBlank()) {
            startBlock("p", emptyList(), "", CssRule())
          }
          if (currentBlockTag != null) {
            if (currentText.isEmpty() && collapsed.isBlank()) continue
            val avoidDoubleSpace = currentText.endsWith(" ") && collapsed.startsWith(" ")
            val toAppend = if (avoidDoubleSpace) collapsed.drop(1) else collapsed
            currentText.append(toAppend)
          }
        }
        continue
      }

      val isClosing = token.groupValues[1] == "/"
      val rawTag = token.groupValues[2]
      val tag = rawTag.substringAfterLast(':').lowercase()
      val attrs = token.groupValues[3]
      val isSelfClosing = attrs.trimEnd().endsWith("/")

      if (!isClosing) {
        if (tag == "br") {
          if (activeFootnoteId != null) activeFootnoteText.append(' ')
          else if (currentBlockTag != null) currentText.append('\n')
          continue
        }
        if (tag == "hr") {
          flushCurrentBlock()
          paragraphs.add(
            BookParagraph(
              text = "• • •",
              style = ParagraphStyle.SUBHEADING,
              css = ParagraphCss(textAlign = CssTextAlign.CENTER, marginTopDp = 6f, marginBottomDp = 6f),
            )
          )
          continue
        }

        if (tag == "ul" && !isSelfClosing) {
          listCounterStack.add(-1)
        } else if (tag == "ol" && !isSelfClosing) {
          listCounterStack.add(0)
        }

        val hasAttrs = attrs.isNotBlank()
        val id = if (hasAttrs) extractAttr(attrs, "id") ?: extractAttr(attrs, "name") else null
        val classAttr = if (hasAttrs) extractAttr(attrs, "class") ?: "" else ""
        val classes =
          if (classAttr.isBlank()) emptyList()
          else classAttr.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val inlineStyle = if (hasAttrs) extractAttr(attrs, "style") else null
        val epubType =
          if (hasAttrs) (extractAttr(attrs, "epub:type") ?: extractAttr(attrs, "type") ?: "").lowercase()
          else ""
        val href =
          if (hasAttrs && tag == "a") (extractAttr(attrs, "href") ?: extractAttr(attrs, "l:href"))
          else null

        val ancestors =
          if (sheet.descendantRules.isNotEmpty() && stack.isNotEmpty()) {
            stack.map { SelectorMatchNode(it.tag, it.classes, it.id) }
          } else emptyList()
        val css = sheet.resolve(tag, classes, id, inlineStyle, ancestors)

        val elementHidden = css.displayNone || epubType.contains("pagebreak")
        if (elementHidden && !isSelfClosing) hiddenDepth++

        val clsJoined = classes.joinToString(" ").lowercase()
        val isFootnoteContainer =
          activeFootnoteId == null &&
            !id.isNullOrBlank() &&
            (epubType.contains("footnote") ||
              epubType.contains("endnote") ||
              epubType.contains("rearnote") ||
              clsJoined.contains("footnote") ||
              clsJoined.contains("endnote") ||
              knownNoteTargets.contains(id) ||
              (chapterHref.isNotBlank() && knownNoteTargets.contains("$chapterHref#$id")))

        if (!id.isNullOrBlank() && !isFootnoteContainer && activeFootnoteId == null) {
          val targetParaIndex =
            if (currentBlockTag != null && currentText.isBlank()) paragraphs.size
            else paragraphs.size
          anchorParagraphIndices.putIfAbsent(id, targetParaIndex)
        }

        if (isFootnoteContainer && !isSelfClosing) {
          flushCurrentBlock()
          activeFootnoteId = id
          activeFootnoteDepth = stack.size
          activeFootnoteText = StringBuilder()
        } else if (activeFootnoteId == null && tag in blockTags && !isSelfClosing) {
          val isWrapperContainer =
            (tag == "div" || tag == "section" || tag == "article") &&
              !clsJoined.contains("verse")
          if (!isWrapperContainer) {
            startBlock(tag, classes, epubType, css)
            if (tag == "li") {
              val prefix =
                if (listCounterStack.isNotEmpty()) {
                  val top = listCounterStack.last()
                  if (top >= 0) {
                    val next = top + 1
                    listCounterStack[listCounterStack.lastIndex] = next
                    "$next. "
                  } else {
                    "• "
                  }
                } else {
                  "• "
                }
              currentText.append(prefix)
            }
          } else if (currentText.isNotBlank()) {
            flushCurrentBlock()
          }
        }

        if (!isSelfClosing) {
          stack.add(
            OpenElement(
              tag = tag,
              id = id,
              classes = classes,
              epubType = epubType,
              css = css,
              isHidden = elementHidden,
              isFootnoteContainer = isFootnoteContainer,
              inlineStart = currentText.length,
              href = href,
            )
          )
        }
      } else {
        if ((tag == "ul" || tag == "ol") && listCounterStack.isNotEmpty()) {
          listCounterStack.removeAt(listCounterStack.lastIndex)
        }
        val openIdx = stack.indexOfLast { it.tag == tag }
        if (openIdx >= 0) {
          val open = stack.removeAt(openIdx)
          if (open.isHidden && hiddenDepth > 0) hiddenDepth--

          if (open.isFootnoteContainer && activeFootnoteId != null && stack.size <= activeFootnoteDepth) {
            val rawNote = activeFootnoteText.toString().replace(WHITESPACE_REGEX, " ").trim()
            if (rawNote.isNotEmpty()) {
              val noteId = activeFootnoteId!!
              val extractedMarker = extractLeadingMarker(rawNote)
              val cleanNoteBody =
                if (extractedMarker != null) rawNote.removePrefix(extractedMarker).trimStart(' ', '.', ':', ')')
                else rawNote
              val note =
                BookFootnote(
                  id = noteId,
                  marker = extractedMarker ?: noteId,
                  text = cleanNoteBody.ifBlank { rawNote },
                )
              footnotes[noteId] = note
              if (chapterHref.isNotBlank()) {
                footnotes["$chapterHref#$noteId"] = note
              }
            }
            activeFootnoteId = null
            activeFootnoteDepth = -1
            activeFootnoteText = StringBuilder()
            continue
          }

          if (activeFootnoteId != null) continue

          val endPos = currentText.length
          if (endPos > open.inlineStart && open.tag in inlineTags) {
            applyInlineSpansFromElement(
              tag = open.tag,
              classes = open.classes,
              epubType = open.epubType,
              css = open.css,
              href = open.href,
              chapterHref = chapterHref,
              knownNoteTargets = knownNoteTargets,
              start = open.inlineStart,
              end = endPos,
              text = currentText,
              outSpans = currentSpans,
              insideSup = stack.any { it.tag == "sup" || it.css.verticalAlignSuper },
            )
          }

          if (open.tag in blockTags && open.tag == currentBlockTag) {
            flushCurrentBlock()
          }
        }
      }
    }

    flushCurrentBlock()

    return HtmlParseResult(
      chapter =
        BookChapter(
          index = 0,
          title = chapterTitle.ifBlank { htmlTitle ?: defaultTitle },
          paragraphs = paragraphs,
        ),
      footnotes = footnotes,
      anchorParagraphIndices = anchorParagraphIndices,
    )
  }

  private fun applyInlineSpansFromElement(
    tag: String,
    classes: List<String>,
    epubType: String,
    css: CssRule,
    href: String?,
    chapterHref: String,
    knownNoteTargets: Set<String>,
    start: Int,
    end: Int,
    text: StringBuilder,
    outSpans: MutableList<InlineSpan>,
    insideSup: Boolean,
  ) {
    val clsJoined = classes.joinToString(" ").lowercase()

    if (tag == "b" || tag == "strong" || css.fontWeightBold == true) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.BOLD))
    }
    if (tag == "i" || tag == "em" || tag == "cite" || css.fontStyleItalic == true) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.ITALIC))
    }
    if (tag == "u" || tag == "ins" || css.underline == true) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.UNDERLINE))
    }
    if (tag == "s" || tag == "del" || tag == "strike" || css.lineThrough == true) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.STRIKETHROUGH))
    }
    if (tag == "code" || tag == "tt" || tag == "kbd" || tag == "samp" || css.isMonospace == true) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.CODE, isMonospace = true))
    }
    if (tag == "sup" || css.verticalAlignSuper) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.SUPERSCRIPT))
    }
    if (tag == "sub" || css.verticalAlignSub) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.SUBSCRIPT))
    }
    if (css.smallCaps) {
      outSpans.add(InlineSpan(start, end, InlineSpanType.SMALL_CAPS))
    }
    if (css.uppercase) {
      val sub = text.substring(start, end).uppercase()
      if (sub.length == end - start) {
        text.replace(start, end, sub)
      }
    } else if (css.lowercase) {
      val sub = text.substring(start, end).lowercase()
      if (sub.length == end - start) {
        text.replace(start, end, sub)
      }
    }

    val effectiveBg = css.backgroundColorHex ?: if (tag == "mark") 0x4DFFE066L else null
    val effectiveScale =
      css.fontScale ?: when (tag) {
        "small" -> 0.85f
        "big" -> 1.15f
        else -> null
      }
    if (css.colorHex != null || effectiveBg != null || effectiveScale != null) {
      outSpans.add(
        InlineSpan(
          start = start,
          end = end,
          type = InlineSpanType.STYLED,
          colorHex = css.colorHex,
          backgroundColorHex = effectiveBg,
          fontSizeScale = effectiveScale,
          isMonospace = css.isMonospace == true,
        )
      )
    }

    if (tag == "a" && !href.isNullOrBlank()) {
      val trimmedHref = href.trim()
      val lowerHref = trimmedHref.lowercase()
      if (!lowerHref.startsWith("http://") &&
        !lowerHref.startsWith("https://") &&
        !lowerHref.startsWith("mailto:") &&
        !lowerHref.startsWith("tel:") &&
        !lowerHref.startsWith("javascript:")
      ) {
        val resolvedHref = resolveRelativeHref(chapterHref, trimmedHref)
        val rawFragment = trimmedHref.substringAfter('#', "")
        val snippet = text.substring(start, end).trim()
        val isExplicitNoteref =
          epubType.contains("noteref") ||
            epubType.contains("footnote") ||
            clsJoined.contains("noteref") ||
            clsJoined.contains("footnote") ||
            clsJoined.contains("fn-ref")
        val matchesKnownNote =
          (rawFragment.isNotEmpty() && knownNoteTargets.contains(rawFragment)) ||
            knownNoteTargets.contains(resolvedHref)
        val looksLikeShortNoteMarker =
          (trimmedHref.startsWith("#") ||
            lowerHref.contains("#note") ||
            lowerHref.contains("#fn") ||
            lowerHref.contains("#ftn")) &&
            (snippet.matches(NOTEREF_LABEL_REGEX) || (insideSup && snippet.length <= 6))

        if (isExplicitNoteref || matchesKnownNote || looksLikeShortNoteMarker) {
          val targetKey =
            when {
              matchesKnownNote && knownNoteTargets.contains(resolvedHref) -> resolvedHref
              rawFragment.isNotEmpty() -> rawFragment
              else -> resolvedHref
            }
          outSpans.add(
            InlineSpan(
              start = start,
              end = end,
              type = InlineSpanType.FOOTNOTE_REF,
              targetId = targetKey,
            )
          )
        } else if (resolvedHref.isNotBlank()) {
          outSpans.add(
            InlineSpan(
              start = start,
              end = end,
              type = InlineSpanType.CHAPTER_LINK,
              targetId = resolvedHref,
            )
          )
        }
      }
    }
  }

  /**
   * Pre-scans an XHTML/HTML file for standalone footnote/endnote blocks (cross-file EPUB notes).
   */
  fun extractFootnoteTargets(html: String, fileHref: String = ""): Map<String, BookFootnote> {
    val result = linkedMapOf<String, BookFootnote>()
    val cleaned = html.replace(HIDDEN_ELEMENTS_REGEX, "")
    for (m in BLOCK_NOTE_REGEX.findAll(cleaned)) {
      val attrs = m.groupValues[2]
      val id = extractAttr(attrs, "id") ?: extractAttr(attrs, "name")
      if (id.isNullOrBlank()) continue
      val epubType = (extractAttr(attrs, "epub:type") ?: extractAttr(attrs, "type") ?: "").lowercase()
      val cls = (extractAttr(attrs, "class") ?: "").lowercase()
      val isNote =
        epubType.contains("footnote") ||
          epubType.contains("endnote") ||
          epubType.contains("rearnote") ||
          cls.contains("footnote") ||
          cls.contains("endnote") ||
          id.startsWith("fn", ignoreCase = true) ||
          id.startsWith("note", ignoreCase = true) ||
          id.startsWith("ftn", ignoreCase = true)
      if (!isNote) continue

      val rawText =
        TextFormatParser.stripHtmlTags(m.groupValues[3]).replace(WHITESPACE_REGEX, " ").trim()
      if (rawText.isEmpty()) continue
      val marker = extractLeadingMarker(rawText) ?: id
      val body = rawText.removePrefix(marker).trimStart(' ', '.', ':', ')').ifBlank { rawText }
      val note = BookFootnote(id = id, marker = marker, text = body)
      result[id] = note
      if (fileHref.isNotBlank()) {
        result["$fileHref#$id"] = note
        val decodedFile = decodeHref(fileHref)
        if (decodedFile != fileHref) {
          result["$decodedFile#$id"] = note
        }
      }
    }
    return result
  }

  private fun extractLeadingMarker(raw: String): String? {
    val match = LEADING_MARKER_REGEX.find(raw.trimStart())
    return match?.value?.trim()?.takeIf { it.length <= 8 }
  }

  private fun extractTitle(html: String): String? {
    val m = TITLE_TAG_REGEX.find(html) ?: return null
    return TextFormatParser.decodeEntities(TextFormatParser.stripHtmlTags(m.groupValues[1]))
      .trim()
      .takeIf { it.isNotBlank() }
  }

  fun extractAttr(attrs: String, attrName: String): String? {
    val regex =
      when (attrName.lowercase()) {
        "id" -> ATTR_ID_REGEX
        "class" -> ATTR_CLASS_REGEX
        "style" -> ATTR_STYLE_REGEX
        "epub:type" -> ATTR_EPUB_TYPE_REGEX
        "type" -> ATTR_TYPE_REGEX
        "href" -> ATTR_HREF_REGEX
        "l:href" -> ATTR_LHREF_REGEX
        else ->
          Regex("""(?:^|\s)${Regex.escape(attrName)}\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
      }
    return regex.find(attrs)?.groupValues?.getOrNull(1)
  }

  private fun decodeHref(value: String): String {
    if (!value.contains('%')) return value
    return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
  }

  private fun resolveRelativeHref(chapterHref: String, href: String): String {
    val fragment = href.substringAfter('#', "")
    val pathPart = href.substringBefore('#').trim()
    val resolvedPath =
      if (pathPart.isEmpty()) {
        chapterHref
      } else {
        val baseDir = chapterHref.substringBeforeLast('/', "")
        val combined =
          if (baseDir.isBlank() || pathPart.startsWith('/')) pathPart.trimStart('/')
          else "$baseDir/$pathPart"
        val segments = mutableListOf<String>()
        for (seg in combined.split('/')) {
          when (seg) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments.add(seg)
          }
        }
        segments.joinToString("/")
      }
    val decodedPath = decodeHref(resolvedPath)
    val decodedFragment = decodeHref(fragment)
    return if (decodedFragment.isNotEmpty()) "$decodedPath#$decodedFragment" else decodedPath
  }

  private fun parseEmSize(value: String): Float? {
    return when {
      value == "0" || value == "0px" || value == "0em" || value == "0rem" -> 0f
      value.endsWith("rem") -> value.removeSuffix("rem").toFloatOrNull()
      value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()
      value.endsWith("ex") -> value.removeSuffix("ex").toFloatOrNull()?.times(0.55f)
      value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()?.div(16f)
      value.endsWith("pt") -> value.removeSuffix("pt").toFloatOrNull()?.div(12f)
      value.endsWith("%") -> value.removeSuffix("%").toFloatOrNull()?.div(100f)?.times(2.5f)
      else -> value.toFloatOrNull()
    }
  }

  private fun parseFontScale(value: String): Float? {
    return when {
      value == "xx-small" -> 0.72f
      value == "x-small" || value == "smaller" -> 0.82f
      value == "small" -> 0.90f
      value == "medium" -> 1.0f
      value == "large" || value == "larger" -> 1.15f
      value == "x-large" -> 1.32f
      value == "xx-large" -> 1.55f
      value.endsWith("%") ->
        value.removeSuffix("%").toFloatOrNull()?.div(100f)?.coerceIn(0.6f, 2.2f)
      value.endsWith("rem") -> value.removeSuffix("rem").toFloatOrNull()?.coerceIn(0.6f, 2.2f)
      value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()?.coerceIn(0.6f, 2.2f)
      value.endsWith("px") ->
        value.removeSuffix("px").toFloatOrNull()?.div(16f)?.coerceIn(0.6f, 2.2f)
      value.endsWith("pt") ->
        value.removeSuffix("pt").toFloatOrNull()?.div(12f)?.coerceIn(0.6f, 2.2f)
      else -> null
    }
  }

  private fun parseDpSpacing(value: String): Float? {
    return when {
      value == "0" || value == "auto" -> 0f
      value.endsWith("rem") -> value.removeSuffix("rem").toFloatOrNull()?.times(14f)?.coerceIn(0f, 36f)
      value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()?.times(14f)?.coerceIn(0f, 36f)
      value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()?.coerceIn(0f, 36f)
      value.endsWith("pt") -> value.removeSuffix("pt").toFloatOrNull()?.times(1.33f)?.coerceIn(0f, 36f)
      value.endsWith("%") -> value.removeSuffix("%").toFloatOrNull()?.times(2.4f)?.coerceIn(0f, 36f)
      else -> null
    }
  }

  private fun parseCssColor(raw: String): Long? {
    val v = raw.trim().lowercase()
    if (v.isEmpty() || v == "inherit" || v == "initial" || v == "transparent") return null
    NAMED_COLORS[v]?.let { return it }
    if (v.startsWith("#")) {
      val hex = v.substring(1)
      return when (hex.length) {
        3 -> {
          val r = hex[0].toString().repeat(2)
          val g = hex[1].toString().repeat(2)
          val b = hex[2].toString().repeat(2)
          "ff$r$g$b".toLongOrNull(16)
        }
        6 -> "ff$hex".toLongOrNull(16)
        8 -> {
          // CSS #rrggbbaa -> ARGB
          val rrggbb = hex.substring(0, 6)
          val aa = hex.substring(6, 8)
          "$aa$rrggbb".toLongOrNull(16)
        }
        else -> null
      }
    }
    RGB_COLOR_REGEX.matchEntire(v)?.let { m ->
      val r = m.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
      val g = m.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
      val b = m.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: return null
      val a = m.groupValues[4].toFloatOrNull()?.let { (it * 255f).toInt().coerceIn(0, 255) } ?: 255
      return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
    return null
  }
}
