package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ThemeCrossfadeEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
private const val THEME_TRANSITION_DURATION_MS = 620

@Composable
fun ListayTheme(
  palette: ExpressivePalette = ExpressivePalette.WALLPAPER_DYNAMIC,
  darkModeSetting: DarkModeSetting = DarkModeSetting.SYSTEM,
  hapticsEnabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (darkModeSetting) {
      DarkModeSetting.SYSTEM -> systemDark
      DarkModeSetting.LIGHT -> false
      DarkModeSetting.DARK -> true
    }

  val context = LocalContext.current
  val targetScheme =
    remember(palette, isDark, context) {
      when (palette) {
        ExpressivePalette.WALLPAPER_DYNAMIC -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
          } else {
            if (isDark) BerryDarkScheme else BerryLightScheme
          }
        }
        ExpressivePalette.BERRY_POP -> if (isDark) BerryDarkScheme else BerryLightScheme
        ExpressivePalette.MINT_BREEZE -> if (isDark) MintDarkScheme else MintLightScheme
        ExpressivePalette.SUNSET_AMBER -> if (isDark) SunsetDarkScheme else SunsetLightScheme
      }
    }

  // Drive all ColorScheme tokens from a single shared Animatable so Light <-> Dark transitions
  // are gentle on the eyes and 100% frame-synchronized across every UI component without flicker.
  var fromScheme by remember { mutableStateOf(targetScheme) }
  var currentScheme by remember { mutableStateOf(targetScheme) }
  val transitionAnim = remember { Animatable(1f) }
  var hasInitialized by remember { mutableStateOf(false) }

  LaunchedEffect(targetScheme) {
    if (!hasInitialized) {
      hasInitialized = true
      fromScheme = targetScheme
      currentScheme = targetScheme
      transitionAnim.snapTo(1f)
      return@LaunchedEffect
    }
    if (targetScheme == currentScheme && transitionAnim.value >= 1f) return@LaunchedEffect
    fromScheme = currentScheme
    transitionAnim.snapTo(0f)
    transitionAnim.animateTo(
      targetValue = 1f,
      animationSpec =
        tween(
          durationMillis = THEME_TRANSITION_DURATION_MS,
          easing = ThemeCrossfadeEasing,
        ),
    ) {
      currentScheme = lerpColorScheme(fromScheme, targetScheme, value)
    }
    currentScheme = targetScheme
  }

  val view = LocalView.current
  val isLightSurface = currentScheme.background.luminance() > 0.5f
  if (!view.isInEditMode) {
    LaunchedEffect(view, isLightSurface) {
      view.context.findActivity()?.window?.let { window ->
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = isLightSurface
        controller.isAppearanceLightNavigationBars = isLightSurface
      }
    }
  }

  val haptics = rememberExpressiveHaptics(enabled = hapticsEnabled)

  CompositionLocalProvider(LocalExpressiveHaptics provides haptics) {
    MaterialTheme(
      colorScheme = currentScheme,
      typography = ExpressiveTypography,
      shapes = ExpressiveShapes,
      content = content,
    )
  }
}

private fun lerpColorScheme(start: ColorScheme, stop: ColorScheme, fraction: Float): ColorScheme {
  val t = fraction.coerceIn(0f, 1f)
  if (t <= 0f) return start
  if (t >= 1f) return stop
  return stop.copy(
    primary = lerp(start.primary, stop.primary, t),
    onPrimary = lerp(start.onPrimary, stop.onPrimary, t),
    primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, t),
    onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, t),
    inversePrimary = lerp(start.inversePrimary, stop.inversePrimary, t),
    secondary = lerp(start.secondary, stop.secondary, t),
    onSecondary = lerp(start.onSecondary, stop.onSecondary, t),
    secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, t),
    onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, t),
    tertiary = lerp(start.tertiary, stop.tertiary, t),
    onTertiary = lerp(start.onTertiary, stop.onTertiary, t),
    tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, t),
    onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, t),
    background = lerp(start.background, stop.background, t),
    onBackground = lerp(start.onBackground, stop.onBackground, t),
    surface = lerp(start.surface, stop.surface, t),
    onSurface = lerp(start.onSurface, stop.onSurface, t),
    surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, t),
    onSurfaceVariant = lerp(start.onSurfaceVariant, stop.onSurfaceVariant, t),
    surfaceTint = lerp(start.surfaceTint, stop.surfaceTint, t),
    inverseSurface = lerp(start.inverseSurface, stop.inverseSurface, t),
    inverseOnSurface = lerp(start.inverseOnSurface, stop.inverseOnSurface, t),
    error = lerp(start.error, stop.error, t),
    onError = lerp(start.onError, stop.onError, t),
    errorContainer = lerp(start.errorContainer, stop.errorContainer, t),
    onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, t),
    outline = lerp(start.outline, stop.outline, t),
    outlineVariant = lerp(start.outlineVariant, stop.outlineVariant, t),
    scrim = lerp(start.scrim, stop.scrim, t),
    surfaceBright = lerp(start.surfaceBright, stop.surfaceBright, t),
    surfaceDim = lerp(start.surfaceDim, stop.surfaceDim, t),
    surfaceContainer = lerp(start.surfaceContainer, stop.surfaceContainer, t),
    surfaceContainerHigh = lerp(start.surfaceContainerHigh, stop.surfaceContainerHigh, t),
    surfaceContainerHighest = lerp(start.surfaceContainerHighest, stop.surfaceContainerHighest, t),
    surfaceContainerLow = lerp(start.surfaceContainerLow, stop.surfaceContainerLow, t),
    surfaceContainerLowest = lerp(start.surfaceContainerLowest, stop.surfaceContainerLowest, t),
  )
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
