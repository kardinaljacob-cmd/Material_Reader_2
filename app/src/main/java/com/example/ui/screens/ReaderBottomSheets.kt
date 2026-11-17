package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookChapter
import com.example.data.model.BookmarkEntity
import com.example.data.model.NoteEntity
import com.example.data.model.PageFlipAnimation
import com.example.data.prefs.ReaderPreferences
import com.example.ui.components.MorphingFilterChip
import com.example.ui.theme.ExpressiveScallopShape
import com.example.ui.theme.LocalExpressiveHaptics
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderTheme
import java.util.Locale
import kotlin.math.roundToInt

private val SheetTopShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
private val SheetFontSelectedShape = RoundedCornerShape(24.dp)
private val SheetFontDefaultShape = RoundedCornerShape(16.dp)
private val ThemeSelectedScallop = ExpressiveScallopShape(lobes = 8, scallopDepth = 0.1f)
private val ThemeDefaultShape = RoundedCornerShape(22.dp)

private fun <S> AnimatedContentTransitionScope<S>.verticalSectionTransitionSpec(
  forward: Boolean,
): ContentTransform {
  val easing = FastOutSlowInEasing
  val enterOffset: (Int) -> Int = { fullHeight -> if (forward) fullHeight else -fullHeight }
  val exitOffset: (Int) -> Int = { fullHeight -> if (forward) -fullHeight else fullHeight }

  return (slideInVertically(
    animationSpec = tween(durationMillis = 360, easing = easing),
    initialOffsetY = enterOffset,
  ) +
    fadeIn(
      animationSpec = tween(durationMillis = 250, delayMillis = 60, easing = easing),
    ))
    .togetherWith(
      slideOutVertically(
        animationSpec = tween(durationMillis = 300, easing = easing),
        targetOffsetY = exitOffset,
      ) +
        fadeOut(
          animationSpec = tween(durationMillis = 200, easing = easing),
        )
    )
    .using(
      SizeTransform(
        clip = true,
        sizeAnimationSpec = { _, _ ->
          spring(
            dampingRatio = 0.88f,
            stiffness = 380f,
          )
        },
      )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFontSheet(
  prefs: ReaderPreferences,
  onSelectFont: (ReaderFontOption) -> Unit,
  onFontSizeChange: (Float) -> Unit,
  onLineHeightChange: (Float) -> Unit,
  onLetterSpacingChange: (Float) -> Unit,
  onWordSpacingChange: (Float) -> Unit,
  onParagraphSpacingChange: (Float) -> Unit,
  onFirstLineIndentChange: (Float) -> Unit,
  onPaddingChange: (Float) -> Unit,
  onJustifyChange: (Boolean) -> Unit,
  onResetTypography: () -> Unit,
  onDismiss: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedSection by remember { mutableIntStateOf(0) } // 0 = Текст, 1 = Расширенные настройки

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = SheetTopShape,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxSectionHeight = (maxHeight - 150.dp).coerceIn(260.dp, 640.dp)

      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Шрифты и типографика",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              SectionTabPill(
                title = "Текст и шрифты",
                selected = selectedSection == 0,
                onClick = {
                  haptics.tick()
                  selectedSection = 0
                },
                modifier = Modifier.weight(1f).testTag("tab_basic_typography"),
              )
              SectionTabPill(
                title = "Расширенные настройки",
                selected = selectedSection == 1,
                onClick = {
                  haptics.tick()
                  selectedSection = 1
                },
                modifier = Modifier.weight(1f).testTag("tab_advanced_typography"),
              )
            }
          }
        }

        AnimatedContent(
          targetState = selectedSection,
          transitionSpec = { verticalSectionTransitionSpec(forward = targetState > initialState) },
          label = "fontSectionVerticalMorph",
          modifier = Modifier.fillMaxWidth(),
        ) { section ->
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = maxSectionHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (section == 0) {
        // Uniform fixed-height font cards so scrolling never shifts the UI below
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Гарнитура (${ReaderFontOption.entries.size} шрифтов)",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Листайте →",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = ((maxWidth - 40.dp - 20.dp) / 2.28f).coerceIn(124.dp, 160.dp)

            LazyRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
            ) {
              items(
                items = ReaderFontOption.entries,
                key = { it.name },
                contentType = { "font_card" },
              ) { option ->
                val selected = option == prefs.fontOption
                val shape = if (selected) SheetFontSelectedShape else SheetFontDefaultShape
                val bgColor =
                  if (selected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                val borderModifier =
                  if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                  else Modifier

                Column(
                  modifier =
                    Modifier.width(cardWidth)
                      .height(124.dp)
                      .testTag("font_option_${option.name.lowercase()}")
                      .clip(shape)
                      .background(bgColor)
                      .then(borderModifier)
                      .clickable {
                        haptics.morph()
                        onSelectFont(option)
                      }
                      .padding(12.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier =
                        Modifier.clip(CircleShape)
                          .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                          )
                          .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                      Text(
                        text = option.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    if (selected) {
                      Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Выбран",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }

                  Text(
                    text = "Аа Бб",
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
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = option.previewPhrase,
                      style =
                        MaterialTheme.typography.bodySmall.copy(
                          fontFamily = option.fontFamily,
                          lineHeight = 14.sp,
                          platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Размер шрифта",
                  style = MaterialTheme.typography.titleMedium,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  StepperCircleButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Уменьшить шрифт",
                    onClick = {
                      haptics.tick()
                      onFontSizeChange((prefs.fontSizeSp - 1f).coerceAtLeast(14f))
                    },
                  )
                  Text(
                    text = "${prefs.fontSizeSp.roundToInt()} pt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  StepperCircleButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Увеличить шрифт",
                    onClick = {
                      haptics.tick()
                      onFontSizeChange((prefs.fontSizeSp + 1f).coerceAtMost(32f))
                    },
                  )
                }
              }
              Slider(
                value = prefs.fontSizeSp,
                onValueChange = {
                  if (it.roundToInt() != prefs.fontSizeSp.roundToInt()) haptics.tick()
                  onFontSizeChange(it)
                },
                valueRange = 14f..32f,
              )

              FineTunedSliderRow(
                label = "Межстрочное расстояние",
                valueText = String.format(Locale.US, "%.2f×", prefs.lineHeightMultiplier),
                value = prefs.lineHeightMultiplier,
                range = 1.05f..2.3f,
                step = 0.05f,
                onValueChange = onLineHeightChange,
              )

              FineTunedSliderRow(
                label = "Межобзацевое расстояние",
                valueText = "${prefs.paragraphSpacingDp.roundToInt()} dp",
                value = prefs.paragraphSpacingDp,
                range = 0f..28f,
                step = 2f,
                onValueChange = onParagraphSpacingChange,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                  haptics.morph()
                  onJustifyChange(!prefs.textAlignJustify)
                },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector =
                    if (prefs.textAlignJustify) Icons.Rounded.FormatAlignJustify
                    else Icons.AutoMirrored.Rounded.FormatAlignLeft,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text("Выравнивание по ширине", style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = if (prefs.textAlignJustify) "Книжный блок" else "По левому краю",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = prefs.textAlignJustify,
                onCheckedChange = {
                  haptics.morph()
                  onJustifyChange(it)
                },
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable {
                  haptics.morph()
                  selectedSection = 1
                },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column {
                  Text(
                    text = "Расширенные настройки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Text(
                    text = "Межстрочные, межбуквенные, межсловесные и абзацы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                  )
                }
              }
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          LiveTypographyPreviewCard(prefs = prefs)

          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Точная типографика",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                TextButton(
                  onClick = {
                    haptics.confirm()
                    onResetTypography()
                  },
                  modifier = Modifier.testTag("reset_typography_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text("Сбросить")
                }
              }

              FineTunedSliderRow(
                label = "Межстрочное расстояние",
                valueText = String.format(Locale.US, "%.2f×", prefs.lineHeightMultiplier),
                value = prefs.lineHeightMultiplier,
                range = 1.05f..2.30f,
                step = 0.05f,
                onValueChange = onLineHeightChange,
                testTag = "slider_line_height",
              )

              FineTunedSliderRow(
                label = "Межбуквенное расстояние",
                valueText = String.format(Locale.US, "%+.2f sp", prefs.letterSpacingSp),
                value = prefs.letterSpacingSp,
                range = -0.50f..3.00f,
                step = 0.10f,
                onValueChange = onLetterSpacingChange,
                testTag = "slider_letter_spacing",
              )

              FineTunedSliderRow(
                label = "Межсловесное расстояние",
                valueText = String.format(Locale.US, "+%.1f sp", prefs.wordSpacingSp),
                value = prefs.wordSpacingSp,
                range = 0.0f..8.0f,
                step = 0.5f,
                onValueChange = onWordSpacingChange,
                testTag = "slider_word_spacing",
              )

              FineTunedSliderRow(
                label = "Межобзацевое расстояние",
                valueText = "${prefs.paragraphSpacingDp.roundToInt()} dp",
                value = prefs.paragraphSpacingDp,
                range = 0f..28f,
                step = 2f,
                onValueChange = onParagraphSpacingChange,
                testTag = "slider_paragraph_spacing",
              )

              FineTunedSliderRow(
                label = "Красная строка (отступ)",
                valueText = "${prefs.firstLineIndentSp.roundToInt()} sp",
                value = prefs.firstLineIndentSp,
                range = 0f..36f,
                step = 2f,
                onValueChange = onFirstLineIndentChange,
                testTag = "slider_first_line_indent",
              )

              FineTunedSliderRow(
                label = "Боковые поля страницы",
                valueText = "${prefs.horizontalPaddingDp.roundToInt()} dp",
                value = prefs.horizontalPaddingDp,
                range = 8f..44f,
                step = 2f,
                onValueChange = onPaddingChange,
                testTag = "slider_horizontal_padding",
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
}

@Composable
private fun LiveTypographyPreviewCard(prefs: ReaderPreferences) {
  val sample1 = "Строки дышат свободно, когда каждый интервал настроен точно под ваш взгляд."
  val sample2 = "Второй абзац показывает отступ красной строки и воздух между мыслями."

  val align = if (prefs.textAlignJustify) TextAlign.Justify else TextAlign.Start
  val spaceTracking = (prefs.letterSpacingSp + prefs.wordSpacingSp).sp

  val firstPara =
    remember(sample1, prefs.letterSpacingSp, prefs.wordSpacingSp, prefs.firstLineIndentSp, align) {
      buildAnnotatedString {
        pushStyle(
          androidx.compose.ui.text.ParagraphStyle(
            textAlign = align,
            textIndent = TextIndent(firstLine = prefs.firstLineIndentSp.sp),
          )
        )
        append(sample1)
        if (prefs.wordSpacingSp > 0.01f) {
          sample1.forEachIndexed { i, c ->
            if (c == ' ') addStyle(SpanStyle(letterSpacing = spaceTracking), i, i + 1)
          }
        }
        pop()
      }
    }

  val secondPara =
    remember(sample2, prefs.letterSpacingSp, prefs.wordSpacingSp, prefs.firstLineIndentSp, align) {
      buildAnnotatedString {
        pushStyle(
          androidx.compose.ui.text.ParagraphStyle(
            textAlign = align,
            textIndent = TextIndent(firstLine = prefs.firstLineIndentSp.sp),
          )
        )
        append(sample2)
        if (prefs.wordSpacingSp > 0.01f) {
          sample2.forEachIndexed { i, c ->
            if (c == ' ') addStyle(SpanStyle(letterSpacing = spaceTracking), i, i + 1)
          }
        }
        pop()
      }
    }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "Мгновенный предпросмотр (${prefs.fontOption.title})",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
      val baseStyle =
        TextStyle(
          fontFamily = prefs.fontOption.fontFamily,
          fontSize = (prefs.fontSizeSp * 0.85f).coerceIn(13f, 22f).sp,
          lineHeight = ((prefs.fontSizeSp * 0.85f).coerceIn(13f, 22f) * prefs.lineHeightMultiplier).sp,
          letterSpacing = prefs.letterSpacingSp.sp,
          color = MaterialTheme.colorScheme.onSurface,
        )
      Column(verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp)) {
        Text(text = firstPara, style = baseStyle)
        Text(text = secondPara, style = baseStyle)
      }
    }
  }
}

@Composable
private fun SectionTabPill(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgColor by
    animateColorAsState(
      targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
      animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      label = "tabPillBg",
    )
  val textColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      label = "tabPillContent",
    )

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = bgColor,
    contentColor = textColor,
    modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StepperCircleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier =
      Modifier.size(36.dp)
        .minimumInteractiveComponentSize()
        .clip(CircleShape)
        .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
private fun FineTunedSliderRow(
  label: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  step: Float,
  onValueChange: (Float) -> Unit,
  testTag: String? = null,
) {
  val haptics = LocalExpressiveHaptics.current
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier =
            Modifier.size(28.dp)
              .clip(CircleShape)
              .clickable {
                haptics.tick()
                onValueChange((value - step).coerceIn(range.start, range.endInclusive))
              },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(15.dp))
          }
        }
        Text(
          text = valueText,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier =
            Modifier.size(28.dp)
              .clip(CircleShape)
              .clickable {
                haptics.tick()
                onValueChange((value + step).coerceIn(range.start, range.endInclusive))
              },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = "Увеличить", modifier = Modifier.size(15.dp))
          }
        }
      }
    }
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      valueRange = range,
      modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderThemeAndMotionSheet(
  prefs: ReaderPreferences,
  onSelectReaderTheme: (ReaderTheme) -> Unit,
  onSelectPageAnimation: (PageFlipAnimation) -> Unit,
  onDismiss: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
        text = "Бумага и анимация страниц",
        style = MaterialTheme.typography.headlineMedium,
      )

      Text(
        text = "Цветовая тема страницы",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(
          items = ReaderTheme.entries,
          key = { it.name },
          contentType = { "theme_swatch" },
        ) { theme ->
          val selected = theme == prefs.readerTheme
          val previewBg =
            if (theme.useSystemDynamic) MaterialTheme.colorScheme.surfaceContainerLowest
            else theme.background
          val previewFg =
            if (theme.useSystemDynamic) MaterialTheme.colorScheme.onSurface
            else theme.text
          val previewAccent =
            if (theme.useSystemDynamic) MaterialTheme.colorScheme.primary
            else theme.accent

          val scale by
            animateFloatAsState(
              targetValue = if (selected) 1.06f else 1f,
              animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
              label = "themeSwatchScale",
            )
          val boxShape = if (selected) ThemeSelectedScallop else ThemeDefaultShape

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
              Modifier.graphicsLayer {
                  scaleX = scale
                  scaleY = scale
                }
                .clip(ThemeDefaultShape)
                .clickable {
                  haptics.morph()
                  onSelectReaderTheme(theme)
                }
                .padding(4.dp)
                .testTag("reader_theme_${theme.name.lowercase()}"),
          ) {
            Box(
              modifier =
                Modifier.size(72.dp)
                  .clip(boxShape)
                  .background(previewBg)
                  .border(
                    width = if (selected) 3.dp else 1.dp,
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
                    shape = boxShape,
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Аа",
                color = previewFg,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Box(
                modifier =
                  Modifier.align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(previewAccent)
              )
            }
            Text(
              text = theme.title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Text(
        text = "Эффект перелистывания",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageFlipAnimation.entries.forEach { anim ->
          val selected = anim == prefs.pageAnimation
          val corner by
            animateDpAsState(
              targetValue = if (selected) 28.dp else 18.dp,
              animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
              label = "animCorner",
            )
          Surface(
            shape = RoundedCornerShape(corner),
            color =
              if (selected) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainer,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(corner))
                .clickable {
                  haptics.morph()
                  onSelectPageAnimation(anim)
                }
                .testTag("page_anim_${anim.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = anim.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = anim.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (selected) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Активно",
                    modifier = Modifier.padding(6.dp).size(16.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTocBookmarksSheet(
  chapters: List<BookChapter>,
  currentChapterIndex: Int,
  bookmarks: List<BookmarkEntity>,
  notes: List<NoteEntity>,
  initialTab: Int = 0,
  onSelectChapter: (Int) -> Unit,
  onSelectBookmark: (BookmarkEntity) -> Unit,
  onSelectNote: (NoteEntity) -> Unit,
  onEditNote: (NoteEntity) -> Unit,
  onDeleteNote: (Long) -> Unit,
  onAddCurrentBookmark: () -> Unit,
  onDeleteBookmark: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxListHeight = (maxHeight - 160.dp).coerceIn(220.dp, 480.dp)

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MorphingFilterChip(
            label = "Главы (${chapters.size})",
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("toc_tab_chapters"),
          )
          MorphingFilterChip(
            label = "Закладки (${bookmarks.size})",
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("toc_tab_bookmarks"),
          )
          MorphingFilterChip(
            label = "Заметки (${notes.size})",
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            modifier = Modifier.testTag("toc_tab_notes"),
          )
        }

        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = { verticalSectionTransitionSpec(forward = targetState > initialState) },
          label = "tocBookmarksNotesMorph",
          modifier = Modifier.fillMaxWidth(),
        ) { tab ->
          when (tab) {
            0 -> {
              LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
              ) {
          itemsIndexed(
            items = chapters,
            key = { index, chapter -> "${index}_${chapter.title}" },
            contentType = { _, _ -> "chapter_item" },
          ) { index, chapter ->
            val isCurrent = index == currentChapterIndex
            Surface(
              shape = RoundedCornerShape(if (isCurrent) 24.dp else 16.dp),
              color =
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(if (isCurrent) 24.dp else 16.dp))
                  .clickable {
                    haptics.tap()
                    onSelectChapter(index)
                    onDismiss()
                  },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color =
                      if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                  )
                }
                if (isCurrent) {
                  Text(
                    text = "Сейчас",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
              }
            }
            1 -> {
              Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(22.dp))
              .clickable {
                haptics.confirm()
                onAddCurrentBookmark()
              },
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.BookmarkAdd,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Сохранить закладку на текущей странице",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        if (bookmarks.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Пока нет закладок в этой книге",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
          ) {
            items(
              items = bookmarks,
              key = { it.id },
              contentType = { "bookmark_item" },
            ) { mark ->
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                      haptics.tap()
                      onSelectBookmark(mark)
                      onDismiss()
                    },
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "${mark.chapterTitle} • Стр. ${mark.pageIndex + 1}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = mark.excerpt,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  IconButton(
                    onClick = {
                      haptics.heavy()
                      onDeleteBookmark(mark.id)
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Удалить закладку",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
              }
            }
              }
            }
            else -> {
              if (notes.isEmpty()) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  NoteIllustrationBadge(
                    palette = NoteHighlightPalette[0],
                    modifier = Modifier.size(64.dp),
                  )
                  Text(
                    text = "Заметок пока нет",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "Удерживайте палец на любом слове в книге, выделите фрагмент и выберите «Заметка».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  contentPadding = PaddingValues(bottom = 8.dp),
                ) {
            items(
              items = notes,
              key = { it.id },
              contentType = { "note_item" },
            ) { note ->
              val pal = notePaletteFor(note.colorIndex)
              Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                      haptics.tap()
                      onSelectNote(note)
                      onDismiss()
                    }
                    .testTag("note_item_${note.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      NoteIllustrationBadge(palette = pal, modifier = Modifier.size(22.dp))
                      Text(
                        text = "${note.chapterTitle} • Стр. ${note.pageIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = pal.accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                      IconButton(
                        onClick = {
                          haptics.tap()
                          onEditNote(note)
                        }
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Edit,
                          contentDescription = "Редактировать заметку",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(19.dp),
                        )
                      }
                      IconButton(
                        onClick = {
                          haptics.heavy()
                          onDeleteNote(note.id)
                        }
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.DeleteOutline,
                          contentDescription = "Удалить заметку",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(19.dp),
                        )
                      }
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pal.paperTint.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = "«${note.selectedText}»",
                      style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                  }

                  if (note.noteText.isNotBlank()) {
                    Text(
                      text = note.noteText,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
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
}
    }
  }
}
