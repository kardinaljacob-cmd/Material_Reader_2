package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val ExpressiveShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
  )

/**
 * A Material 3 Expressive morphing shape that smoothly interpolates between a soft squircle/circle
 * and a multi-lobed scalloped badge ("clover" / "sunburst" / "cookie") as [scallopDepth] and
 * [rotationDegrees] animate.
 */
class ExpressiveScallopShape(
  private val lobes: Int = 8,
  private val scallopDepth: Float = 0.12f,
  private val rotationDegrees: Float = 0f,
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val path = Path()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = minOf(cx, cy) * (1f - scallopDepth * 0.5f)
    val steps = 64
    val rotRad = rotationDegrees * (PI.toFloat() / 180f)

    for (i in 0..steps) {
      val theta = (2f * PI.toFloat() * i) / steps
      val wave = cos(lobes * theta)
      val r = baseRadius * (1f + scallopDepth * wave)
      val angle = theta + rotRad
      val x = cx + r * cos(angle)
      val y = cy + r * sin(angle)
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return Outline.Generic(path)
  }
}

/**
 * Morphs between a rounded rectangle (morph = 0f) and a softer pill/squircle (morph = 1f) or
 * diamond-ish organic blob.
 */
class MorphingRoundedShape(
  private val startCornerRatio: Float = 0.16f,
  private val endCornerRatio: Float = 0.50f,
  private val progress: Float = 0f,
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val clamped = progress.coerceIn(0f, 1f)
    val ratio = startCornerRatio + (endCornerRatio - startCornerRatio) * clamped
    val radius = minOf(size.width, size.height) * ratio
    return RoundedCornerShape(radius).createOutline(size, layoutDirection, density)
  }
}
