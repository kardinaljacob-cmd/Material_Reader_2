package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.ui.components.BookCoverArtwork
import com.example.ui.components.BookDeleteRequest
import com.example.ui.components.BookLaunchOrigin
import com.example.ui.components.ExpressiveWavyProgressIndicator
import com.example.ui.components.MorphingExpressiveButton
import com.example.ui.components.MorphingFilterChip
import com.example.ui.theme.ExpressiveScallopShape
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.UnboundedFamily
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.LibraryUiState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val LibraryGridCardShape = RoundedCornerShape(26.dp)
private val LibraryCoverShape = RoundedCornerShape(20.dp)
private val HeroCardCoverShape = RoundedCornerShape(22.dp)
private val SearchFieldShape = RoundedCornerShape(28.dp)
private val TrashButtonShape = RoundedCornerShape(10.dp)
private val HeaderBadgeScallop = ExpressiveScallopShape(lobes = 9, scallopDepth = 0.14f)
private val BookFavScallop = ExpressiveScallopShape(lobes = 7, scallopDepth = 0.14f)
private val EmptyCardScallop = ExpressiveScallopShape(lobes = 8, scallopDepth = 0.14f)

@Composable
fun LibraryScreen(
  uiState: LibraryUiState,
  onOpenBook: (BookLaunchOrigin) -> Unit,
  onImportClick: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onFilterSelected: (LibraryFilter) -> Unit,
  onToggleFavorite: (Long) -> Unit,
  onRequestDeleteBook: (BookDeleteRequest) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalExpressiveHaptics.current
  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()
  var searchScrollJob by remember { mutableStateOf<Job?>(null) }

  fun scrollSearchBarToTopSmoothly(searchItemIndex: Int) {
    searchScrollJob?.cancel()
    searchScrollJob =
      scope.launch {
        val visibleSearch =
          gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == searchItemIndex }
        if (visibleSearch != null) {
          val deltaY = visibleSearch.offset.y.toFloat()
          if (deltaY.absoluteValue > 2f) {
            gridState.animateScrollBy(
              value = deltaY,
              animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
            )
          }
        } else {
          gridState.animateScrollToItem(index = searchItemIndex)
        }
      }
  }

  val primaryTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
  val bgColor = MaterialTheme.colorScheme.background
  val secondaryTint = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.14f)
  val bgBrush =
    remember(primaryTint, bgColor, secondaryTint) {
      Brush.verticalGradient(colors = listOf(primaryTint, bgColor, secondaryTint))
    }

  BoxWithConstraints(modifier = modifier.fillMaxSize().background(bgBrush)) {
    val isExpanded = maxWidth >= 720.dp
    val columns = if (maxWidth >= 900.dp) 3 else 2

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(columns),
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .align(Alignment.TopCenter)
          .widthIn(max = if (isExpanded) 1100.dp else 640.dp),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "library_header", span = { GridItemSpan(columns) }, contentType = "header") {
        LibraryHeader(
          isImporting = uiState.isImporting,
          onImportClick = onImportClick,
        )
      }

      // Always keep the last-read HeroBookCard pinned at top during search & section filtering
      // so the search field never shifts index or closes the IME keyboard.
      val hero = uiState.heroBook
      if (hero != null) {
        item(key = "hero_book_card", span = { GridItemSpan(columns) }, contentType = "hero") {
          HeroBookCard(
            book = hero,
            onContinueClick = { bounds ->
              onOpenBook(
                BookLaunchOrigin(
                  book = hero,
                  sourceRect = bounds,
                  cornerRadiusDp = 34.dp,
                  isHero = true,
                )
              )
            },
          )
        }
      }

      item(key = "search_and_filters", span = { GridItemSpan(columns) }, contentType = "search") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          val searchItemIndex = if (uiState.heroBook != null) 2 else 1
          ExpressiveSearchField(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onActivate = { scrollSearchBarToTopSmoothly(searchItemIndex) },
          )
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            LibraryFilter.entries.forEach { filter ->
              MorphingFilterChip(
                label = filter.label,
                selected = filter == uiState.selectedFilter,
                leadingIcon = if (filter == LibraryFilter.FAVORITES) Icons.Rounded.Favorite else null,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
              )
            }
          }
        }
      }

      item(key = "collection_title", span = { GridItemSpan(columns) }, contentType = "section_title") {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AnimatedContent(
            targetState =
              if (uiState.selectedFilter == LibraryFilter.ALL) "Ваша коллекция"
              else uiState.selectedFilter.label,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "sectionTitleAnim",
          ) { titleText ->
            Text(
              text = titleText,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          ) {
            AnimatedContent(
              targetState = uiState.filteredBooks.size,
              transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
              label = "sectionCountAnim",
            ) { count ->
              Text(
                text = "$count книг",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      if (uiState.isLoaded && uiState.filteredBooks.isEmpty()) {
        item(key = "empty_library", span = { GridItemSpan(columns) }, contentType = "empty") {
          EmptyLibraryState(
            onImportClick = onImportClick,
            modifier =
              Modifier.animateItem(
                fadeInSpec = tween(200),
                fadeOutSpec = tween(140),
              ),
          )
        }
      } else {
        items(
          items = uiState.filteredBooks,
          key = { it.id },
          contentType = { "book_card" },
        ) { book ->
          ExpressiveBookGridCard(
            book = book,
            onOpen = { bounds ->
              onOpenBook(
                BookLaunchOrigin(
                  book = book,
                  sourceRect = bounds,
                  cornerRadiusDp = 26.dp,
                  isHero = false,
                )
              )
            },
            onToggleFavorite = {
              haptics.morph()
              onToggleFavorite(book.id)
            },
            onRequestDelete = { deleteBounds ->
              haptics.heavy()
              onRequestDeleteBook(BookDeleteRequest(book = book, sourceRect = deleteBounds))
            },
            modifier =
              Modifier.animateItem(
                fadeInSpec = tween(220),
                fadeOutSpec = tween(160),
                placementSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
              ),
          )
        }
      }
    }
  }
}

@Composable
private fun LibraryHeader(
  isImporting: Boolean,
  onImportClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f),
    ) {
      Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .clip(HeaderBadgeScallop)
              .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.MenuBook,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(24.dp),
        )
      }
      Column {
        Text(
          text = "Material Reader",
          fontFamily = UnboundedFamily,
          fontSize = 21.sp,
          lineHeight = 25.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "EPUB • FB2 • PDF • TXT • MD",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    MorphingExpressiveButton(
      text = if (isImporting) "Импорт..." else "Добавить",
      icon = Icons.Rounded.Add,
      onClick = onImportClick,
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      modifier = Modifier.testTag("import_book_button"),
    )
  }

  AnimatedVisibility(visible = isImporting) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
      Text(
        text = "Открываем и разбираем книгу...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun HeroBookCard(
  book: BookEntity,
  onContinueClick: (Rect?) -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  var cardBounds by remember(book.id) { mutableStateOf<Rect?>(null) }

  val cornerRadius by
    animateDpAsState(
      targetValue = if (pressed) 22.dp else 34.dp,
      animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
      label = "heroCornerMorph",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.98f else 1f,
      animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
      label = "heroScale",
    )
  val percentInt = (book.progressPercent * 100f).roundToInt().coerceIn(0, 100)
  val c1 = MaterialTheme.colorScheme.primaryContainer
  val c2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
  val heroBrush = remember(c1, c2) { Brush.linearGradient(colors = listOf(c1, c2)) }

  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation = 4.dp,
    shadowElevation = 4.dp,
    modifier =
      Modifier.fillMaxWidth()
        .testTag("hero_book_card")
        .onGloballyPositioned { coords ->
          if (coords.isAttached) cardBounds = coords.boundsInRoot()
        }
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(RoundedCornerShape(cornerRadius))
        .clickable(
          interactionSource = interactionSource,
          indication = null,
        ) {
          haptics.tap()
          onContinueClick(cardBounds)
        },
  ) {
    Box(modifier = Modifier.fillMaxWidth().background(heroBrush).padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = HeroCardCoverShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shadowElevation = 6.dp,
          modifier =
            Modifier.width(106.dp)
              .height(150.dp)
              .graphicsLayer { rotationZ = -2.2f },
        ) {
          BookCoverArtwork(
            book = book,
            shape = HeroCardCoverShape,
            modifier = Modifier.fillMaxSize(),
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                )
                Text(
                  text = "СЕЙЧАС ЧИТАЮ",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            ) {
              Text(
                text = book.format.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          Text(
            text = book.title,
            fontFamily = PlayfairDisplayFamily,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExpressiveWavyProgressIndicator(
              progress = book.progressPercent.coerceAtLeast(0.04f),
              activeColor = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth(),
            )
            Text(
              text =
                if (percentInt == 0) "Глава 1 из ${book.totalChapters}"
                else "Прочитано $percentInt% • Глава ${book.currentChapterIndex + 1}/${book.totalChapters}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
          }

          MorphingExpressiveButton(
            text = if (percentInt == 0) "Начать чтение" else "Продолжить",
            icon = Icons.Rounded.PlayArrow,
            onClick = { onContinueClick(cardBounds) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            modifier = Modifier.testTag("hero_continue_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun ExpressiveSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  onActivate: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val viewConfiguration = LocalViewConfiguration.current
  val latestOnActivate by rememberUpdatedState(onActivate)
  var wasFocused by remember { mutableStateOf(false) }
  var lastActivateMs by remember { mutableStateOf(0L) }

  fun triggerActivateOnce() {
    val now = android.os.SystemClock.uptimeMillis()
    if (now - lastActivateMs > 180L) {
      lastActivateMs = now
      latestOnActivate()
    }
  }

  Surface(
    shape = SearchFieldShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier =
      Modifier.fillMaxWidth()
        .height(54.dp)
        .clip(SearchFieldShape)
        .pointerInput(viewConfiguration.touchSlop) {
          val touchSlop = viewConfiguration.touchSlop
          awaitPointerEventScope {
            while (true) {
              val downEvent = awaitPointerEvent(PointerEventPass.Initial)
              val down =
                downEvent.changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
              val downPos = down.position
              val pointerId = down.id
              var movedTooFar = false
              while (true) {
                val nextEvent = awaitPointerEvent(PointerEventPass.Initial)
                val change = nextEvent.changes.firstOrNull { it.id == pointerId } ?: break
                if ((change.position - downPos).getDistance() > touchSlop) {
                  movedTooFar = true
                }
                if (!change.pressed) {
                  if (!movedTooFar) {
                    // Re-trigger smooth scroll-to-top even if BasicTextField already kept focus after scrolling
                    triggerActivateOnce()
                  }
                  break
                }
              }
            }
          }
        }
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          focusRequester.requestFocus()
        }
        .testTag("library_search_field"),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = "Поиск",
        tint = MaterialTheme.colorScheme.primary,
      )
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = "Найти книгу или автора...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
          textStyle =
            MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface
            ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier =
            Modifier.fillMaxWidth()
              .focusRequester(focusRequester)
              .onFocusChanged { focusState ->
                if (focusState.isFocused && !wasFocused) {
                  wasFocused = true
                  triggerActivateOnce()
                } else if (!focusState.isFocused) {
                  wasFocused = false
                }
              },
        )
      }
      AnimatedVisibility(visible = query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = fadeOut()) {
        IconButton(
          onClick = {
            haptics.tick()
            onQueryChange("")
          }
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Очистить поиск",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ExpressiveBookGridCard(
  book: BookEntity,
  onOpen: (Rect?) -> Unit,
  onToggleFavorite: () -> Unit,
  onRequestDelete: (Rect?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalExpressiveHaptics.current
  var cardBounds by remember(book.id) { mutableStateOf<Rect?>(null) }
  var deleteBtnBounds by remember(book.id) { mutableStateOf<Rect?>(null) }

  Surface(
    shape = LibraryGridCardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("book_card_${book.id}")
        .onGloballyPositioned { coords ->
          if (coords.isAttached) cardBounds = coords.boundsInRoot()
        }
        .clip(LibraryGridCardShape)
        .clickable {
          haptics.tap()
          onOpen(cardBounds)
        },
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(modifier = Modifier.fillMaxWidth().height(186.dp)) {
        BookCoverArtwork(
          book = book,
          shape = LibraryCoverShape,
          modifier = Modifier.fillMaxSize(),
        )

        Box(
          modifier =
            Modifier.align(Alignment.TopStart)
              .padding(8.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
              .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = book.format.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        val favShape = if (book.isFavorite) BookFavScallop else CircleShape
        Box(
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(6.dp)
              .size(40.dp)
              .minimumInteractiveComponentSize()
              .clip(favShape)
              .background(
                if (book.isFavorite) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
              )
              .clickable(onClick = onToggleFavorite)
              .testTag("fav_button_${book.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              if (book.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "В избранное",
            tint =
              if (book.isFavorite) MaterialTheme.colorScheme.onSecondary
              else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val pct = (book.progressPercent * 100f).roundToInt().coerceIn(0, 100)
        Column(modifier = Modifier.weight(1f)) {
          ExpressiveWavyProgressIndicator(
            progress = book.progressPercent.coerceAtLeast(0.03f),
            waveAmplitude = 2.dp,
            strokeWidth = 3.dp,
            modifier = Modifier.fillMaxWidth(),
          )
          Text(
            text = if (pct > 0) "$pct%" else "Новая",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
              ) {
                onRequestDelete(deleteBtnBounds)
              }
              .testTag("delete_book_button_${book.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = TrashButtonShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 1.dp,
            modifier =
              Modifier.size(36.dp).onGloballyPositioned { coords ->
                if (coords.isAttached) deleteBtnBounds = coords.boundsInRoot()
              },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Удалить книгу",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyLibraryState(
  onImportClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(32.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(72.dp)
            .clip(EmptyCardScallop)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.MenuBook,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(34.dp),
        )
      }
      Text(
        text = "Книги не найдены",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Попробуйте изменить фильтр или добавьте файл EPUB, FB2, PDF или TXT с устройства.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      MorphingExpressiveButton(
        text = "Открыть файл",
        icon = Icons.Rounded.Add,
        onClick = onImportClick,
      )
    }
  }
}
