package com.example.data.parser

import android.util.Xml
import com.example.data.model.BookAnchorTarget
import com.example.data.model.BookChapter
import com.example.data.model.BookFootnote
import com.example.data.model.BookParagraph
import com.example.data.model.ParagraphStyle
import com.example.data.model.ParsedBook
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

object EpubParser {

  private data class ManifestItem(
    val id: String,
    val href: String,
    val mediaType: String,
    val properties: String = "",
  )

  private val relevantTextExtensions =
    setOf("opf", "ncx", "xml", "xhtml", "html", "htm", "css", "svg")
  private val relevantImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
  private val LINK_TAG_REGEX = Regex("""<link\b([^>]+)>""", RegexOption.IGNORE_CASE)

  fun parse(inputStream: InputStream, fallbackTitle: String): ParsedBook {
    val entries = linkedMapOf<String, ByteArray>()
    ZipInputStream(inputStream).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val normalized = entry.name.replace('\\', '/')
          val ext = normalized.substringAfterLast('.', "").lowercase()
          val isMeta = normalized == "META-INF/container.xml"
          val isTextOrCss = ext in relevantTextExtensions
          val isSmallImage = ext in relevantImageExtensions && entry.size < 4_000_000L
          if ((isMeta || isTextOrCss || isSmallImage) && entry.size < 20_000_000L) {
            entries[normalized] = zis.readBytes()
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    val opfPath = findOpfPath(entries)
    val opfDir = opfPath?.substringBeforeLast('/', "") ?: ""
    val opfBytes = opfPath?.let { entries[it] }
      ?: entries.entries.firstOrNull { it.key.endsWith(".opf", ignoreCase = true) }?.value

    var title = fallbackTitle
    var author = "Неизвестный автор"
    var description = ""
    var coverIdFromMeta: String? = null
    val manifest = linkedMapOf<String, ManifestItem>()
    val spineIds = mutableListOf<String>()

    if (opfBytes != null) {
      runCatching {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(opfBytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
          if (event == XmlPullParser.START_TAG) {
            val tag = parser.name.substringAfterLast(':').lowercase()
            when (tag) {
              "title" -> {
                val text = readTextSafely(parser).trim()
                if (text.isNotEmpty()) title = text
              }
              "creator" -> {
                val text = readTextSafely(parser).trim()
                if (text.isNotEmpty() && author == "Неизвестный автор") author = text
              }
              "description" -> {
                val text = TextFormatParser.stripHtmlTags(readTextSafely(parser)).trim()
                if (text.isNotEmpty()) description = text
              }
              "meta" -> {
                val name = parser.getAttributeValue(null, "name")
                val content = parser.getAttributeValue(null, "content")
                if (name.equals("cover", ignoreCase = true) && !content.isNullOrBlank()) {
                  coverIdFromMeta = content
                }
              }
              "item" -> {
                val id = parser.getAttributeValue(null, "id") ?: ""
                val href = parser.getAttributeValue(null, "href") ?: ""
                val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                val props = parser.getAttributeValue(null, "properties") ?: ""
                if (id.isNotEmpty() && href.isNotEmpty()) {
                  manifest[id] = ManifestItem(id, href, mediaType, props)
                }
              }
              "itemref" -> {
                val idref = parser.getAttributeValue(null, "idref")
                if (!idref.isNullOrBlank()) spineIds.add(idref)
              }
            }
          }
          event = parser.next()
        }
      }
    }

    val coverItem =
      coverIdFromMeta?.let { manifest[it] }
        ?: manifest.values.firstOrNull {
          it.properties.contains("cover-image", ignoreCase = true)
        }
        ?: manifest.values.firstOrNull {
          it.mediaType.startsWith("image/", ignoreCase = true) &&
            (it.id.contains("cover", ignoreCase = true) ||
              it.href.contains("cover", ignoreCase = true))
        }

    val coverBytes =
      coverItem?.let { item ->
        val resolved = resolvePath(opfDir, item.href)
        val decoded = decodeHref(resolved)
        entries[resolved]
          ?: entries[decoded]
          ?: entries.entries.firstOrNull { it.key.endsWith(decodeHref(item.href)) }?.value
      }

    // Parse all CSS stylesheets in the EPUB archive preserving both simple & descendant selectors
    val cssSheetsByPath = linkedMapOf<String, CssStyleParser.CssSheet>()
    var globalCssSheet = CssStyleParser.CssSheet()
    entries.forEach { (path, bytes) ->
      if (path.endsWith(".css", ignoreCase = true)) {
        val sheet = CssStyleParser.parseCss(bytes.toString(Charsets.UTF_8))
        cssSheetsByPath[path] = sheet
        val decodedPath = decodeHref(path)
        if (decodedPath != path) cssSheetsByPath[decodedPath] = sheet
        globalCssSheet = globalCssSheet.mergeWith(sheet)
      }
    }

    val htmlEntries: List<Pair<String, ByteArray>> =
      if (spineIds.isNotEmpty()) {
        spineIds.mapNotNull { id ->
          val item = manifest[id] ?: return@mapNotNull null
          val resolved = resolvePath(opfDir, item.href)
          val decodedResolved = decodeHref(resolved)
          val decodedItemHref = decodeHref(item.href.substringBefore('#'))
          val bytes =
            entries[resolved]
              ?: entries[decodedResolved]
              ?: entries.entries.firstOrNull { it.key.endsWith(decodedItemHref) }?.value
          bytes?.let { decodedResolved to it }
        }
      } else {
        entries.entries
          .filter {
            it.key.endsWith(".xhtml", true) ||
              it.key.endsWith(".html", true) ||
              it.key.endsWith(".htm", true)
          }
          .sortedBy { it.key }
          .map { decodeHref(it.key) to it.value }
      }

    // Decode HTML entries once and pre-scan for footnote definitions (supports cross-file notes)
    val decodedHtmlByPath = HashMap<String, String>(entries.size)
    val allFootnotes = linkedMapOf<String, BookFootnote>()
    entries.forEach { (path, bytes) ->
      if (path.endsWith(".xhtml", true) ||
        path.endsWith(".html", true) ||
        path.endsWith(".htm", true)
      ) {
        val html = bytes.toString(Charsets.UTF_8)
        val decodedPath = decodeHref(path)
        decodedHtmlByPath[path] = html
        decodedHtmlByPath[decodedPath] = html
        if (html.contains("note", ignoreCase = true) || html.contains("fn", ignoreCase = true)) {
          allFootnotes.putAll(CssStyleParser.extractFootnoteTargets(html, fileHref = decodedPath))
        }
      }
    }

    val chapters = mutableListOf<BookChapter>()
    val anchorTargets = linkedMapOf<String, BookAnchorTarget>()

    htmlEntries.forEachIndexed { index, (resolvedPath, bytes) ->
      val html = decodedHtmlByPath[resolvedPath] ?: bytes.toString(Charsets.UTF_8)
      val chapterDir = resolvedPath.substringBeforeLast('/', "")
      var linkedSheet = CssStyleParser.CssSheet()
      var hasLinkedCss = false

      if (html.contains("<link", ignoreCase = true)) {
        for (tagMatch in LINK_TAG_REGEX.findAll(html)) {
          val attrs = tagMatch.groupValues[1]
          val rel = CssStyleParser.extractAttr(attrs, "rel")?.lowercase() ?: ""
          val href = CssStyleParser.extractAttr(attrs, "href") ?: continue
          if (rel.contains("stylesheet") || href.endsWith(".css", ignoreCase = true)) {
            val cssPath = resolvePath(chapterDir, href)
            val decodedCssPath = decodeHref(cssPath)
            val sheet =
              cssSheetsByPath[cssPath]
                ?: cssSheetsByPath[decodedCssPath]
                ?: cssSheetsByPath.entries.firstOrNull {
                  it.key.endsWith(decodeHref(href.substringAfterLast('/')))
                }?.value
            if (sheet != null) {
              linkedSheet = linkedSheet.mergeWith(sheet)
              hasLinkedCss = true
            }
          }
        }
      }
      val effectiveCss = if (hasLinkedCss) linkedSheet else globalCssSheet

      val result =
        CssStyleParser.parseHtmlWithCss(
          html = html,
          defaultTitle = "Глава ${chapters.size + 1}",
          externalCss = effectiveCss,
          chapterHref = resolvedPath,
          knownNoteTargets = allFootnotes.keys,
        )
      allFootnotes.putAll(result.footnotes)

      if (result.chapter.paragraphs.isNotEmpty()) {
        val chapterIdx = chapters.size
        val maxParaIdx = result.chapter.paragraphs.lastIndex.coerceAtLeast(0)
        chapters.add(
          result.chapter.copy(
            index = chapterIdx,
            title = result.chapter.title.ifBlank {
              resolvedPath.substringAfterLast('/').substringBeforeLast('.').ifBlank {
                "Глава ${index + 1}"
              }
            },
          )
        )

        val chapterStartTarget = BookAnchorTarget(chapterIdx, 0)
        val fileName = resolvedPath.substringAfterLast('/')
        anchorTargets[resolvedPath] = chapterStartTarget
        anchorTargets.putIfAbsent(fileName, chapterStartTarget)
        val lowerResolved = resolvedPath.lowercase()
        val lowerFile = fileName.lowercase()
        anchorTargets.putIfAbsent(lowerResolved, chapterStartTarget)
        anchorTargets.putIfAbsent(lowerFile, chapterStartTarget)

        result.anchorParagraphIndices.forEach { (anchorId, rawParaIdx) ->
          val target = BookAnchorTarget(chapterIdx, rawParaIdx.coerceIn(0, maxParaIdx))
          anchorTargets["$resolvedPath#$anchorId"] = target
          anchorTargets.putIfAbsent("$fileName#$anchorId", target)
          anchorTargets.putIfAbsent("$lowerResolved#${anchorId.lowercase()}", target)
          anchorTargets.putIfAbsent(anchorId, target)
        }
      }
    }

    if (chapters.isEmpty()) {
      chapters.add(
        BookChapter(
          index = 0,
          title = title,
          paragraphs = listOf(BookParagraph("Не удалось извлечь текст из EPUB-файла.", ParagraphStyle.BODY)),
        )
      )
    }

    return ParsedBook(
      title = title,
      author = author,
      description = description,
      chapters = chapters,
      coverBytes = coverBytes,
      footnotes = allFootnotes,
      anchorTargets = anchorTargets,
    )
  }

  private fun decodeHref(value: String): String {
    if (!value.contains('%')) return value
    return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
  }

  private fun findOpfPath(entries: Map<String, ByteArray>): String? {
    val container = entries["META-INF/container.xml"] ?: return null
    return runCatching {
      val parser = Xml.newPullParser()
      parser.setInput(ByteArrayInputStream(container), "UTF-8")
      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name.endsWith("rootfile")) {
          val path = parser.getAttributeValue(null, "full-path")
          if (!path.isNullOrBlank()) return path
        }
        event = parser.next()
      }
      null
    }.getOrNull()
  }

  private fun resolvePath(baseDir: String, href: String): String {
    val cleanHref = href.substringBefore('#').trimStart('/')
    if (baseDir.isBlank()) return cleanHref
    val combined = "$baseDir/$cleanHref"
    val parts = mutableListOf<String>()
    for (segment in combined.split('/')) {
      when (segment) {
        "", "." -> Unit
        ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
        else -> parts.add(segment)
      }
    }
    return parts.joinToString("/")
  }

  private fun readTextSafely(parser: XmlPullParser): String {
    val sb = StringBuilder()
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.TEXT -> sb.append(parser.text)
        XmlPullParser.START_TAG -> depth++
        XmlPullParser.END_TAG -> depth--
        XmlPullParser.END_DOCUMENT -> break
      }
    }
    return sb.toString()
  }
}
