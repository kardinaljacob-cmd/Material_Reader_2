package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageFlipAnimation
import com.example.ui.components.MorphingFilterChip
import com.example.ui.theme.DarkModeSetting
import com.example.ui.theme.ExpressivePalette
import com.example.ui.theme.ExpressiveScallopShape
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderTheme
import com.example.ui.theme.UnboundedFamily
import com.example.ui.viewmodel.AppearanceUiState

private val SectionCardShape = RoundedCornerShape(32.dp)
private val FontSelectedShape = RoundedCornerShape(24.dp)
private val FontDefaultShape = RoundedCornerShape(16.dp)
private val StatCardShape = RoundedCornerShape(26.dp)
private val SelectedPaletteScallop = ExpressiveScallopShape(lobes = 8, scallopDepth = 0.14f)
private val DefaultPaletteScallop = ExpressiveScallopShape(lobes = 6, scallopDepth = 0.06f)

@Composable
fun AppearanceScreen(
  uiState: AppearanceUiState,
  onPaletteSelected: (ExpressivePalette) -> Unit,
  onDarkModeSelected: (DarkModeSetting) -> Unit,
  onReaderThemeSelected: (ReaderTheme) -> Unit,
  onFontSelected: (ReaderFontOption) -> Unit,
  onPageAnimSelected: (PageFlipAnimation) -> Unit,
  onHapticsToggle: (Boolean) -> Unit,
  onKeepScreenOnToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = uiState.preferences
  val haptics = LocalExpressiveHaptics.current

  val topTint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
  val bgColor = MaterialTheme.colorScheme.background
  val bottomTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
  val bgBrush =
    remember(topTint, bgColor, bottomTint) {
      Brush.verticalGradient(colors = listOf(topTint, bgColor, bottomTint))
    }

  BoxWithConstraints(modifier = modifier.fillMaxSize().background(bgBrush)) {
    val isExpanded = maxWidth >= 720.dp

    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .align(Alignment.TopCenter)
          .widthIn(max = if (isExpanded) 960.dp else 640.dp),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 112.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      item(key = "header", contentType = "header") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Стиль и цвета",
            fontFamily = UnboundedFamily,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Material 3 Expressive • Адаптация под обои",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      item(key = "stats", contentType = "stats") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          BentoStatCard(
            value = "${uiState.totalBooks}",
            label = "Книг",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
          )
          BentoStatCard(
            value = "${uiState.totalBookmarks}",
            label = "Закладок",
            icon = Icons.Rounded.Bookmark,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
          )
          BentoStatCard(
            value = "${uiState.favoriteBooks}",
            label = "Избранных",
            icon = Icons.Rounded.Favorite,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // 1. Dynamic Wallpaper & Expressive Palette Selector with lush morph animations
      item(key = "palette", contentType = "section_card") {
        Surface(
          shape = SectionCardShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Палитра интерфейса",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              ExpressivePalette.entries.forEach { palette ->
                val selected = palette == prefs.appPalette
                val corner by
                  animateDpAsState(
                    targetValue = if (selected) 28.dp else 18.dp,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "paletteCorner",
                  )
                val bg =
                  if (selected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                val scallopRotation by
                  animateFloatAsState(
                    targetValue = if (selected) 22f else 0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                    label = "scallopRot",
                  )

                Surface(
                  shape = RoundedCornerShape(corner),
                  color = bg,
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(corner))
                      .clickable {
                        haptics.morph()
                        onPaletteSelected(palette)
                      }
                      .testTag("palette_${palette.name.lowercase()}"),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                  ) {
                    val previewBrush =
                      remember(palette) {
                        Brush.linearGradient(
                          colors =
                            listOf(
                              palette.previewPrimary,
                              palette.previewSecondary,
                              palette.previewTertiary,
                            )
                        )
                      }
                    Box(
                      modifier =
                        Modifier.size(48.dp)
                          .graphicsLayer { rotationZ = scallopRotation }
                          .clip(if (selected) SelectedPaletteScallop else DefaultPaletteScallop)
                          .background(previewBrush),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (selected) {
                        Icon(
                          imageVector = Icons.Rounded.Check,
                          contentDescription = null,
                          tint = Color.White,
                        )
                      }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = palette.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = palette.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }

            Text(
              text = "Режим яркости",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              DarkModeSetting.entries.forEach { mode ->
                MorphingFilterChip(
                  label = mode.title,
                  selected = mode == prefs.darkModeSetting,
                  onClick = { onDarkModeSelected(mode) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // 2. Expressive & Book Fonts Showcase (LazyRow + uniform fixed card height)
      item(key = "fonts", contentType = "fonts_section") {
        Surface(
          shape = SectionCardShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 20.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Шрифты для чтения (${ReaderFontOption.entries.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Активный: ${prefs.fontOption.title} (${prefs.fontOption.badge})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            LazyRow(
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 20.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(
                items = ReaderFontOption.entries,
                key = { it.name },
                contentType = { "font_card" },
              ) { option ->
                val selected = option == prefs.fontOption
                val shape = if (selected) FontSelectedShape else FontDefaultShape
                val cardBg =
                  if (selected) MaterialTheme.colorScheme.secondaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                val borderModifier =
                  if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, shape)
                  else Modifier

                Column(
                  modifier =
                    Modifier.width(132.dp)
                      .height(106.dp)
                      .clip(shape)
                      .background(cardBg)
                      .then(borderModifier)
                      .clickable {
                        haptics.morph()
                        onFontSelected(option)
                      }
                      .padding(12.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "АбВ",
                    style =
                      TextStyle(
                        fontFamily = option.fontFamily,
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                      ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = option.title,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = option.badge,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 3. Page Turn Animation & Paper Theme Quick Controls
      item(key = "paper_and_motion", contentType = "section_card") {
        Surface(
          shape = SectionCardShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Перелистывание и бумага",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 2.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PageFlipAnimation.entries.forEach { anim ->
                MorphingFilterChip(
                  label = anim.title,
                  selected = anim == prefs.pageAnimation,
                  onClick = { onPageAnimSelected(anim) },
                )
              }
            }
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 2.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ReaderTheme.entries.forEach { theme ->
                MorphingFilterChip(
                  label = theme.title,
                  selected = theme == prefs.readerTheme,
                  onClick = { onReaderThemeSelected(theme) },
                )
              }
            }
          }
        }
      }

      // 4. Haptic Feedback card (Switch only, crisp feedback on toggle) & Screen Settings
      item(key = "haptics_card", contentType = "section_card") {
        Surface(
          shape = SectionCardShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth().testTag("haptics_section_card"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.Vibration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text(
                  text = "Тактильный отклик",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Вибрация при нажатиях и перелистывании",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = prefs.hapticsEnabled,
              onCheckedChange = { enabled ->
                onHapticsToggle(enabled)
                if (enabled) {
                  haptics.enabled = true
                  haptics.confirm()
                }
              },
              modifier = Modifier.testTag("haptics_switch"),
            )
          }
        }
      }

      item(key = "screen_keep_on_card", contentType = "section_card") {
        Surface(
          shape = SectionCardShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Column {
                Text(
                  text = "Не выключать экран",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Экран остаётся активным во время чтения",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = prefs.keepScreenOn,
              onCheckedChange = {
                haptics.morph()
                onKeepScreenOnToggle(it)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BentoStatCard(
  value: String,
  label: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalExpressiveHaptics.current
  var popped by remember { mutableIntStateOf(0) }
  val scale by
    animateFloatAsState(
      targetValue = if (popped % 2 == 1) 0.94f else 1f,
      animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
      label = "bentoPop",
    )

  Surface(
    shape = StatCardShape,
    color = containerColor,
    contentColor = contentColor,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(StatCardShape)
        .clickable {
          popped++
          haptics.tick()
        },
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Text(
        text = value,
        fontFamily = UnboundedFamily,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}
