package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BookDeleteRequest
import com.example.ui.components.BookLaunchOrigin
import com.example.ui.components.BookReaderMorphContainer
import com.example.ui.components.ExpressiveDeleteBookOverlay
import com.example.ui.components.ExpressiveFloatingNavBar
import com.example.ui.components.ExpressiveNavRail
import com.example.ui.components.MainNavTab
import com.example.ui.screens.AppearanceScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.ListayTheme
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.ReaderFontOption
import com.example.ui.viewmodel.AppearanceViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { ListayApp() }
  }
}

@Composable
fun ListayApp() {
  val appearanceViewModel: AppearanceViewModel = viewModel()
  val libraryViewModel: LibraryViewModel = viewModel()
  val readerViewModel: ReaderViewModel = viewModel()

  val appearanceState by appearanceViewModel.uiState.collectAsStateWithLifecycle()
  val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
  val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
  val prefs = appearanceState.preferences
  val fontFamilyResolver = LocalFontFamilyResolver.current

  LaunchedEffect(fontFamilyResolver) {
    withContext(Dispatchers.Default) {
      ReaderFontOption.entries.forEach { option ->
        runCatching { fontFamilyResolver.preload(option.fontFamily) }
      }
    }
  }

  ListayTheme(
    palette = prefs.appPalette,
    darkModeSetting = prefs.darkModeSetting,
    hapticsEnabled = prefs.hapticsEnabled,
  ) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalExpressiveHaptics.current
    val scope = rememberCoroutineScope()
    val tabPagerState =
      rememberPagerState(
        initialPage = 0,
        pageCount = { MainNavTab.entries.size },
      )

    var activeBookLaunch by remember { mutableStateOf<BookLaunchOrigin?>(null) }
    val bookMorphAnim = remember { Animatable(0f) }
    val closingVeilAnim = remember { Animatable(1f) }
    var bookMorphJob by remember { mutableStateOf<Job?>(null) }
    var isClosingBook by remember { mutableStateOf(false) }

    var deleteRequest by remember { mutableStateOf<BookDeleteRequest?>(null) }

    fun openBookWithMorph(origin: BookLaunchOrigin) {
      bookMorphJob?.cancel()
      isClosingBook = false
      activeBookLaunch = origin
      bookMorphJob =
        scope.launch {
          closingVeilAnim.snapTo(1f)
          bookMorphAnim.snapTo(0f)
          bookMorphAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 310f),
          )
        }
    }

    fun closeBookWithMorph() {
      if (activeBookLaunch == null || isClosingBook) return
      bookMorphJob?.cancel()
      isClosingBook = true
      bookMorphJob =
        scope.launch {
          closingVeilAnim.snapTo(1f)
          bookMorphAnim.animateTo(
            targetValue = 0f,
            animationSpec =
              spring(
                dampingRatio = 0.94f,
                stiffness = 390f,
                visibilityThreshold = 0.002f,
              ),
          )
          if (activeBookLaunch?.sourceRect != null) {
            closingVeilAnim.animateTo(
              targetValue = 0f,
              animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
          }
          activeBookLaunch = null
          isClosingBook = false
          closingVeilAnim.snapTo(1f)
        }
    }

    BackHandler(enabled = activeBookLaunch != null && deleteRequest == null) {
      closeBookWithMorph()
    }

    val openDocumentLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
      ) { uri ->
        if (uri != null) {
          haptics.confirm()
          libraryViewModel.importBook(uri) { importedBook ->
            openBookWithMorph(BookLaunchOrigin(book = importedBook, sourceRect = null))
          }
        }
      }

    LaunchedEffect(libraryState.message) {
      val msg = libraryState.message ?: return@LaunchedEffect
      snackbarHostState.showSnackbar(msg)
      libraryViewModel.clearMessage()
    }

    val tabIcons = remember {
      mapOf(
        MainNavTab.LIBRARY to (Icons.AutoMirrored.Rounded.MenuBook to Icons.AutoMirrored.Outlined.MenuBook),
        MainNavTab.APPEARANCE to (Icons.Rounded.Palette to Icons.Outlined.Palette),
      )
    }

    val selectedTab =
      MainNavTab.entries.getOrElse(tabPagerState.targetPage) { MainNavTab.LIBRARY }

    val onSelectTab: (MainNavTab) -> Unit = { tab ->
      val targetIndex = MainNavTab.entries.indexOf(tab).coerceAtLeast(0)
      if (activeBookLaunch != null) {
        closeBookWithMorph()
      }
      scope.launch {
        tabPagerState.animateScrollToPage(
          page = targetIndex,
          animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
        )
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 720.dp

      Scaffold(
        snackbarHost = {
          SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
              snackbarData = data,
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier.padding(bottom = if (!useNavRail) 84.dp else 12.dp),
            )
          }
        }
      ) { _ ->
        Row(modifier = Modifier.fillMaxSize()) {
          if (useNavRail) {
            ExpressiveNavRail(
              selectedTab = selectedTab,
              icons = tabIcons,
              onSelectTab = onSelectTab,
            )
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            HorizontalPager(
              state = tabPagerState,
              beyondViewportPageCount = 1,
              userScrollEnabled = activeBookLaunch == null && deleteRequest == null,
              modifier = Modifier.fillMaxSize(),
            ) { page ->
              when (page) {
                0 ->
                  LibraryScreen(
                    uiState = libraryState,
                    onOpenBook = ::openBookWithMorph,
                    onImportClick = {
                      openDocumentLauncher.launch(
                        arrayOf(
                          "application/epub+zip",
                          "application/pdf",
                          "text/plain",
                          "text/html",
                          "text/markdown",
                          "application/x-fictionbook+xml",
                          "application/octet-stream",
                          "*/*",
                        )
                      )
                    },
                    onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                    onFilterSelected = libraryViewModel::onFilterSelected,
                    onToggleFavorite = libraryViewModel::toggleFavorite,
                    onRequestDeleteBook = { request -> deleteRequest = request },
                  )
                else ->
                  AppearanceScreen(
                    uiState = appearanceState,
                    onPaletteSelected = appearanceViewModel::setAppPalette,
                    onDarkModeSelected = appearanceViewModel::setDarkMode,
                    onReaderThemeSelected = appearanceViewModel::setReaderTheme,
                    onFontSelected = appearanceViewModel::setFontOption,
                    onPageAnimSelected = appearanceViewModel::setPageAnimation,
                    onHapticsToggle = appearanceViewModel::setHapticsEnabled,
                    onKeepScreenOnToggle = appearanceViewModel::setKeepScreenOn,
                  )
              }
            }

            if (!useNavRail) {
              val navBarVisible = activeBookLaunch == null
              val navBarProgress by
                animateFloatAsState(
                  targetValue = if (navBarVisible) 1f else 0f,
                  animationSpec = spring(dampingRatio = 0.80f, stiffness = 400f),
                  label = "floatingNavSlide",
                )
              val density = LocalDensity.current
              ExpressiveFloatingNavBar(
                selectedTab = selectedTab,
                selectionProgress = {
                  tabPagerState.currentPage + tabPagerState.currentPageOffsetFraction
                },
                icons = tabIcons,
                onSelectTab = onSelectTab,
                modifier =
                  Modifier.align(Alignment.BottomCenter).graphicsLayer {
                    val hiddenOffsetPx = with(density) { 128.dp.toPx() }
                    translationY = (1f - navBarProgress) * hiddenOffsetPx
                    alpha = navBarProgress.coerceIn(0f, 1f)
                  },
              )
            }
          }
        }
      }

      // Container-transform morph overlay from clicked book card (or Hero card) to full-screen Reader
      val launchOrigin = activeBookLaunch
      if (launchOrigin != null) {
        val readerTheme = readerState.preferences.readerTheme
        val readerBgColor =
          if (readerTheme.useSystemDynamic) MaterialTheme.colorScheme.surfaceContainerLowest
          else readerTheme.background

        BookReaderMorphContainer(
          launchOrigin = launchOrigin,
          morphProgress = bookMorphAnim.value,
          isClosing = isClosingBook,
          closingVeilAlpha = closingVeilAnim.value,
          readerBgColor = readerBgColor,
        ) { isMorphSettled ->
          ReaderScreen(
            bookId = launchOrigin.book.id,
            uiState = readerState,
            viewModel = readerViewModel,
            onNavigateBack = ::closeBookWithMorph,
            showLoadingSpinner = isMorphSettled,
            enableBackHandler = false,
          )
        }
      }

      // Physically unfolding book-deletion dialog originating from the tapped trash icon button
      val pendingDelete = deleteRequest
      if (pendingDelete != null) {
        ExpressiveDeleteBookOverlay(
          request = pendingDelete,
          onConfirmDelete = { book -> libraryViewModel.deleteBook(book) },
          onDismissComplete = { deleteRequest = null },
        )
      }
    }
  }
}
