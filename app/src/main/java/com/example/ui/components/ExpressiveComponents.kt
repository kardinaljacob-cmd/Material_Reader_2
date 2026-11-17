package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BookEntity
import com.example.ui.theme.ExpressiveScallopShape
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.UnboundedFamily
import java.io.File
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sin

private val DefaultCoverCornerShape = RoundedCornerShape(20.dp)
private val CoverBadgeScallopShape = ExpressiveScallopShape(lobes = 8, scallopDepth = 0.14f)
private val FloatingBarShape = RoundedCornerShape(34.dp)

/**
 * Material 3 Expressive continuously flowing sinusoidal wavy progress indicator.
 * Reads animation state strictly inside Canvas DrawScope to avoid per-frame recompositions.
 */
@Composable
fun ExpressiveWavyProgressIndicator(
  progress: Float,
  modifier: Modifier = Modifier,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
  waveAmplitude: Dp = 3.dp,
  strokeWidth: Dp = 4.dp,
) {
  val clampedProgress = progress.coerceIn(0f, 1f)
  val animatedProgress =
    animateFloatAsState(
      targetValue = clampedProgress,
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "wavyProgress",
    )
  val infiniteTransition = rememberInfiniteTransition(label = "wavyInfinitePhase")
  val wavePhase =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "wavePhase",
    )
  val wavePath = remember { Path() }

  Canvas(modifier = modifier.height(14.dp)) {
    val cy = size.height / 2f
    val strokePx = strokeWidth.toPx()
    val ampPx = waveAmplitude.toPx()
    val activeEndX = size.width * animatedProgress.value
    val phase = wavePhase.value

    val trackStartX = (activeEndX + strokePx).coerceAtMost(size.width)
    if (trackStartX < size.width) {
      drawLine(
        color = trackColor,
        start = Offset(trackStartX, cy),
        end = Offset(size.width, cy),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
      )
    }

    if (activeEndX > 2f) {
      wavePath.reset()
      val wavelength = 36f
      val step = 5f
      val twoPi = 2f * PI.toFloat()
      var x = 0f
      while (x <= activeEndX) {
        val y = cy + ampPx * sin((x / wavelength) * twoPi - phase)
        if (x == 0f) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
        x += step
      }
      val endY = cy + ampPx * sin((activeEndX / wavelength) * twoPi - phase)
      wavePath.lineTo(activeEndX, endY)

      drawPath(
        path = wavePath,
        color = activeColor,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
    }
  }
}

/**
 * Filter chip that morphs corner radius and tactile press scale while updating palette colors
 * synchronously in a single frame (zero clipping and zero theme-transition flicker).
 */
@Composable
fun MorphingFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
) {
  val haptics = LocalExpressiveHaptics.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()

  val cornerRadius by
    animateDpAsState(
      targetValue = if (pressed) 10.dp else if (selected) 24.dp else 14.dp,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "chipCornerMorph",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.94f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "chipScale",
    )
  val bgColor =
    if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val fgColor =
    if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
  val chipShape = RoundedCornerShape(cornerRadius)

  Surface(
    shape = chipShape,
    color = bgColor,
    contentColor = fgColor,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(chipShape)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Tab,
        ) {
          haptics.tick()
          onClick()
        },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

/**
 * Oversized expressive button whose shape morphs smoothly on press and delivers tactile feedback.
 */
@Composable
fun MorphingExpressiveButton(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = MaterialTheme.colorScheme.onPrimary,
  contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
) {
  val haptics = LocalExpressiveHaptics.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()

  val cornerRadius by
    animateDpAsState(
      targetValue = if (pressed) 14.dp else 30.dp,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
      label = "btnMorphCorner",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.94f else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
      label = "btnMorphScale",
    )

  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = containerColor,
    contentColor = contentColor,
    shadowElevation = if (pressed) 2.dp else 6.dp,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(RoundedCornerShape(cornerRadius))
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
        ) {
          haptics.tap()
          onClick()
        },
  ) {
    Row(
      modifier = Modifier.padding(contentPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(10.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}

/**
 * Renders a book's cover from extracted image, bundled cover resource, or expressive typographic geometry.
 */
@Composable
fun BookCoverArtwork(
  book: BookEntity,
  modifier: Modifier = Modifier,
  shape: Shape = DefaultCoverCornerShape,
) {
  val bundledRes =
    when (book.bundledCoverKey) {
      "cosmos" -> R.drawable.img_cover_cosmos
      "lighthouse" -> R.drawable.img_cover_lighthouse
      "garden" -> R.drawable.img_cover_garden
      else -> null
    }

  Box(modifier = modifier.clip(shape)) {
    when {
      book.coverPath != null && File(book.coverPath).exists() -> {
        AsyncImage(
          model = File(book.coverPath),
          contentDescription = "Обложка книги ${book.title}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      bundledRes != null -> {
        AsyncImage(
          model = bundledRes,
          contentDescription = "Обложка книги ${book.title}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      else -> {
        val baseColor = Color(book.accentColorHex)
        val secondaryColor = MaterialTheme.colorScheme.tertiary
        val coverBrush =
          remember(baseColor, secondaryColor) {
            Brush.linearGradient(
              colors = listOf(baseColor, baseColor.copy(alpha = 0.78f), secondaryColor)
            )
          }
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(coverBrush)
              .padding(12.dp)
        ) {
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CoverBadgeScallopShape)
                .background(Color.White.copy(alpha = 0.22f))
          )
          Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
              text = book.format.label,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = book.title,
              fontFamily = UnboundedFamily,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

enum class MainNavTab(
  val route: String,
  val label: String,
  val testTag: String,
) {
  LIBRARY("library", "Полка", "nav_tab_library"),
  APPEARANCE("appearance", "Стиль", "nav_tab_appearance"),
}

/**
 * Floating 2-tab bottom navigation bar ("Полка" & "Стиль") in M3 Expressive style:
 * only the active tab expands to show its label while the inactive tab smoothly compresses
 * to an icon-only capsule, driven 1:1 by [selectionProgress] (0f..1f) without layout jumps.
 */
@Composable
fun ExpressiveFloatingNavBar(
  selectedTab: MainNavTab,
  selectionProgress: () -> Float,
  icons: Map<MainNavTab, Pair<ImageVector, ImageVector>>,
  onSelectTab: (MainNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalExpressiveHaptics.current

  Box(
    modifier =
      modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = FloatingBarShape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
    ) {
      // Use a generous bounded ripple radius so the touch wave spans the full expanded pill width
      // even when press starts in the narrow collapsed state.
      val tabRipple = ripple(bounded = true, radius = 96.dp)
      val tabCount = MainNavTab.entries.size

      Row(
        modifier = Modifier.wrapContentWidth().padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val progress = selectionProgress().coerceIn(0f, (tabCount - 1).toFloat())
        MainNavTab.entries.forEachIndexed { idx, tab ->
          val activeFraction = (1f - (progress - idx).absoluteValue).coerceIn(0f, 1f)
          val isSelected = tab == selectedTab
          val (filledIcon, outlinedIcon) = icons.getValue(tab)
          val pillBg =
            lerp(
              Color.Transparent,
              MaterialTheme.colorScheme.primaryContainer,
              activeFraction,
            )
          val contentColor =
            lerp(
              MaterialTheme.colorScheme.onSurfaceVariant,
              MaterialTheme.colorScheme.onPrimaryContainer,
              activeFraction,
            )
          val iconScale = 0.96f + 0.08f * activeFraction
          val pillCorner = (22f + 6f * activeFraction).dp
          val horizontalPad = (14f + 6f * activeFraction).dp
          val interactionSource = remember(tab) { MutableInteractionSource() }

          Box(
            modifier =
              Modifier.height(50.dp)
                .widthIn(min = 52.dp)
                .testTag(tab.testTag)
                .clip(RoundedCornerShape(pillCorner))
                .background(pillBg)
                .clickable(
                  interactionSource = interactionSource,
                  indication = tabRipple,
                  role = Role.Tab,
                ) {
                  haptics.morph()
                  onSelectTab(tab)
                }
                .padding(horizontal = horizontalPad),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (activeFraction >= 0.5f) filledIcon else outlinedIcon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier =
                  Modifier.size(22.dp).graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                  },
              )
              Box(
                modifier =
                  Modifier.clipToBounds()
                    .layout { measurable, constraints ->
                      val placeable =
                        measurable.measure(constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE))
                      val targetW = (placeable.width * activeFraction).roundToInt()
                      layout(targetW, placeable.height) {
                        placeable.placeRelative(0, 0)
                      }
                    }
                    .graphicsLayer {
                      alpha = ((activeFraction - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    },
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Spacer(Modifier.width(8.dp))
                  Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ExpressiveNavRail(
  selectedTab: MainNavTab,
  icons: Map<MainNavTab, Pair<ImageVector, ImageVector>>,
  onSelectTab: (MainNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalExpressiveHaptics.current
  NavigationRail(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Spacer(Modifier.height(16.dp))
    MainNavTab.entries.forEach { tab ->
      val isSelected = tab == selectedTab
      val (filledIcon, outlinedIcon) = icons.getValue(tab)
      NavigationRailItem(
        selected = isSelected,
        onClick = {
          haptics.morph()
          onSelectTab(tab)
        },
        icon = {
          Icon(
            imageVector = if (isSelected) filledIcon else outlinedIcon,
            contentDescription = tab.label,
          )
        },
        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.testTag(tab.testTag),
      )
    }
  }
}
