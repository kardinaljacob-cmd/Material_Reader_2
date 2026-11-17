package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookAnchorTarget
import com.example.data.model.BookEntity
import com.example.data.model.BookFootnote
import com.example.data.model.BookFormat
import com.example.data.model.BookParagraph
import com.example.data.model.BookmarkEntity
import com.example.data.model.InlineSpanType
import com.example.data.model.NoteEntity
import com.example.data.model.PageFlipAnimation
import com.example.data.model.ParagraphStyle
import com.example.data.model.ParsedBook
import com.example.data.prefs.ReaderPreferences
import com.example.ui.components.ExpressiveWavyProgressIndicator
import com.example.ui.reader.PageParagraph
import com.example.ui.reader.ReaderPage
import com.example.ui.reader.TextPaginator
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.UnboundedFamily
import com.example.ui.viewmodel.ReaderLoadState
import com.example.ui.viewmodel.ReaderScreenState
import com.example.ui.viewmodel.ReaderViewModel
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ScrollNode {
  val chapterIndex: Int
  val chapterTitle: String
  val charWeight: Int

  data class Paragraph(
    override val chapterIndex: Int,
    override val chapterTitle: String,
    val paragraphIndex: Int,
    val paragraph: BookParagraph,
    override val charWeight: Int,
  ) : ScrollNode

  data class ChapterSpacer(
    override val chapterIndex: Int,
    override val chapterTitle: String,
    override val charWeight: Int = 1,
  ) : ScrollNode
}

@Composable
fun ReaderScreen(
  bookId: Long,
  uiState: ReaderScreenState,
  viewModel: ReaderViewModel,
  onNavigateBack: () -> Unit,
  showLoadingSpinner: Boolean = true,
  enableBackHandler: Boolean = true,
) {
  val haptics = LocalExpressiveHaptics.current
  val view = LocalView.current

  BackHandler(enabled = enableBackHandler) { onNavigateBack() }

  LaunchedEffect(bookId) { viewModel.loadBook(bookId) }

  DisposableEffect(view, uiState.preferences.keepScreenOn) {
    val prev = view.keepScreenOn
    view.keepScreenOn = uiState.preferences.keepScreenOn
    onDispose { view.keepScreenOn = prev }
  }

  val prefs = uiState.preferences
  val readerTheme = prefs.readerTheme
  val targetBg =
    if (readerTheme.useSystemDynamic) MaterialTheme.colorScheme.surfaceContainerLowest
    else readerTheme.background
  val targetText =
    if (readerTheme.useSystemDynamic) MaterialTheme.colorScheme.onSurface
    else readerTheme.text
  val targetSecondary =
    if (readerTheme.useSystemDynamic) MaterialTheme.colorScheme.onSurfaceVariant
    else readerTheme.secondaryText
  val targetAccent =
    if (readerTheme.useSystemDynamic) MaterialTheme.colorScheme.primary
    else readerTheme.accent

  val themeSnapshot =
    remember {
      ReaderThemeSnapshot(
        bg = targetBg,
        text = targetText,
        secondary = targetSecondary,
        accent = targetAccent,
      )
    }
  var fromBg by remember { mutableStateOf(targetBg) }
  var fromText by remember { mutableStateOf(targetText) }
  var fromSecondary by remember { mutableStateOf(targetSecondary) }
  var fromAccent by remember { mutableStateOf(targetAccent) }
  var appliedReaderTheme by remember { mutableStateOf(readerTheme) }
  val readerThemeAnim = remember { Animatable(1f) }

  val isTransitioning = appliedReaderTheme != readerTheme || readerThemeAnim.value < 1f
  val blendT =
    if (appliedReaderTheme != readerTheme) 0f else readerThemeAnim.value.coerceIn(0f, 1f)
  val bgColor =
    if (!isTransitioning) targetBg
    else if (appliedReaderTheme != readerTheme) themeSnapshot.bg
    else lerp(fromBg, targetBg, blendT)
  val textColor =
    if (!isTransitioning) targetText
    else if (appliedReaderTheme != readerTheme) themeSnapshot.text
    else lerp(fromText, targetText, blendT)
  val secondaryColor =
    if (!isTransitioning) targetSecondary
    else if (appliedReaderTheme != readerTheme) themeSnapshot.secondary
    else lerp(fromSecondary, targetSecondary, blendT)
  val accentColor =
    if (!isTransitioning) targetAccent
    else if (appliedReaderTheme != readerTheme) themeSnapshot.accent
    else lerp(fromAccent, targetAccent, blendT)

  if (appliedReaderTheme == readerTheme) {
    themeSnapshot.bg = bgColor
    themeSnapshot.text = textColor
    themeSnapshot.secondary = secondaryColor
    themeSnapshot.accent = accentColor
  }

  LaunchedEffect(readerTheme) {
    if (appliedReaderTheme == readerTheme && readerThemeAnim.value >= 1f) return@LaunchedEffect
    fromBg = themeSnapshot.bg
    fromText = themeSnapshot.text
    fromSecondary = themeSnapshot.secondary
    fromAccent = themeSnapshot.accent
    readerThemeAnim.snapTo(0f)
    appliedReaderTheme = readerTheme
    readerThemeAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 580, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
    )
  }

  val appBackgroundIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
  val readerBackgroundIsLight = bgColor.luminance() > 0.5f
  if (!view.isInEditMode) {
    LaunchedEffect(view, readerBackgroundIsLight) {
      view.context.findReaderActivity()?.window?.let { window ->
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = readerBackgroundIsLight
        controller.isAppearanceLightNavigationBars = readerBackgroundIsLight
      }
    }
    DisposableEffect(view, appBackgroundIsLight) {
      onDispose {
        view.context.findReaderActivity()?.window?.let { window ->
          val controller = WindowCompat.getInsetsController(window, view)
          controller.isAppearanceLightStatusBars = appBackgroundIsLight
          controller.isAppearanceLightNavigationBars = appBackgroundIsLight
        }
      }
    }
  }

  var showControls by remember { mutableStateOf(false) }
  var showFontSheet by remember { mutableStateOf(false) }
  var showThemeSheet by remember { mutableStateOf(false) }
  var showTocSheet by remember { mutableStateOf(false) }
  var isInitialLayoutReady by remember(bookId) { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
    when (val load = uiState.loadState) {
      ReaderLoadState.Loading -> Unit
      is ReaderLoadState.Error -> {
        isInitialLayoutReady = true
        Column(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(load.message, style = MaterialTheme.typography.titleMedium, color = textColor)
          Spacer(Modifier.height(16.dp))
          Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier.clickable { onNavigateBack() },
          ) {
            Text(
              "Вернуться на полку",
              color = Color.White,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
          }
        }
      }
      is ReaderLoadState.Ready -> {
        ReaderContentBody(
          book = load.book,
          parsedBook = load.parsedBook,
          prefs = prefs,
          bookmarks = uiState.bookmarks,
          notes = uiState.notes,
          bgColor = bgColor,
          textColor = textColor,
          secondaryColor = secondaryColor,
          accentColor = accentColor,
          showControls = showControls,
          onToggleControls = {
            haptics.tick()
            showControls = !showControls
          },
          onInitialContentReady = { isInitialLayoutReady = true },
          viewModel = viewModel,
          onNavigateBack = onNavigateBack,
          onOpenFonts = { showFontSheet = true },
          onOpenThemes = { showThemeSheet = true },
          onOpenToc = { showTocSheet = true },
          showTocSheet = showTocSheet,
          onDismissToc = { showTocSheet = false },
        )
      }
    }

    // Single unified loading overlay ("Открываем книгу...") that stays hidden while cached/fast
    // books morph in, and only fades in softly if parsing/pagination takes longer than the morph.
    AnimatedVisibility(
      visible =
        showLoadingSpinner && (uiState.loadState is ReaderLoadState.Loading || !isInitialLayoutReady),
      enter = fadeIn(tween(140)),
      exit = fadeOut(tween(240)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator(color = accentColor)
          Text("Открываем книгу...", color = secondaryColor)
        }
      }
    }

    if (showFontSheet) {
      ReaderFontSheet(
        prefs = prefs,
        onSelectFont = viewModel::setFontOption,
        onFontSizeChange = viewModel::setFontSize,
        onLineHeightChange = viewModel::setLineHeight,
        onLetterSpacingChange = viewModel::setLetterSpacing,
        onWordSpacingChange = viewModel::setWordSpacing,
        onParagraphSpacingChange = viewModel::setParagraphSpacing,
        onFirstLineIndentChange = viewModel::setFirstLineIndent,
        onPaddingChange = viewModel::setHorizontalPadding,
        onJustifyChange = viewModel::setJustify,
        onResetTypography = viewModel::resetTypographyDefaults,
        onDismiss = { showFontSheet = false },
      )
    }

    if (showThemeSheet) {
      ReaderThemeAndMotionSheet(
        prefs = prefs,
        onSelectReaderTheme = viewModel::setReaderTheme,
        onSelectPageAnimation = viewModel::setPageAnimation,
        onDismiss = { showThemeSheet = false },
      )
    }
  }
}

@Composable
private fun ReaderContentBody(
  book: BookEntity,
  parsedBook: ParsedBook,
  prefs: ReaderPreferences,
  bookmarks: List<BookmarkEntity>,
  notes: List<NoteEntity>,
  bgColor: Color,
  textColor: Color,
  secondaryColor: Color,
  accentColor: Color,
  showControls: Boolean,
  onToggleControls: () -> Unit,
  onInitialContentReady: () -> Unit,
  viewModel: ReaderViewModel,
  onNavigateBack: () -> Unit,
  onOpenFonts: () -> Unit,
  onOpenThemes: () -> Unit,
  onOpenToc: () -> Unit,
  showTocSheet: Boolean,
  onDismissToc: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val scope = rememberCoroutineScope()
  val isPdf = book.format == BookFormat.PDF
  val density = LocalDensity.current
  val fontFamilyResolver = LocalFontFamilyResolver.current
  val layoutDirection = LocalLayoutDirection.current

  var textViewportSize by remember { mutableStateOf(IntSize.Zero) }
  val safetyMarginPx = with(density) { 4.dp.toPx().toInt() }
  val footnoteChromePx = with(density) { 26.dp.toPx().toInt() }
  val contentWidthPx = textViewportSize.width
  val contentHeightPx = (textViewportSize.height - safetyMarginPx).coerceAtLeast(0)
  val paragraphSpacingPx = with(density) { prefs.paragraphSpacingDp.dp.toPx().toInt() }

  val bgTextMeasurer =
    remember(fontFamilyResolver, density, layoutDirection) {
      TextMeasurer(
        defaultFontFamilyResolver = fontFamilyResolver,
        defaultDensity = density,
        defaultLayoutDirection = layoutDirection,
        cacheSize = 0,
      )
    }

  var pages by remember(book.id) { mutableStateOf<List<ReaderPage>>(emptyList()) }
  var currentVisiblePage by remember(book.id) { mutableIntStateOf(0) }
  var hasInitializedPage by remember(book.id) { mutableStateOf(false) }
  var selectedFootnoteDetail by remember { mutableStateOf<BookFootnote?>(null) }
  var pageTurnJob by remember(book.id) { mutableStateOf<Job?>(null) }
  var targetTurnPage by remember(book.id) { mutableIntStateOf(0) }
  var pendingAnchorTarget by remember(book.id) { mutableStateOf<BookAnchorTarget?>(null) }

  val pagerState =
    rememberPagerState(
      initialPage = 0,
      pageCount = { pages.size.coerceAtLeast(1) },
    )

  fun turnPageBy(delta: Int) {
    val maxIdx = pages.lastIndex
    if (maxIdx <= 0) return
    val wasAnimating = pageTurnJob?.isActive == true
    val prevTarget = targetTurnPage.coerceIn(0, maxIdx)
    val basePage = if (wasAnimating) prevTarget else pagerState.currentPage
    val nextPage = (basePage + delta).coerceIn(0, maxIdx)
    if (nextPage == basePage && !wasAnimating) return
    pageTurnJob?.cancel()
    targetTurnPage = nextPage
    haptics.tick()
    pageTurnJob =
      scope.launch {
        if (wasAnimating) {
          pagerState.scrollToPage(prevTarget)
        }
        pagerState.animateScrollToPage(
          page = nextPage,
          animationSpec =
            spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessMedium,
            ),
        )
      }
  }

  val isScrollMode = !isPdf && prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL

  // Notify parent immediately for PDF / Scroll mode, or after initial pagination for Paged modes
  LaunchedEffect(isPdf, isScrollMode) {
    if (isPdf || isScrollMode) {
      onInitialContentReady()
    }
  }

  // Asynchronous progressive background pagination using exact measured viewport bounds
  LaunchedEffect(
    parsedBook,
    isScrollMode,
    contentWidthPx,
    contentHeightPx,
    prefs.fontOption,
    prefs.fontSizeSp,
    prefs.lineHeightMultiplier,
    prefs.letterSpacingSp,
    prefs.wordSpacingSp,
    prefs.horizontalPaddingDp,
    prefs.paragraphSpacingDp,
    prefs.firstLineIndentSp,
    prefs.textAlignJustify,
  ) {
    if (isPdf) {
      pages =
        parsedBook.chapters.mapIndexed { idx, ch ->
          ReaderPage(
            globalIndex = idx,
            chapterIndex = idx,
            chapterTitle = ch.title,
            pageInChapter = 0,
            totalPagesInChapter = 1,
            paragraphs =
              listOf(
                PageParagraph(
                  content = AnnotatedString(ch.title),
                  style = ParagraphStyle.HEADING,
                )
              ),
            previewExcerpt = "PDF Страница ${idx + 1}",
          )
        }
      if (!hasInitializedPage) {
        hasInitializedPage = true
        val initIdx = book.currentChapterIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        currentVisiblePage = initIdx
        targetTurnPage = initIdx
        pagerState.scrollToPage(initIdx)
        onInitialContentReady()
      }
      return@LaunchedEffect
    }

    if (isScrollMode) return@LaunchedEffect
    if (contentWidthPx <= 64 || contentHeightPx <= 64) return@LaunchedEffect

    val prevPage = pages.getOrNull(currentVisiblePage)
    val anchorChapter = prevPage?.chapterIndex ?: book.currentChapterIndex
    val anchorPageInChapter = prevPage?.pageInChapter ?: book.currentPageInChapter
    val anchorRatio =
      if (prevPage != null && prevPage.totalPagesInChapter > 1) {
        prevPage.pageInChapter.toFloat() / (prevPage.totalPagesInChapter - 1).toFloat()
      } else 0f

    if (pages.isNotEmpty()) {
      delay(150)
    }

    val shouldUseProgressiveLoad = !hasInitializedPage && parsedBook.chapters.size > 3
    val phase1ChapterCount =
      if (shouldUseProgressiveLoad) {
        (anchorChapter + 2).coerceIn(2, parsedBook.chapters.size)
      } else {
        parsedBook.chapters.size
      }

    if (phase1ChapterCount < parsedBook.chapters.size) {
      // Phase 1: Paginate only chapters through anchorChapter + 1 for near-instant opening
      val phase1Pages =
        withContext(Dispatchers.Default) {
          TextPaginator.paginate(
            chapters = parsedBook.chapters.subList(0, phase1ChapterCount),
            textMeasurer = bgTextMeasurer,
            widthPx = contentWidthPx,
            heightPx = contentHeightPx,
            prefs = prefs,
            paragraphSpacingPx = paragraphSpacingPx,
            footnotes = parsedBook.footnotes,
            footnoteChromePx = footnoteChromePx,
          )
        }
      pages = phase1Pages
      hasInitializedPage = true
      if (phase1Pages.isNotEmpty()) {
        val targetIndex =
          phase1Pages
            .indexOfFirst {
              it.chapterIndex == anchorChapter && it.pageInChapter >= anchorPageInChapter
            }
            .takeIf { it >= 0 }
            ?: phase1Pages.indexOfFirst { it.chapterIndex >= anchorChapter }.coerceAtLeast(0)
        val safeTarget = targetIndex.coerceIn(0, phase1Pages.lastIndex)
        currentVisiblePage = safeTarget
        targetTurnPage = safeTarget
        pagerState.scrollToPage(safeTarget)
      }
      onInitialContentReady()

      // Phase 2: Paginate remaining chapters seamlessly in background and append
      val tailPages =
        withContext(Dispatchers.Default) {
          TextPaginator.paginate(
            chapters = parsedBook.chapters.subList(phase1ChapterCount, parsedBook.chapters.size),
            textMeasurer = bgTextMeasurer,
            widthPx = contentWidthPx,
            heightPx = contentHeightPx,
            prefs = prefs,
            paragraphSpacingPx = paragraphSpacingPx,
            footnotes = parsedBook.footnotes,
            footnoteChromePx = footnoteChromePx,
            startingGlobalIndex = phase1Pages.size,
          )
        }
      pages = phase1Pages + tailPages
    } else {
      val newPages =
        withContext(Dispatchers.Default) {
          TextPaginator.paginate(
            chapters = parsedBook.chapters,
            textMeasurer = bgTextMeasurer,
            widthPx = contentWidthPx,
            heightPx = contentHeightPx,
            prefs = prefs,
            paragraphSpacingPx = paragraphSpacingPx,
            footnotes = parsedBook.footnotes,
            footnoteChromePx = footnoteChromePx,
          )
        }

      pages = newPages
      if (newPages.isNotEmpty()) {
        val targetIndex =
          if (!hasInitializedPage) {
            hasInitializedPage = true
            newPages
              .indexOfFirst {
                it.chapterIndex == anchorChapter && it.pageInChapter >= anchorPageInChapter
              }
              .takeIf { it >= 0 }
              ?: newPages.indexOfFirst { it.chapterIndex >= anchorChapter }.coerceAtLeast(0)
          } else {
            val chapterMatches = newPages.filter { it.chapterIndex == anchorChapter }
            if (chapterMatches.isNotEmpty()) {
              val offsetInCh =
                (anchorRatio * (chapterMatches.size - 1))
                  .roundToInt()
                  .coerceIn(0, chapterMatches.lastIndex)
              chapterMatches[offsetInCh].globalIndex
            } else {
              newPages.indexOfFirst { it.chapterIndex >= anchorChapter }.coerceAtLeast(0)
            }
          }
        val safeTarget = targetIndex.coerceIn(0, newPages.lastIndex)
        currentVisiblePage = safeTarget
        targetTurnPage = safeTarget
        pagerState.scrollToPage(safeTarget)
        onInitialContentReady()
      } else {
        onInitialContentReady()
      }
    }
  }

  // Flattened scroll nodes for fast budget-friendly LazyColumn + accurate progress + footnote tracking
  val scrollNodes =
    remember(parsedBook) {
      buildList<ScrollNode> {
        parsedBook.chapters.forEachIndexed { chIdx, chapter ->
          chapter.paragraphs.forEachIndexed { pIdx, para ->
            add(
              ScrollNode.Paragraph(
                chapterIndex = chIdx,
                chapterTitle = chapter.title,
                paragraphIndex = pIdx,
                paragraph = para,
                charWeight = para.text.length.coerceAtLeast(24),
              )
            )
          }
          if (chIdx < parsedBook.chapters.lastIndex) {
            add(ScrollNode.ChapterSpacer(chapterIndex = chIdx, chapterTitle = chapter.title))
          }
        }
      }
    }

  val scrollCumWeights =
    remember(scrollNodes) {
      IntArray(scrollNodes.size + 1).also { arr ->
        for (i in scrollNodes.indices) {
          arr[i + 1] = arr[i] + scrollNodes[i].charWeight
        }
      }
    }
  val scrollTotalWeight = remember(scrollCumWeights) { scrollCumWeights.last().coerceAtLeast(1) }

  val initialScrollNodeIndex =
    remember(book.id, scrollNodes) {
      val chStart =
        scrollNodes.indexOfFirst { it.chapterIndex >= book.currentChapterIndex }.coerceAtLeast(0)
      val savedNodeInChapter = book.currentPageInChapter.coerceAtLeast(0)
      val candidate = chStart + savedNodeInChapter
      if (candidate in scrollNodes.indices &&
        scrollNodes[candidate].chapterIndex == book.currentChapterIndex
      ) {
        candidate
      } else {
        chStart
      }
    }

  val scrollListState =
    rememberLazyListState(initialFirstVisibleItemIndex = initialScrollNodeIndex)

  // Track Y bounds of footnote references per visible scroll item
  val scrollRefBounds =
    remember(parsedBook) {
      mutableStateMapOf<Int, List<Pair<String, ClosedFloatingPointRange<Float>>>>()
    }

  val scrollProgress by remember(scrollNodes, scrollCumWeights, scrollTotalWeight) {
    derivedStateOf {
      if (scrollNodes.isEmpty()) return@derivedStateOf 0f
      val layout = scrollListState.layoutInfo
      if (layout.totalItemsCount > 0 && !scrollListState.canScrollForward && scrollListState.firstVisibleItemIndex > 0) {
        return@derivedStateOf 1f
      }
      if (!scrollListState.canScrollBackward) {
        return@derivedStateOf 0f
      }
      val firstIdx = scrollListState.firstVisibleItemIndex.coerceIn(0, scrollNodes.lastIndex)
      val firstInfo = layout.visibleItemsInfo.firstOrNull { it.index == firstIdx }
      val intraFraction =
        if (firstInfo != null && firstInfo.size > 0) {
          ((-firstInfo.offset).toFloat() / firstInfo.size.toFloat()).coerceIn(0f, 1f)
        } else 0f
      val done = scrollCumWeights[firstIdx] + scrollNodes[firstIdx].charWeight * intraFraction
      (done / scrollTotalWeight.toFloat()).coerceIn(0f, 1f)
    }
  }

  val activeScrollNode by remember(scrollNodes) {
    derivedStateOf {
      val idx = scrollListState.firstVisibleItemIndex.coerceIn(0, (scrollNodes.size - 1).coerceAtLeast(0))
      scrollNodes.getOrNull(idx)
    }
  }

  val visibleScrollFootnotes by remember(parsedBook.footnotes, scrollNodes) {
    derivedStateOf {
      if (parsedBook.footnotes.isEmpty() || scrollNodes.isEmpty()) return@derivedStateOf emptyList()
      val layout = scrollListState.layoutInfo
      val vpStart = layout.viewportStartOffset.toFloat()
      val vpEnd = layout.viewportEndOffset.toFloat()
      val collected = LinkedHashMap<String, BookFootnote>()

      for (itemInfo in layout.visibleItemsInfo) {
        val node = scrollNodes.getOrNull(itemInfo.index) as? ScrollNode.Paragraph ?: continue
        val footnoteSpans = node.paragraph.spans.filter { it.type == InlineSpanType.FOOTNOTE_REF }
        if (footnoteSpans.isEmpty()) continue

        val recorded = scrollRefBounds[itemInfo.index]
        if (recorded != null) {
          for ((fnId, localRange) in recorded) {
            val globalTop = itemInfo.offset + localRange.start
            val globalBottom = itemInfo.offset + localRange.endInclusive
            if (globalBottom > vpStart + 2f && globalTop < vpEnd - 2f) {
              TextPaginator.resolveFootnote(fnId, parsedBook.footnotes)?.let { fn ->
                collected[fn.id] = fn
              }
            }
          }
        } else {
          // Fallback on very first frame before onTextLayout completes
          for (sp in footnoteSpans) {
            val target = sp.targetId ?: continue
            TextPaginator.resolveFootnote(target, parsedBook.footnotes)?.let { fn ->
              collected[fn.id] = fn
            }
          }
        }
      }
      collected.values.toList()
    }
  }

  fun jumpToAnchor(target: BookAnchorTarget) {
    haptics.morph()
    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL && !isPdf) {
      val exactIdx =
        scrollNodes.indexOfFirst {
          it.chapterIndex == target.chapterIndex &&
            it is ScrollNode.Paragraph &&
            it.paragraphIndex >= target.paragraphIndex
        }.takeIf { it >= 0 }
          ?: scrollNodes.indexOfFirst { it.chapterIndex == target.chapterIndex }
      if (exactIdx >= 0) {
        scope.launch { scrollListState.animateScrollToItem(exactIdx) }
      }
    } else {
      val exactPage =
        pages.indexOfFirst { page ->
          page.chapterIndex == target.chapterIndex &&
            page.paragraphs.any { it.paragraphIndex >= target.paragraphIndex }
        }.takeIf { it >= 0 }
          ?: pages.indexOfFirst { it.chapterIndex == target.chapterIndex }
      if (exactPage >= 0) {
        pendingAnchorTarget = null
        pageTurnJob?.cancel()
        targetTurnPage = exactPage
        pageTurnJob = scope.launch { pagerState.scrollToPage(exactPage) }
      } else {
        pendingAnchorTarget = target
      }
    }
  }

  LaunchedEffect(pages.size, pendingAnchorTarget) {
    val pending = pendingAnchorTarget ?: return@LaunchedEffect
    val exactPage =
      pages.indexOfFirst { page ->
        page.chapterIndex == pending.chapterIndex &&
          page.paragraphs.any { it.paragraphIndex >= pending.paragraphIndex }
      }.takeIf { it >= 0 }
        ?: pages.indexOfFirst { it.chapterIndex == pending.chapterIndex }
    if (exactPage >= 0) {
      pendingAnchorTarget = null
      pageTurnJob?.cancel()
      targetTurnPage = exactPage
      pagerState.scrollToPage(exactPage)
    }
  }

  LaunchedEffect(pagerState.currentPage, pages.size, prefs.pageAnimation) {
    if (prefs.pageAnimation != PageFlipAnimation.VERTICAL_SCROLL && pages.isNotEmpty()) {
      val idx = pagerState.currentPage.coerceIn(0, pages.lastIndex)
      currentVisiblePage = idx
      delay(250)
      val page = pages.getOrNull(idx) ?: return@LaunchedEffect
      val pct = if (pages.size <= 1) 1f else (idx + 1).toFloat() / pages.size.toFloat()
      viewModel.saveReadingPosition(book.id, page.chapterIndex, page.pageInChapter, pct)
    }
  }

  val latestPages by rememberUpdatedState(pages)
  val latestVisiblePage by rememberUpdatedState(currentVisiblePage)
  val latestIsScroll by rememberUpdatedState(prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL && !isPdf)
  val latestScrollNodes by rememberUpdatedState(scrollNodes)
  val latestScrollIndex by rememberUpdatedState(scrollListState.firstVisibleItemIndex)
  val latestScrollPct by rememberUpdatedState(scrollProgress)

  DisposableEffect(book.id) {
    onDispose {
      if (latestIsScroll && latestScrollNodes.isNotEmpty()) {
        val idx = latestScrollIndex.coerceIn(0, latestScrollNodes.lastIndex)
        val node = latestScrollNodes[idx]
        val chStart = latestScrollNodes.indexOfFirst { it.chapterIndex == node.chapterIndex }.coerceAtLeast(0)
        viewModel.saveReadingPosition(book.id, node.chapterIndex, (idx - chStart).coerceAtLeast(0), latestScrollPct)
      } else if (latestPages.isNotEmpty()) {
        val idx = latestVisiblePage.coerceIn(0, latestPages.lastIndex)
        val page = latestPages[idx]
        val pct = if (latestPages.size <= 1) 1f else (idx + 1).toFloat() / latestPages.size.toFloat()
        viewModel.saveReadingPosition(book.id, page.chapterIndex, page.pageInChapter, pct)
      }
    }
  }

  LaunchedEffect(scrollListState, prefs.pageAnimation, scrollNodes) {
    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL && scrollNodes.isNotEmpty()) {
      snapshotFlow {
        Triple(
          scrollListState.firstVisibleItemIndex,
          scrollListState.firstVisibleItemScrollOffset,
          scrollProgress,
        )
      }.collectLatest { (idx, _, pct) ->
        delay(300)
        val node = scrollNodes.getOrNull(idx.coerceIn(0, scrollNodes.lastIndex)) ?: return@collectLatest
        val chStart = scrollNodes.indexOfFirst { it.chapterIndex == node.chapterIndex }.coerceAtLeast(0)
        val offsetInCh = (idx - chStart).coerceAtLeast(0)
        viewModel.saveReadingPosition(book.id, node.chapterIndex, offsetInCh, pct)
      }
    }
  }

  val activePage = pages.getOrNull(currentVisiblePage)
  val activeChapterIdx =
    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) activeScrollNode?.chapterIndex ?: 0
    else activePage?.chapterIndex ?: 0
  val activeChapterTitle =
    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) {
      activeScrollNode?.chapterTitle ?: book.title
    } else {
      activePage?.chapterTitle ?: book.author
    }
  val activeBookmarkPage =
    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) {
      val chStart = scrollNodes.indexOfFirst { it.chapterIndex == activeChapterIdx }.coerceAtLeast(0)
      (scrollListState.firstVisibleItemIndex - chStart).coerceAtLeast(0)
    } else {
      activePage?.pageInChapter ?: 0
    }
  val isCurrentBookmarked =
    bookmarks.any {
      it.chapterIndex == activeChapterIdx && it.pageIndex == activeBookmarkPage
    }

  val notesByChapter = remember(notes) { notes.groupBy { it.chapterIndex } }
  var pendingNoteQuote by remember(book.id) { mutableStateOf<String?>(null) }
  var editingNote by remember(book.id) { mutableStateOf<NoteEntity?>(null) }
  var tocInitialTab by remember(book.id) { mutableIntStateOf(0) }
  var readerToastMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(readerToastMessage) {
    if (readerToastMessage != null) {
      delay(1800)
      readerToastMessage = null
    }
  }

  val latestChapterIdx by rememberUpdatedState(activeChapterIdx)
  val latestChapterTitle by rememberUpdatedState(activeChapterTitle)
  val latestBookmarkPage by rememberUpdatedState(activeBookmarkPage)

  val systemClipboard = LocalClipboardManager.current
  val readerToolbarController = remember(book.id) { ReaderTextToolbarController() }
  val clipboardInterceptor =
    remember(book.id, systemClipboard) {
      ReaderClipboardInterceptor(systemClipboard) { capturedText, action ->
        val cleaned = capturedText.trim()
        if (cleaned.isNotEmpty()) {
          when (action) {
            SelectionCaptureAction.NOTE -> pendingNoteQuote = cleaned
            SelectionCaptureAction.BOOKMARK -> {
              haptics.confirm()
              viewModel.addBookmarkWithExcerpt(
                bookId = book.id,
                chapterIndex = latestChapterIdx,
                chapterTitle = latestChapterTitle,
                pageIndex = latestBookmarkPage,
                excerpt = cleaned,
              )
              readerToastMessage = "Закладка с цитатой сохранена"
            }
            SelectionCaptureAction.SYSTEM_COPY -> Unit
          }
        }
      }
    }
  val isTextSelectionActive by rememberUpdatedState(
    readerToolbarController.status == TextToolbarStatus.Shown
  )

  CompositionLocalProvider(
    LocalTextToolbar provides readerToolbarController,
    LocalClipboardManager provides clipboardInterceptor,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
    // Invisible measurement scaffold matching exact statusBars/navigationBars/header/footer sizes
    // so text never collides with or gets covered by the bottom progress bar on any device.
    Box(modifier = Modifier.fillMaxSize().alpha(0f), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 680.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = prefs.horizontalPaddingDp.dp, vertical = 18.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
          Text(text = "Глава", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Box(
          modifier =
            Modifier.weight(1f).fillMaxWidth().onSizeChanged { measured ->
              if (measured != textViewportSize) {
                textViewportSize = measured
              }
            }
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.height(14.dp).weight(1f))
          Text(text = "100%", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL && !isPdf) {
      val baseScrollTextStyle =
        TextStyle(
          fontFamily = prefs.fontOption.fontFamily,
          fontSize = prefs.fontSizeSp.sp,
          lineHeight = (prefs.fontSizeSp * prefs.lineHeightMultiplier).sp,
          letterSpacing = prefs.letterSpacingSp.sp,
          color = textColor,
          textAlign = if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start,
        )

      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 680.dp)
              .statusBarsPadding()
              .navigationBarsPadding()
              .padding(horizontal = prefs.horizontalPaddingDp.dp, vertical = 18.dp),
        ) {
          // Subtle Chapter Header
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = activeChapterTitle,
              style = MaterialTheme.typography.labelSmall,
              color = secondaryColor.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "Глава ${activeChapterIdx + 1}/${parsedBook.chapters.size.coerceAtLeast(1)}",
              style = MaterialTheme.typography.labelSmall,
              color = secondaryColor.copy(alpha = 0.72f),
            )
          }

          // Scrollable content area + floating auto footnote popup window
          Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            LazyColumn(
              state = scrollListState,
              contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
              modifier =
                Modifier.fillMaxSize()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                  ) {
                    onToggleControls()
                  },
            ) {
              itemsIndexed(
                items = scrollNodes,
                key = { _, node ->
                  when (node) {
                    is ScrollNode.Paragraph -> "p_${node.chapterIndex}_${node.paragraphIndex}"
                    is ScrollNode.ChapterSpacer -> "s_${node.chapterIndex}"
                  }
                },
                contentType = { _, node ->
                  when (node) {
                    is ScrollNode.Paragraph -> "paragraph"
                    is ScrollNode.ChapterSpacer -> "spacer"
                  }
                },
              ) { nodeIndex, node ->
                when (node) {
                  is ScrollNode.ChapterSpacer -> Spacer(Modifier.height(22.dp))
                  is ScrollNode.Paragraph -> {
                    val para = node.paragraph
                    val rawStyled =
                      remember(
                        para,
                        prefs.fontOption,
                        prefs.fontSizeSp,
                        prefs.lineHeightMultiplier,
                        prefs.letterSpacingSp,
                        prefs.wordSpacingSp,
                        prefs.firstLineIndentSp,
                        prefs.textAlignJustify,
                        secondaryColor,
                        accentColor,
                      ) {
                        TextPaginator.buildScrollParagraphAnnotated(
                          paragraph = para,
                          prefs = prefs,
                          secondaryColor = secondaryColor,
                          accentColor = accentColor,
                        )
                      }

                    val interactiveText =
                      remember(rawStyled, parsedBook.footnotes, parsedBook.anchorTargets) {
                        attachInteractiveLinks(
                          source = rawStyled,
                          footnotes = parsedBook.footnotes,
                          anchorTargets = parsedBook.anchorTargets,
                          onFootnoteClick = { fn ->
                            haptics.tick()
                            selectedFootnoteDetail = fn
                          },
                          onAnchorClick = ::jumpToAnchor,
                        )
                      }

                    val chapterNotes = notesByChapter[node.chapterIndex].orEmpty()
                    val highlightedText =
                      remember(interactiveText, chapterNotes) {
                        attachNoteHighlights(
                          source = interactiveText,
                          chapterNotes = chapterNotes,
                          onNoteClick = { note ->
                            haptics.tick()
                            editingNote = note
                          },
                        )
                      }

                    val topPad = para.css.marginTopDp.coerceIn(0f, 20f).dp
                    val bottomPad = para.css.marginBottomDp.coerceIn(0f, 20f).dp
                    val startPad = para.css.marginStartDp.coerceIn(0f, 36f).dp

                    SelectionContainer {
                      Text(
                        text = highlightedText,
                        style = baseScrollTextStyle,
                        onTextLayout = { layoutResult ->
                          val refAnns =
                            rawStyled.getStringAnnotations(
                              TextPaginator.TAG_FOOTNOTE_REF,
                              0,
                              rawStyled.length,
                            )
                          if (refAnns.isNotEmpty()) {
                            val ranges =
                              refAnns.mapNotNull { ann ->
                                val lineIdx =
                                  layoutResult.getLineForOffset(
                                    ann.start.coerceIn(0, (rawStyled.length - 1).coerceAtLeast(0))
                                  )
                                val top = layoutResult.getLineTop(lineIdx)
                                val bot = layoutResult.getLineBottom(lineIdx)
                                ann.item to (top..bot)
                              }
                            scrollRefBounds[nodeIndex] = ranges
                          }
                        },
                        modifier =
                          Modifier.fillMaxWidth()
                            .padding(start = startPad, top = topPad, bottom = bottomPad),
                      )
                    }
                  }
                }
              }
            }

            // Auto-appearing/disappearing bottom footnote popup in Scroll ("Свиток") mode
            androidx.compose.animation.AnimatedVisibility(
              visible = visibleScrollFootnotes.isNotEmpty() && !showControls,
              enter =
                slideInVertically(
                  animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                  initialOffsetY = { it },
                ) + fadeIn(tween(180)),
              exit =
                slideOutVertically(
                  animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
                  targetOffsetY = { it },
                ) + fadeOut(tween(150)),
              modifier =
                Modifier.align(Alignment.BottomCenter)
                  .padding(top = 8.dp, bottom = 6.dp)
                  .testTag("scroll_footnote_popup"),
            ) {
              ScrollFootnotePopupCard(
                footnotes = visibleScrollFootnotes,
                accentColor = accentColor,
                onFootnoteClick = { fn ->
                  haptics.tick()
                  selectedFootnoteDetail = fn
                },
              )
            }
          }

          // Persistent non-overlapping Scroll Progress Bar ("Свиток")
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("scroll_progress_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ExpressiveWavyProgressIndicator(
              progress = scrollProgress,
              activeColor = accentColor.copy(alpha = 0.75f),
              trackColor = secondaryColor.copy(alpha = 0.16f),
              waveAmplitude = 1.6.dp,
              strokeWidth = 2.dp,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "${(scrollProgress * 100).roundToInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = secondaryColor.copy(alpha = 0.75f),
            )
          }
        }
      }
    } else if (pages.isNotEmpty() || isPdf) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .pointerInput(pages.size, isPdf) {
              if (isPdf) return@pointerInput
              awaitPointerEventScope {
                while (true) {
                  val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                  val downChange =
                    downEvent.changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
                  val downId = downChange.id
                  val downPos = downChange.position
                  val downTime = downChange.uptimeMillis
                  val wasScrollingOnDown = pagerState.isScrollInProgress
                  var slopExceeded = false
                  var upPos = downPos
                  var upTime = downTime
                  var trackedUp = false

                  while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } > 1) {
                      slopExceeded = true
                    }
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    if (!slopExceeded &&
                      (change.position - downPos).getDistance() > viewConfiguration.touchSlop
                    ) {
                      slopExceeded = true
                    }
                    if (!change.pressed) {
                      upPos = change.position
                      upTime = change.uptimeMillis
                      trackedUp = true
                      if (!slopExceeded && wasScrollingOnDown && (upTime - downTime) < 350L) {
                        change.consume()
                      }
                      break
                    }
                  }

                  if (trackedUp && !slopExceeded && (upTime - downTime) < 350L) {
                    val shouldFireTap =
                      if (wasScrollingOnDown) {
                        true
                      } else {
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        val finalChange = finalEvent.changes.firstOrNull { it.id == downId }
                        finalChange != null && !finalChange.isConsumed
                      }
                    if (shouldFireTap && !isTextSelectionActive) {
                      val width = size.width.toFloat()
                      when {
                        upPos.x < width * 0.28f -> turnPageBy(-1)
                        upPos.x > width * 0.72f -> turnPageBy(1)
                        else -> onToggleControls()
                      }
                    }
                  }
                }
              }
            }
      ) {
        HorizontalPager(
          state = pagerState,
          beyondViewportPageCount = 1,
          userScrollEnabled = !isTextSelectionActive,
          modifier = Modifier.fillMaxSize().testTag("reader_pager"),
        ) { pageIdx ->
          val page = pages.getOrNull(pageIdx) ?: return@HorizontalPager
          val pageOffset =
            (pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction

          Box(
            modifier =
              Modifier.fillMaxSize()
                .applyPageFlipTransition(
                  animation = prefs.pageAnimation,
                  pageOffset = pageOffset,
                  bgColor = bgColor,
                )
                .let { mod ->
                  if (isPdf) {
                    mod.pointerInput(pages.size) {
                      detectTapGestures(
                        onTap = { tapOffset ->
                          val width = size.width.toFloat()
                          when {
                            tapOffset.x < width * 0.28f -> turnPageBy(-1)
                            tapOffset.x > width * 0.72f -> turnPageBy(1)
                            else -> onToggleControls()
                          }
                        }
                      )
                    }
                  } else {
                    mod
                  }
                },
            contentAlignment = Alignment.TopCenter,
          ) {
            if (isPdf) {
              PdfPageCanvas(
                filePath = book.filePath,
                pageIndex = pageIdx,
                viewModel = viewModel,
                onCenterTap = onToggleControls,
              )
            } else {
              val baseTextStyle =
                TextStyle(
                  fontFamily = prefs.fontOption.fontFamily,
                  fontSize = prefs.fontSizeSp.sp,
                  lineHeight = (prefs.fontSizeSp * prefs.lineHeightMultiplier).sp,
                  letterSpacing = prefs.letterSpacingSp.sp,
                  color = textColor,
                  textAlign = if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start,
                )

              Column(
                modifier =
                  Modifier.fillMaxSize()
                    .widthIn(max = 680.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = prefs.horizontalPaddingDp.dp, vertical = 18.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = page.chapterTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "${pageIdx + 1} / ${pages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor.copy(alpha = 0.72f),
                  )
                }

                Column(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                  val chapterNotes = notesByChapter[page.chapterIndex].orEmpty()
                  SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
                    ) {
                      page.paragraphs.forEach { para ->
                        val liveParagraphText =
                          remember(
                            para,
                            prefs.letterSpacingSp,
                            prefs.wordSpacingSp,
                            prefs.firstLineIndentSp,
                            prefs.textAlignJustify,
                            secondaryColor,
                            accentColor,
                          ) {
                            TextPaginator.applyLiveStylingToParagraph(
                              paragraph = para,
                              prefs = prefs,
                              secondaryColor = secondaryColor,
                              accentColor = accentColor,
                            )
                          }
                        val interactiveParagraphText =
                          remember(
                            liveParagraphText,
                            parsedBook.footnotes,
                            parsedBook.anchorTargets,
                          ) {
                            attachInteractiveLinks(
                              source = liveParagraphText,
                              footnotes = parsedBook.footnotes,
                              anchorTargets = parsedBook.anchorTargets,
                              onFootnoteClick = { fn ->
                                haptics.tick()
                                selectedFootnoteDetail = fn
                              },
                              onAnchorClick = ::jumpToAnchor,
                            )
                          }
                        val highlightedParagraphText =
                          remember(interactiveParagraphText, chapterNotes) {
                            attachNoteHighlights(
                              source = interactiveParagraphText,
                              chapterNotes = chapterNotes,
                              onNoteClick = { note ->
                                haptics.tick()
                                editingNote = note
                              },
                            )
                          }
                        val topPad =
                          if (!para.isContinuation) para.css.marginTopDp.coerceIn(0f, 16f).dp else 0.dp
                        val startPad = para.css.marginStartDp.coerceIn(0f, 36f).dp
                        Text(
                          text = highlightedParagraphText,
                          style = baseTextStyle,
                          modifier = Modifier.fillMaxWidth().padding(start = startPad, top = topPad),
                        )
                      }
                    }
                  }

                if (page.footnotes.isNotEmpty()) {
                  PageFootnotesFooter(
                    footnotes = page.footnotes,
                    fontSizeSp = prefs.fontSizeSp,
                    fontFamily = prefs.fontOption.fontFamily,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    onFootnoteClick = { fn ->
                      haptics.tick()
                      selectedFootnoteDetail = fn
                    },
                  )
                }
              }

              val pct =
                if (pages.isEmpty()) 0f else (pageIdx + 1).toFloat() / pages.size.toFloat()
              Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                ExpressiveWavyProgressIndicator(
                  progress = pct,
                  activeColor = accentColor.copy(alpha = 0.75f),
                  trackColor = secondaryColor.copy(alpha = 0.16f),
                  waveAmplitude = 1.6.dp,
                  strokeWidth = 2.dp,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${(pct * 100).roundToInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = secondaryColor.copy(alpha = 0.75f),
                )
              }
            }
          }
        }
      }
    }
  }

    // Floating Expressive Top Toolbar:
    AnimatedVisibility(
      visible = showControls,
      enter =
        slideInVertically(
          animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
          initialOffsetY = { -it },
        ),
      exit =
        slideOutVertically(
          animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
          targetOffsetY = { -it },
        ),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      Box(modifier = Modifier.statusBarsPadding().padding(14.dp)) {
        Surface(
          shape = RoundedCornerShape(30.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          tonalElevation = 6.dp,
          shadowElevation = 6.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(
              onClick = {
                haptics.tap()
                onNavigateBack()
              },
              modifier = Modifier.testTag("reader_back_button"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
              Text(
                text = book.title,
                fontFamily = UnboundedFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = activeChapterTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(
              onClick = {
                haptics.tap()
                tocInitialTab = 2
                onOpenToc()
              },
              modifier = Modifier.testTag("reader_notes_button"),
            ) {
              NoteIllustrationBadge(
                palette = NoteHighlightPalette[0],
                modifier = Modifier.size(22.dp),
              )
            }
            IconButton(
              onClick = {
                haptics.confirm()
                viewModel.toggleBookmark(
                  bookId = book.id,
                  chapterIndex = activeChapterIdx,
                  chapterTitle = activeChapterTitle,
                  pageIndex = activeBookmarkPage,
                  excerpt =
                    activePage?.previewExcerpt
                      ?: (activeScrollNode as? ScrollNode.Paragraph)?.paragraph?.text?.take(120)
                      ?: activeChapterTitle,
                )
              },
              modifier = Modifier.testTag("reader_bookmark_button"),
            ) {
              Icon(
                imageVector =
                  if (isCurrentBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Закладка",
                tint =
                  if (isCurrentBookmarked) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // Floating Expressive Bottom Dock
    AnimatedVisibility(
      visible = showControls,
      enter =
        slideInVertically(
          animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
          initialOffsetY = { it },
        ),
      exit =
        slideOutVertically(
          animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
          targetOffsetY = { it },
        ),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      Box(modifier = Modifier.navigationBarsPadding().padding(14.dp)) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          tonalElevation = 6.dp,
          shadowElevation = 6.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (pages.size > 1 && prefs.pageAnimation != PageFlipAnimation.VERTICAL_SCROLL) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "${currentVisiblePage + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
                Slider(
                  value =
                    currentVisiblePage
                      .toFloat()
                      .coerceIn(0f, pages.lastIndex.toFloat().coerceAtLeast(1f)),
                  onValueChange = { target ->
                    val intPage = target.roundToInt().coerceIn(0, pages.lastIndex)
                    if (intPage != currentVisiblePage) {
                      haptics.tick()
                      pageTurnJob?.cancel()
                      targetTurnPage = intPage
                      pageTurnJob = scope.launch { pagerState.scrollToPage(intPage) }
                    }
                  },
                  valueRange = 0f..pages.lastIndex.toFloat().coerceAtLeast(1f),
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${pages.size}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL && scrollNodes.size > 1) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "${(scrollProgress * 100).roundToInt()}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
                Slider(
                  value = scrollProgress,
                  onValueChange = { targetPct ->
                    val targetIdx =
                      (targetPct * scrollNodes.lastIndex).roundToInt().coerceIn(0, scrollNodes.lastIndex)
                    scope.launch { scrollListState.scrollToItem(targetIdx) }
                  },
                  valueRange = 0f..1f,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "100%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              ReaderDockAction(
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                label = "Главы",
                onClick = {
                  haptics.tap()
                  tocInitialTab = 0
                  onOpenToc()
                },
                modifier = Modifier.testTag("reader_toc_button"),
              )
              ReaderDockAction(
                icon = Icons.Rounded.FormatSize,
                label = "Шрифты",
                onClick = {
                  haptics.tap()
                  onOpenFonts()
                },
                modifier = Modifier.testTag("reader_fonts_button"),
              )
              ReaderDockAction(
                icon = Icons.Rounded.ColorLens,
                label = "Стиль",
                onClick = {
                  haptics.tap()
                  onOpenThemes()
                },
                modifier = Modifier.testTag("reader_style_button"),
              )
            }
          }
        }
      }
    }

    // Floating custom selection action pill ("Заметка" with picture, "Закладка", "Копия")
    ReaderSelectionToolbarOverlay(
      controller = readerToolbarController,
      onCreateNoteClick = {
        haptics.morph()
        clipboardInterceptor.pendingAction = SelectionCaptureAction.NOTE
        readerToolbarController.onCopyRequested?.invoke()
      },
      onCreateBookmarkClick = {
        haptics.confirm()
        clipboardInterceptor.pendingAction = SelectionCaptureAction.BOOKMARK
        readerToolbarController.onCopyRequested?.invoke()
      },
      onCopyClick = {
        haptics.tick()
        clipboardInterceptor.pendingAction = SelectionCaptureAction.SYSTEM_COPY
        readerToolbarController.onCopyRequested?.invoke()
        readerToastMessage = "Текст скопирован"
      },
    )

    // Brief non-blocking confirmation pill when adding a quote bookmark or copying text
    AnimatedVisibility(
      visible = readerToastMessage != null,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(140)),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(140)),
      modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
      ) {
        Text(
          text = readerToastMessage.orEmpty(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
      }
    }

    if (showTocSheet) {
      ReaderTocBookmarksSheet(
        chapters = parsedBook.chapters,
        currentChapterIndex = activeChapterIdx,
        bookmarks = bookmarks,
        notes = notes,
        initialTab = tocInitialTab,
        onSelectChapter = { chIdx ->
          if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) {
            val nodeIdx = scrollNodes.indexOfFirst { it.chapterIndex == chIdx }.coerceAtLeast(0)
            scope.launch { scrollListState.animateScrollToItem(nodeIdx) }
          } else {
            val targetPage = pages.indexOfFirst { it.chapterIndex == chIdx }.coerceAtLeast(0)
            pageTurnJob?.cancel()
            targetTurnPage = targetPage
            pageTurnJob = scope.launch { pagerState.animateScrollToPage(targetPage) }
          }
        },
        onSelectBookmark = { mark ->
          val targetPage =
            pages
              .indexOfFirst {
                it.chapterIndex == mark.chapterIndex && it.pageInChapter == mark.pageIndex
              }
              .takeIf { it >= 0 }
              ?: pages.indexOfFirst { it.chapterIndex == mark.chapterIndex }.coerceAtLeast(0)
          if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) {
            val chStart = scrollNodes.indexOfFirst { it.chapterIndex == mark.chapterIndex }.coerceAtLeast(0)
            scope.launch { scrollListState.animateScrollToItem(chStart) }
          } else {
            pageTurnJob?.cancel()
            targetTurnPage = targetPage
            pageTurnJob = scope.launch { pagerState.animateScrollToPage(targetPage) }
          }
        },
        onSelectNote = { note ->
          val targetPage =
            pages
              .indexOfFirst {
                it.chapterIndex == note.chapterIndex && it.pageInChapter == note.pageIndex
              }
              .takeIf { it >= 0 }
              ?: pages.indexOfFirst { it.chapterIndex == note.chapterIndex }.coerceAtLeast(0)
          if (prefs.pageAnimation == PageFlipAnimation.VERTICAL_SCROLL) {
            val chStart = scrollNodes.indexOfFirst { it.chapterIndex == note.chapterIndex }.coerceAtLeast(0)
            scope.launch { scrollListState.animateScrollToItem(chStart) }
          } else {
            pageTurnJob?.cancel()
            targetTurnPage = targetPage
            pageTurnJob = scope.launch { pagerState.animateScrollToPage(targetPage) }
          }
        },
        onEditNote = { note -> editingNote = note },
        onDeleteNote = viewModel::deleteNote,
        onAddCurrentBookmark = {
          viewModel.toggleBookmark(
            bookId = book.id,
            chapterIndex = activeChapterIdx,
            chapterTitle = activeChapterTitle,
            pageIndex = activeBookmarkPage,
            excerpt =
              activePage?.previewExcerpt
                ?: (activeScrollNode as? ScrollNode.Paragraph)?.paragraph?.text?.take(120)
                ?: activeChapterTitle,
          )
        },
        onDeleteBookmark = viewModel::deleteBookmark,
        onDismiss = onDismissToc,
      )
    }

    pendingNoteQuote?.let { quote ->
      NoteEditorDialog(
        quoteText = quote,
        initialNoteText = "",
        initialColorIndex = 0,
        isEditing = false,
        onSave = { noteBody, colorIdx ->
          viewModel.addNote(
            bookId = book.id,
            chapterIndex = activeChapterIdx,
            chapterTitle = activeChapterTitle,
            pageIndex = activeBookmarkPage,
            selectedText = quote,
            noteText = noteBody,
            colorIndex = colorIdx,
          )
          pendingNoteQuote = null
          readerToastMessage = "Заметка добавлена"
        },
        onDismiss = { pendingNoteQuote = null },
      )
    }

    editingNote?.let { existingNote ->
      NoteEditorDialog(
        quoteText = existingNote.selectedText,
        initialNoteText = existingNote.noteText,
        initialColorIndex = existingNote.colorIndex,
        isEditing = true,
        onSave = { noteBody, colorIdx ->
          viewModel.updateNote(
            noteId = existingNote.id,
            noteText = noteBody,
            colorIndex = colorIdx,
          )
          editingNote = null
        },
        onDelete = {
          viewModel.deleteNote(existingNote.id)
          editingNote = null
        },
        onDismiss = { editingNote = null },
      )
    }

    selectedFootnoteDetail?.let { note ->
      AlertDialog(
        onDismissRequest = { selectedFootnoteDetail = null },
        shape = RoundedCornerShape(28.dp),
        title = {
          val marker =
            if (note.marker.startsWith("[")) note.marker else "[${note.marker}]"
          Text(
            text = "Примечание $marker",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            Text(
              text = note.text,
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              haptics.tick()
              selectedFootnoteDetail = null
            },
            shape = CircleShape,
            modifier = Modifier.testTag("footnote_close_button"),
          ) {
            Text("Закрыть", fontWeight = FontWeight.Bold)
          }
        },
      )
    }
    }
  }
}

private fun attachInteractiveLinks(
  source: AnnotatedString,
  footnotes: Map<String, BookFootnote>,
  anchorTargets: Map<String, BookAnchorTarget>,
  onFootnoteClick: (BookFootnote) -> Unit,
  onAnchorClick: (BookAnchorTarget) -> Unit,
): AnnotatedString {
  val fnAnns = source.getStringAnnotations(TextPaginator.TAG_FOOTNOTE_REF, 0, source.length)
  val linkAnns = source.getStringAnnotations(TextPaginator.TAG_CHAPTER_LINK, 0, source.length)
  if (fnAnns.isEmpty() && linkAnns.isEmpty()) return source
  return buildAnnotatedString {
    append(source)
    if (footnotes.isNotEmpty()) {
      fnAnns.forEach { ann ->
        val resolved = TextPaginator.resolveFootnote(ann.item, footnotes)
        if (resolved != null) {
          addLink(
            clickable =
              LinkAnnotation.Clickable(
                tag = TextPaginator.TAG_FOOTNOTE_REF,
                linkInteractionListener = { onFootnoteClick(resolved) },
              ),
            start = ann.start,
            end = ann.end,
          )
        }
      }
    }
    if (anchorTargets.isNotEmpty()) {
      linkAnns.forEach { ann ->
        val resolved = TextPaginator.resolveAnchorTarget(ann.item, anchorTargets)
        if (resolved != null) {
          addLink(
            clickable =
              LinkAnnotation.Clickable(
                tag = TextPaginator.TAG_CHAPTER_LINK,
                linkInteractionListener = { onAnchorClick(resolved) },
              ),
            start = ann.start,
            end = ann.end,
          )
        }
      }
    }
  }
}

@Composable
private fun PageFootnotesFooter(
  footnotes: List<BookFootnote>,
  fontSizeSp: Float,
  fontFamily: FontFamily,
  secondaryColor: Color,
  accentColor: Color,
  onFootnoteClick: (BookFootnote) -> Unit,
) {
  val fnSize = (fontSizeSp * 0.80f).coerceIn(11f, 16f).sp
  val fnLineHeight = (fnSize.value * 1.35f).sp
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 8.dp)
        .testTag("page_footnotes_footer"),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    HorizontalDivider(
      thickness = 1.dp,
      color = secondaryColor.copy(alpha = 0.25f),
      modifier = Modifier.fillMaxWidth(0.45f).padding(bottom = 4.dp),
    )
    footnotes.forEach { note ->
      val markerPrefix =
        if (note.marker.startsWith("[")) note.marker else "[${note.marker}]"
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onFootnoteClick(note) }
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = markerPrefix,
          fontFamily = fontFamily,
          fontSize = fnSize,
          lineHeight = fnLineHeight,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        Text(
          text = note.text,
          fontFamily = fontFamily,
          fontSize = fnSize,
          lineHeight = fnLineHeight,
          color = secondaryColor,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun ScrollFootnotePopupCard(
  footnotes: List<BookFootnote>,
  accentColor: Color,
  onFootnoteClick: (BookFootnote) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shadowElevation = 6.dp,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    AnimatedContent(
      targetState = footnotes,
      transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
      label = "scrollFootnoteContent",
    ) { activeNotes ->
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        activeNotes.take(2).forEach { note ->
          val markerPrefix =
            if (note.marker.startsWith("[")) note.marker else "[${note.marker}]"
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onFootnoteClick(note) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Surface(
              shape = CircleShape,
              color = accentColor.copy(alpha = 0.16f),
            ) {
              Text(
                text = markerPrefix,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Text(
              text = note.text,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReaderDockAction(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun PdfPageCanvas(
  filePath: String,
  pageIndex: Int,
  viewModel: ReaderViewModel,
  onCenterTap: () -> Unit,
) {
  var bitmap by remember(filePath, pageIndex) { mutableStateOf<Bitmap?>(null) }
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  LaunchedEffect(filePath, pageIndex) {
    bitmap = viewModel.loadPdfPage(filePath, pageIndex, 1280)
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              if (event.changes.size >= 2 || scale > 1.05f) {
                var zoomChange = 1f
                var panChange = Offset.Zero
                if (event.changes.size >= 2) {
                  val p0 = event.changes[0]
                  val p1 = event.changes[1]
                  val prevDist = (p0.previousPosition - p1.previousPosition).getDistance()
                  val currDist = (p0.position - p1.position).getDistance()
                  if (prevDist > 0f) zoomChange = currDist / prevDist
                  val prevCenter = (p0.previousPosition + p1.previousPosition) / 2f
                  val currCenter = (p0.position + p1.position) / 2f
                  panChange = currCenter - prevCenter
                  event.changes.forEach { it.consume() }
                } else if (scale > 1.05f) {
                  val p0 = event.changes[0]
                  if (p0.pressed && p0.previousPressed) {
                    panChange = p0.position - p0.previousPosition
                    if (panChange != Offset.Zero) p0.consume()
                  }
                }
                val newScale = (scale * zoomChange).coerceIn(1f, 3.5f)
                scale = newScale
                offset = if (newScale > 1.05f) offset + panChange else Offset.Zero
              }
            }
          }
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onDoubleTap = {
              if (scale > 1.2f) {
                scale = 1f
                offset = Offset.Zero
              } else {
                scale = 2.1f
              }
            },
            onTap = { onCenterTap() },
          )
        },
    contentAlignment = Alignment.Center,
  ) {
    val bmp = bitmap
    if (bmp == null) {
      CircularProgressIndicator()
    } else {
      Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "PDF страница ${pageIndex + 1}",
        contentScale = ContentScale.Fit,
        modifier =
          Modifier.fillMaxSize()
            .padding(12.dp)
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offset.x
              translationY = offset.y
            },
      )
    }
  }
}

private fun Modifier.applyPageFlipTransition(
  animation: PageFlipAnimation,
  pageOffset: Float,
  bgColor: Color,
): Modifier {
  val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)
  return when (animation) {
    PageFlipAnimation.CURL_3D ->
      this.graphicsLayer {
          cameraDistance = 18f * density
          if (pageOffset < 0f) {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = (-pageOffset * 82f).coerceIn(0f, 95f)
            alpha = (1f - absOffset * 0.55f).coerceIn(0f, 1f)
            val s = 1f - absOffset * 0.08f
            scaleX = s
            scaleY = s
          } else {
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            translationX = -pageOffset * size.width * 0.14f
            val s = 0.94f + 0.06f * (1f - absOffset.coerceAtMost(1f))
            scaleX = s
            scaleY = s
            alpha = (1f - absOffset * 0.25f).coerceIn(0f, 1f)
          }
        }
        .drawWithContent {
          drawContent()
          if (absOffset > 0.01f) {
            val shadowAlpha = (absOffset * 0.28f).coerceIn(0f, 0.35f)
            if (pageOffset < 0f) {
              drawRect(
                brush =
                  Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    startX = size.width * 0.6f,
                    endX = size.width,
                  )
              )
            } else {
              drawRect(
                color = bgColor.copy(alpha = (absOffset * 0.2f).coerceIn(0f, 0.3f)),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
              )
            }
          }
        }

    PageFlipAnimation.DEPTH_SLIDE ->
      this.graphicsLayer {
        if (pageOffset <= 0f) {
          translationX = 0f
          alpha = 1f - absOffset * 0.2f
        } else {
          translationX = -pageOffset * size.width * 0.65f
          val s = 0.86f + 0.14f * (1f - absOffset.coerceAtMost(1f))
          scaleX = s
          scaleY = s
          alpha = (1f - absOffset).coerceIn(0f, 1f)
        }
      }

    PageFlipAnimation.VERTICAL_SCROLL -> this
  }
}

private class ReaderThemeSnapshot(
  var bg: Color,
  var text: Color,
  var secondary: Color,
  var accent: Color,
)

private tailrec fun Context.findReaderActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findReaderActivity()
    else -> null
  }
