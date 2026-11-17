package com.example.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToLong

@Stable
class ExpressiveHaptics(
  private val context: Context,
  private val view: View,
  var enabled: Boolean = true,
) {
  private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.NONE) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vm?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
      }
      .getOrNull()
  }

  private fun pulse(
    durationMs: Float,
    amplitude: Int,
    fallbackHapticConstant: Int,
  ) {
    if (!enabled) return
    val vib = vibrator
    if (vib != null && vib.hasVibrator()) {
      runCatching {
        val dur = durationMs.roundToLong().coerceAtLeast(8L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val amp =
            if (vib.hasAmplitudeControl()) {
              amplitude.coerceIn(1, 255)
            } else {
              VibrationEffect.DEFAULT_AMPLITUDE
            }
          vib.vibrate(VibrationEffect.createOneShot(dur, amp))
        } else {
          @Suppress("DEPRECATION") vib.vibrate(dur)
        }
        return
      }
    }
    view.performHapticFeedback(fallbackHapticConstant)
  }

  fun tick() {
    val fallback =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_TICK
      } else {
        HapticFeedbackConstants.CLOCK_TICK
      }
    pulse(durationMs = 14f, amplitude = 195, fallbackHapticConstant = fallback)
  }

  fun tap() {
    val fallback =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        HapticFeedbackConstants.CONTEXT_CLICK
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
    pulse(durationMs = 19f, amplitude = 225, fallbackHapticConstant = fallback)
  }

  fun morph() {
    val fallback =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.TOGGLE_ON
      } else {
        HapticFeedbackConstants.KEYBOARD_TAP
      }
    pulse(durationMs = 24f, amplitude = 245, fallbackHapticConstant = fallback)
  }

  fun confirm() {
    val fallback =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
      } else {
        HapticFeedbackConstants.LONG_PRESS
      }
    pulse(durationMs = 30f, amplitude = 255, fallbackHapticConstant = fallback)
  }

  fun heavy() {
    pulse(
      durationMs = 38f,
      amplitude = 255,
      fallbackHapticConstant = HapticFeedbackConstants.LONG_PRESS,
    )
  }
}

val LocalExpressiveHaptics =
  compositionLocalOf<ExpressiveHaptics> {
    error("LocalExpressiveHaptics not provided")
  }

@Composable
fun rememberExpressiveHaptics(
  enabled: Boolean,
): ExpressiveHaptics {
  val context = LocalContext.current
  val view = LocalView.current
  return remember(context, view) {
    ExpressiveHaptics(context = context, view = view, enabled = enabled)
  }.apply {
    this.enabled = enabled
  }
}
