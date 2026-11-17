package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.util.lerp
import com.example.data.model.BookEntity
import com.example.ui.theme.ExpressiveScallopShape
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.PlayfairDisplayFamily
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class BookLaunchOrigin(
  val book: BookEntity,
  val sourceRect: Rect? = null,
  val cornerRadiusDp: Dp = 26.dp,
  val isHero: Boolean = false,
)

data class BookDeleteRequest(
  val book: BookEntity,
  val sourceRect: Rect? = null,
)

private val DeleteBadgeShape = ExpressiveScallopShape(lobes = 8, scallopDepth = 0.14f)
private val DeleteDialogShape = RoundedCornerShape(32.dp)

/**
 * Physically unfolds the book deletion confirmation dialog directly from the tapped trash icon
 * button on a book card, with filled expressive pill backgrounds on both action buttons.
 */
@Composable
fun ExpressiveDeleteBookOverlay(
  request: BookDeleteRequest,
  onConfirmDelete: (BookEntity) -> Unit,
  onDismissComplete: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val unfoldAnim = remember(request) { Animatable(0f) }
  var isDismissing by remember(request) { mutableStateOf(false) }

  fun closeAnimated(afterClose: (() -> Unit)? = null) {
    if (isDismissing) return
    isDismissing = true
    scope.launch {
      unfoldAnim.animateTo(
        targetValue = 0f,
        animationSpec =
          spring(
            dampingRatio = 0.92f,
            stiffness = 440f,
            visibilityThreshold = 0.002f,
          ),
      )
      afterClose?.invoke()
      onDismissComplete()
    }
  }

  BackHandler {
    haptics.tick()
    closeAnimated()
  }

  LaunchedEffect(request) {
    unfoldAnim.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val rootWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
    val rootHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
    val targetWidthDp = minOf(maxWidth - 48.dp, 360.dp).coerceAtLeast(240.dp)
    val targetWidthPx = with(density) { targetWidthDp.toPx() }
    val targetHeightPx = with(density) { 216.dp.toPx() }
    val targetLeft = (rootWidthPx - targetWidthPx) / 2f
    val targetTop = (rootHeightPx - targetHeightPx) / 2f

    val fallbackButtonPx = with(density) { 36.dp.toPx() }
    val sourceRect =
      request.sourceRect
        ?: Rect(
          left = (rootWidthPx - fallbackButtonPx) / 2f,
          top = (rootHeightPx - fallbackButtonPx) / 2f,
          right = (rootWidthPx + fallbackButtonPx) / 2f,
          bottom = (rootHeightPx + fallbackButtonPx) / 2f,
        )

    val p = unfoldAnim.value.coerceIn(0f, 1f)
    val currentCornerDp = lerpDp(10.dp, 32.dp, p)
    val currentTonalDp = lerpDp(1.dp, 8.dp, p)
    val currentShadowDp = lerpDp(0.dp, 16.dp, ((p - 0.14f) / 0.86f).coerceIn(0f, 1f))
    val currentContainerColor =
      lerpColor(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        p,
      )

    // Scrim
    Box(
      modifier =
        Modifier.fillMaxSize()
          .drawBehind {
            val scrimAlpha = 0.48f * p
            drawRect(Color.Black.copy(alpha = scrimAlpha))
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) {
            haptics.tick()
            closeAnimated()
          }
    )

    // Solid container unfolding directly from the filled rounded-square trash button to centered dialog
    Surface(
      shape = RoundedCornerShape(currentCornerDp),
      color = currentContainerColor,
      tonalElevation = currentTonalDp,
      shadowElevation = currentShadowDp,
      modifier =
        Modifier.layout { measurable, constraints ->
            val curL = lerp(sourceRect.left, targetLeft, p)
            val curT = lerp(sourceRect.top, targetTop, p)
            val curR = lerp(sourceRect.right, targetLeft + targetWidthPx, p)
            val curB = lerp(sourceRect.bottom, targetTop + targetHeightPx, p)
            val w = (curR - curL).roundToInt().coerceIn(1, constraints.maxWidth)
            val h = (curB - curT).roundToInt().coerceIn(1, constraints.maxHeight)
            val placeable = measurable.measure(Constraints.fixed(w, h))
            layout(constraints.maxWidth, constraints.maxHeight) {
              placeable.placeRelative(curL.roundToInt(), curT.roundToInt())
            }
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) {},
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Button trash icon visible at start/end of physical morph so collapse feels 100% seamless
        val iconAlpha = (1f - (p / 0.24f)).coerceIn(0f, 1f)
        if (iconAlpha > 0.01f) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp).graphicsLayer { alpha = iconAlpha },
          )
        }

        // Fixed-width inner dialog content fading smoothly inside the expanding container
        val contentAlpha = ((p - 0.22f) / 0.46f).coerceIn(0f, 1f)
        if (contentAlpha > 0.01f) {
          Column(
            modifier =
              Modifier.requiredSize(width = targetWidthDp, height = 216.dp)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .graphicsLayer { alpha = contentAlpha },
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(DeleteBadgeShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(22.dp),
                )
              }
              Text(
                text = "Удалить книгу?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Text(
              text = "«${request.book.title}» будет удалена с полки вместе с закладками и заметками.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              FilledTonalButton(
                onClick = {
                  haptics.tick()
                  closeAnimated()
                },
                shape = CircleShape,
                colors =
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  ),
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("delete_cancel_button"),
              ) {
                Text("Отмена", fontWeight = FontWeight.SemiBold, maxLines = 1)
              }

              Button(
                onClick = {
                  haptics.confirm()
                  closeAnimated { onConfirmDelete(request.book) }
                },
                shape = CircleShape,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                  ),
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("delete_confirm_button"),
              ) {
                Text("Удалить", fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Morphs any clicked book card (grid card or "Сейчас читаю" Hero card) seamlessly from its exact
 * screen bounds into full-screen reader mode without layout re-pagination, jumps, or flicker.
 */
@Composable
fun BookReaderMorphContainer(
  launchOrigin: BookLaunchOrigin,
  morphProgress: Float,
  isClosing: Boolean,
  closingVeilAlpha: Float = 1f,
  readerBgColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable (isMorphSettled: Boolean) -> Unit,
) {
  val density = LocalDensity.current
  val gridStartColor = MaterialTheme.colorScheme.surfaceContainerLow
  val heroC1 = MaterialTheme.colorScheme.primaryContainer
  val heroC2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
  val heroBrush = remember(heroC1, heroC2) { Brush.linearGradient(colors = listOf(heroC1, heroC2)) }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val rootWidthDp = maxWidth
    val rootHeightDp = maxHeight
    val rootWidthPx = with(density) { rootWidthDp.toPx() }.coerceAtLeast(1f)
    val rootHeightPx = with(density) { rootHeightDp.toPx() }.coerceAtLeast(1f)

    val fallbackRect =
      remember(rootWidthPx, rootHeightPx) {
        val w = rootWidthPx * 0.45f
        val h = rootHeightPx * 0.30f
        Rect(
          left = (rootWidthPx - w) / 2f,
          top = (rootHeightPx - h) / 2f,
          right = (rootWidthPx + w) / 2f,
          bottom = (rootHeightPx + h) / 2f,
        )
      }
    val source = launchOrigin.sourceRect ?: fallbackRect
    val sourceW = source.width.coerceAtLeast(1f)
    val sourceH = source.height.coerceAtLeast(1f)

    val p = morphProgress.coerceIn(0f, 1f)
    val isSettled = p >= 0.995f
    val cornerDp = lerpDp(launchOrigin.cornerRadiusDp, 0.dp, p)
    val startBgSolid = if (launchOrigin.isHero) heroC1 else gridStartColor
    val containerColor =
      lerpColor(startBgSolid, readerBgColor, ((p - 0.08f) / 0.52f).coerceIn(0f, 1f))

    // During physical collapse (p: 1f -> 0f), stay 100% opaque so the card physically shrinks
    // all the way back into sourceRect (never dissolving in mid-air). Only once it lands at
    // p == 0f does closingVeilAlpha smoothly reveal the real shelf card underneath.
    val shellAlpha = if (isClosing) closingVeilAlpha.coerceIn(0f, 1f) else 1f

    // Dim backdrop beneath the morphing card and block touches underneath
    Box(
      modifier =
        Modifier.fillMaxSize()
          .drawBehind { drawRect(Color.Black.copy(alpha = 0.32f * p * shellAlpha)) }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) {}
    )

    // Expanding/collapsing container positioned directly from card's window Rect to full-screen
    Box(
      modifier =
        Modifier.layout { measurable, constraints ->
            val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val curL = lerp(source.left, 0f, p)
            val curT = lerp(source.top, 0f, p)
            val curR = lerp(source.right, maxW, p)
            val curB = lerp(source.bottom, maxH, p)
            val w = (curR - curL).roundToInt().coerceIn(1, constraints.maxWidth)
            val h = (curB - curT).roundToInt().coerceIn(1, constraints.maxHeight)
            val placeable = measurable.measure(Constraints.fixed(w, h))
            layout(constraints.maxWidth, constraints.maxHeight) {
              placeable.placeRelative(curL.roundToInt(), curT.roundToInt())
            }
          }
          .graphicsLayer { alpha = shellAlpha }
          .then(if (!isSettled) Modifier.clip(RoundedCornerShape(cornerDp)) else Modifier)
          .background(containerColor),
      contentAlignment = Alignment.Center,
    ) {
      // Exact Hero gradient layer so collapsing/expanding into HeroBookCard matches its background 1:1
      if (launchOrigin.isHero) {
        val heroBgAlpha = (1f - ((p - 0.06f) / 0.45f)).coerceIn(0f, 1f)
        if (heroBgAlpha > 0.005f) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer { alpha = heroBgAlpha }
                .background(heroBrush)
          )
        }
      }

      // Layer 1: Full-screen ReaderScreen measured ONCE at exact full screen dimensions so
      // TextPaginator never re-runs mid-morph.
      val readerAlpha =
        if (isClosing) ((p - 0.50f) / 0.42f).coerceIn(0f, 1f)
        else ((p - 0.16f) / 0.44f).coerceIn(0f, 1f)
      val readerScale = lerp(0.90f, 1f, p)
      Box(
        modifier =
          Modifier.requiredSize(rootWidthDp, rootHeightDp).graphicsLayer {
            alpha = readerAlpha
            scaleX = readerScale
            scaleY = readerScale
          }
      ) {
        content(p >= 0.85f && !isClosing)
      }

      // Layer 2: Card preview shown during opening expansion; during collapse, render soft
      // object silhouettes matching the destination card layout before gently cross-dissolving
      // into the real card content once settled at sourceRect.
      if (!isClosing) {
        val cardAlpha = (1f - ((p - 0.04f) / 0.30f)).coerceIn(0f, 1f)
        if (cardAlpha > 0.005f) {
          val cardWidthDp = with(density) { sourceW.toDp() }
          val cardHeightDp = with(density) { sourceH.toDp() }
          val curW = lerp(sourceW, rootWidthPx, p)
          val curH = lerp(sourceH, rootHeightPx, p)
          val fitScale = minOf(curW / sourceW, curH / sourceH).coerceAtLeast(1f)
          Box(
            modifier =
              Modifier.requiredSize(cardWidthDp, cardHeightDp).graphicsLayer {
                alpha = cardAlpha
                scaleX = fitScale
                scaleY = fitScale
              }
          ) {
            if (launchOrigin.isHero) {
              HeroCardMorphPreview(book = launchOrigin.book)
            } else {
              GridCardMorphPreview(book = launchOrigin.book)
            }
          }
        }
      } else {
        val silhouetteAlpha = ((0.64f - p) / 0.46f).coerceIn(0f, 1f)
        if (silhouetteAlpha > 0.005f) {
          val cardWidthDp = with(density) { sourceW.toDp() }
          val cardHeightDp = with(density) { sourceH.toDp() }
          val curW = lerp(sourceW, rootWidthPx, p)
          val curH = lerp(sourceH, rootHeightPx, p)
          val fitScale = minOf(curW / sourceW, curH / sourceH).coerceAtLeast(1f)
          Box(
            modifier =
              Modifier.requiredSize(cardWidthDp, cardHeightDp).graphicsLayer {
                alpha = silhouetteAlpha
                scaleX = fitScale
                scaleY = fitScale
              }
          ) {
            if (launchOrigin.isHero) {
              HeroCardCollapseSilhouettes()
            } else {
              GridCardCollapseSilhouettes()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeroCardMorphPreview(book: BookEntity) {
  val c1 = MaterialTheme.colorScheme.primaryContainer
  val c2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
  val brush = remember(c1, c2) { Brush.linearGradient(colors = listOf(c1, c2)) }
  Box(modifier = Modifier.fillMaxSize().background(brush).padding(20.dp)) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCoverArtwork(
        book = book,
        shape = RoundedCornerShape(22.dp),
        modifier =
          Modifier.width(106.dp).height(150.dp).graphicsLayer { rotationZ = -2.2f },
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
            Text("СЕЙЧАС ЧИТАЮ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(48.dp))
      }
    }
  }
}

@Composable
private fun GridCardMorphPreview(book: BookEntity) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    BookCoverArtwork(
      book = book,
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.fillMaxWidth().height(186.dp),
    )
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
    Spacer(Modifier.weight(1f))
  }
}

@Composable
private fun HeroCardCollapseSilhouettes() {
  val tintStrong = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
  val tintSoft = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.09f)
  Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.width(106.dp)
            .height(150.dp)
            .graphicsLayer { rotationZ = -2.2f }
            .clip(RoundedCornerShape(22.dp))
            .background(tintStrong)
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(modifier = Modifier.width(98.dp).height(20.dp).clip(CircleShape).background(tintStrong))
          Box(modifier = Modifier.width(42.dp).height(20.dp).clip(CircleShape).background(tintSoft))
        }
        Box(modifier = Modifier.fillMaxWidth(0.90f).height(22.dp).clip(CircleShape).background(tintStrong))
        Box(modifier = Modifier.fillMaxWidth(0.56f).height(14.dp).clip(CircleShape).background(tintSoft))
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(tintSoft))
        Box(modifier = Modifier.width(134.dp).height(40.dp).clip(CircleShape).background(tintStrong))
      }
    }
  }
}

@Composable
private fun GridCardCollapseSilhouettes() {
  val coverTint = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
  val textTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
  val subTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
  Column(
    modifier = Modifier.fillMaxSize().padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(186.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(coverTint)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Box(modifier = Modifier.fillMaxWidth(0.84f).height(15.dp).clip(CircleShape).background(textTint))
      Box(modifier = Modifier.fillMaxWidth(0.52f).height(11.dp).clip(CircleShape).background(subTint))
    }
    Spacer(Modifier.weight(1f))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(textTint))
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(coverTint)
      )
    }
  }
}
