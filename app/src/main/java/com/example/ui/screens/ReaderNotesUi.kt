package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NoteEntity
import com.example.ui.theme.LocalExpressiveHaptics
import kotlin.math.roundToInt

data class NotePaletteOption(
  val title: String,
  val paperTint: Color,
  val accent: Color,
)

val NoteHighlightPalette =
  listOf(
    NotePaletteOption("Янтарь", Color(0xFFFEF08A), Color(0xFFD97706)),
    NotePaletteOption("Мята", Color(0xFFA7F3D0), Color(0xFF059669)),
    NotePaletteOption("Лаванда", Color(0xFFDDD6FE), Color(0xFF7C3AED)),
    NotePaletteOption("Коралл", Color(0xFFFBCFE8), Color(0xFFDB2777)),
  )

fun notePaletteFor(colorIndex: Int): NotePaletteOption =
  NoteHighlightPalette[colorIndex.coerceIn(0, NoteHighlightPalette.lastIndex).let {
    if (colorIndex < 0) 0 else colorIndex % NoteHighlightPalette.size
  }]

/**
 * Crisp custom multi-layer illustrated sticky-note icon ("картинка заметки") rendered with vector
 * paths so it remains razor-sharp at every size and theme.
 */
@Composable
fun NoteIllustrationBadge(
  modifier: Modifier = Modifier,
  palette: NotePaletteOption = NoteHighlightPalette[0],
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val fold = w * 0.28f
    val pad = w * 0.06f
    val left = pad
    val top = pad
    val right = w - pad
    val bottom = h - pad
    val radius = w * 0.14f

    // Soft offset backing card
    drawRoundRect(
      color = palette.accent.copy(alpha = 0.24f),
      topLeft = Offset(left + w * 0.06f, top + h * 0.06f),
      size = Size(right - left, bottom - top),
      cornerRadius = CornerRadius(radius, radius),
    )

    // Main folded sticky note path
    val notePath =
      Path().apply {
        moveTo(left + radius, top)
        lineTo(right - radius, top)
        quadraticTo(right, top, right, top + radius)
        lineTo(right, bottom - fold)
        lineTo(right - fold, bottom)
        lineTo(left + radius, bottom)
        quadraticTo(left, bottom, left, bottom - radius)
        lineTo(left, top + radius)
        quadraticTo(left, top, left + radius, top)
        close()
      }
    drawPath(path = notePath, color = palette.paperTint)
    drawPath(
      path = notePath,
      color = palette.accent,
      style = Stroke(width = (w * 0.065f).coerceAtLeast(1.2f)),
    )

    // Folded bottom-right triangle
    val foldPath =
      Path().apply {
        moveTo(right - fold, bottom)
        lineTo(right - fold, bottom - fold + (w * 0.04f))
        quadraticTo(
          right - fold,
          bottom - fold,
          right - fold + (w * 0.04f),
          bottom - fold,
        )
        lineTo(right, bottom - fold)
        close()
      }
    drawPath(path = foldPath, color = palette.accent.copy(alpha = 0.85f))

    // Ruled ink strokes inside note
    val strokeW = (w * 0.075f).coerceAtLeast(1.4f)
    val lineColor = palette.accent.copy(alpha = 0.92f)
    drawLine(
      color = lineColor,
      start = Offset(left + w * 0.18f, top + h * 0.30f),
      end = Offset(right - w * 0.18f, top + h * 0.30f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = lineColor,
      start = Offset(left + w * 0.18f, top + h * 0.50f),
      end = Offset(right - w * 0.24f, top + h * 0.50f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = lineColor.copy(alpha = 0.65f),
      start = Offset(left + w * 0.18f, top + h * 0.69f),
      end = Offset(left + w * 0.48f, top + h * 0.69f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
  }
}

enum class SelectionCaptureAction {
  SYSTEM_COPY,
  NOTE,
  BOOKMARK,
}

class ReaderClipboardInterceptor(
  private val delegate: ClipboardManager,
  private val onInterceptText: (String, SelectionCaptureAction) -> Unit,
) : ClipboardManager {
  var pendingAction: SelectionCaptureAction = SelectionCaptureAction.SYSTEM_COPY

  override fun getText(): AnnotatedString? = delegate.getText()

  override fun hasText(): Boolean = delegate.hasText()

  override fun setText(annotatedString: AnnotatedString) {
    val action = pendingAction
    pendingAction = SelectionCaptureAction.SYSTEM_COPY
    if (action == SelectionCaptureAction.SYSTEM_COPY) {
      delegate.setText(annotatedString)
    } else {
      onInterceptText(annotatedString.text, action)
    }
  }
}

class ReaderTextToolbarController : TextToolbar {
  override var status by mutableStateOf(TextToolbarStatus.Hidden)
    private set

  var anchorRect by mutableStateOf(Rect.Zero)
    private set

  var onCopyRequested: (() -> Unit)? by mutableStateOf(null)
    private set

  var onSelectAllRequested: (() -> Unit)? by mutableStateOf(null)
    private set

  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?,
  ) {
    anchorRect = rect
    this.onCopyRequested = onCopyRequested
    this.onSelectAllRequested = onSelectAllRequested
    status = TextToolbarStatus.Shown
  }

  override fun hide() {
    status = TextToolbarStatus.Hidden
    onCopyRequested = null
    onSelectAllRequested = null
  }
}

/**
 * Expressive M3 floating selection action pill positioned near the user's highlighted passage.
 * Offers:
 * 1) "Заметка" (with illustrated note graphic icon)
 * 2) "Закладка" (with bookmark icon)
 * 3) "Копия" (standard clipboard copy)
 */
@Composable
fun ReaderSelectionToolbarOverlay(
  controller: ReaderTextToolbarController,
  onCreateNoteClick: () -> Unit,
  onCreateBookmarkClick: () -> Unit,
  onCopyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isVisible =
    controller.status == TextToolbarStatus.Shown && controller.anchorRect != Rect.Zero
  var pillSize by remember { mutableStateOf(IntSize(640, 128)) }

  BoxWithConstraints(modifier = modifier.fillMaxSize().statusBarsPadding()) {
    val containerW = constraints.maxWidth
    val containerH = constraints.maxHeight
    val rect = controller.anchorRect

    AnimatedVisibility(
      visible = isVisible,
      enter =
        scaleIn(
          animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
          initialScale = 0.88f,
        ) + fadeIn(tween(120)),
      exit = scaleOut(targetScale = 0.92f) + fadeOut(tween(90)),
      modifier =
        Modifier.offset {
          val edgePad = 24
          val gapPx = 28
          val targetX =
            (rect.center.x - pillSize.width / 2f)
              .roundToInt()
              .coerceIn(edgePad, (containerW - pillSize.width - edgePad).coerceAtLeast(edgePad))
          val aboveY = rect.top.roundToInt() - pillSize.height - gapPx
          val belowY = rect.bottom.roundToInt() + gapPx
          val targetY =
            if (aboveY >= edgePad) aboveY
            else belowY.coerceIn(edgePad, (containerH - pillSize.height - edgePad).coerceAtLeast(edgePad))
          IntOffset(targetX, targetY)
        },
    ) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier =
          Modifier.onSizeChanged { measured -> pillSize = measured }
            .testTag("selection_action_toolbar"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color = NoteHighlightPalette[0].paperTint.copy(alpha = 0.55f),
            modifier =
              Modifier.heightIn(min = 42.dp)
                .clip(CircleShape)
                .clickable(onClick = onCreateNoteClick)
                .testTag("selection_note_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              NoteIllustrationBadge(
                palette = NoteHighlightPalette[0],
                modifier = Modifier.size(22.dp),
              )
              Text(
                text = "Заметка",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            modifier =
              Modifier.heightIn(min = 42.dp)
                .clip(CircleShape)
                .clickable(onClick = onCreateBookmarkClick)
                .testTag("selection_bookmark_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.BookmarkAdd,
                contentDescription = "В закладки",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Закладка",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier =
              Modifier.heightIn(min = 42.dp)
                .clip(CircleShape)
                .clickable(onClick = onCopyClick)
                .testTag("selection_copy_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Копировать",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
              )
              Text(
                text = "Копия",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NoteEditorDialog(
  quoteText: String,
  initialNoteText: String,
  initialColorIndex: Int,
  isEditing: Boolean,
  onSave: (noteText: String, colorIndex: Int) -> Unit,
  onDelete: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val haptics = LocalExpressiveHaptics.current
  var noteText by remember(quoteText, initialNoteText) { mutableStateOf(initialNoteText) }
  var selectedColorIndex by remember(initialColorIndex) { mutableIntStateOf(initialColorIndex) }
  val activePalette = notePaletteFor(selectedColorIndex)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 8.dp,
      shadowElevation = 16.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            NoteIllustrationBadge(
              palette = activePalette,
              modifier = Modifier.size(34.dp),
            )
            Text(
              text = if (isEditing) "Редактировать заметку" else "Новая заметка",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          if (isEditing && onDelete != null) {
            IconButton(
              onClick = {
                haptics.heavy()
                onDelete()
              },
              modifier = Modifier.testTag("delete_note_dialog_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Удалить заметку",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = activePalette.paperTint.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, activePalette.accent.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Box(
              modifier =
                Modifier.width(4.dp)
                  .heightIn(min = 24.dp, max = 64.dp)
                  .clip(CircleShape)
                  .background(activePalette.accent)
            )
            Text(
              text = "«$quoteText»",
              style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Highlight color picker
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Цвет:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          NoteHighlightPalette.forEachIndexed { idx, opt ->
            val selected = idx == selectedColorIndex
            Box(
              modifier =
                Modifier.size(32.dp)
                  .minimumInteractiveComponentSize()
                  .clip(CircleShape)
                  .background(opt.paperTint)
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) opt.accent else opt.accent.copy(alpha = 0.4f),
                    shape = CircleShape,
                  )
                  .clickable {
                    haptics.tick()
                    selectedColorIndex = idx
                  },
              contentAlignment = Alignment.Center,
            ) {
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = opt.title,
                  tint = opt.accent,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Мысль или комментарий (необязательно)") },
          placeholder = { Text("Добавьте пояснение к цитате...") },
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth().testTag("note_text_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = {
              haptics.tick()
              onDismiss()
            },
            shape = CircleShape,
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("note_cancel_button"),
          ) {
            Text("Отмена", fontWeight = FontWeight.SemiBold)
          }
          Button(
            onClick = {
              haptics.confirm()
              onSave(noteText, selectedColorIndex)
            },
            shape = CircleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("note_save_button"),
          ) {
            Text("Сохранить", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Applies soft translucent note highlights and clickable links onto rendered chapter paragraphs
 * for quotes saved by the reader.
 */
fun attachNoteHighlights(
  source: AnnotatedString,
  chapterNotes: List<NoteEntity>,
  onNoteClick: (NoteEntity) -> Unit,
): AnnotatedString {
  if (chapterNotes.isEmpty() || source.text.isEmpty()) return source
  val rawText = source.text
  val matches = mutableListOf<Triple<Int, Int, NoteEntity>>()

  for (note in chapterNotes) {
    val snippet = note.selectedText.trim()
    if (snippet.length < 2) continue
    val exactIdx = rawText.indexOf(snippet)
    if (exactIdx >= 0) {
      matches.add(Triple(exactIdx, exactIdx + snippet.length, note))
      continue
    }
    // Fallback: match the first line or leading 48 chars for multi-paragraph selections
    val firstLine = snippet.lineSequence().firstOrNull()?.trim().orEmpty()
    val probe =
      when {
        firstLine.length >= 6 -> firstLine
        snippet.length >= 16 -> snippet.take(48).trim()
        else -> ""
      }
    if (probe.isNotEmpty()) {
      val idx = rawText.indexOf(probe)
      if (idx >= 0) {
        matches.add(Triple(idx, (idx + probe.length).coerceAtMost(rawText.length), note))
      }
    }
  }

  if (matches.isEmpty()) return source
  return buildAnnotatedString {
    append(source)
    matches.forEach { (start, end, note) ->
      val pal = notePaletteFor(note.colorIndex)
      addStyle(
        style =
          SpanStyle(
            background = pal.paperTint.copy(alpha = 0.42f),
            textDecoration = TextDecoration.Underline,
          ),
        start = start,
        end = end,
      )
      val hasExistingLinks = source.getLinkAnnotations(start, end).isNotEmpty()
      if (!hasExistingLinks) {
        addLink(
          clickable =
            LinkAnnotation.Clickable(
              tag = "note_highlight_${note.id}",
              linkInteractionListener = { onNoteClick(note) },
            ),
          start = start,
          end = end,
        )
      }
    }
  }
}
