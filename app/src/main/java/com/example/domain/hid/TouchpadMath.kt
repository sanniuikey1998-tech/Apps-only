package com.example.domain.hid

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Pure sub-pixel pointer physics & scroll accumulator.
 * Prevents slow micro-movements from being truncated to 0 and adds smooth velocity acceleration.
 */
class TouchpadMath {
    private var remainderX = 0f
    private var remainderY = 0f
    private var scrollRemV = 0f
    private var scrollRemH = 0f

    data class MouseDelta(val dx: Int, val dy: Int)
    data class ScrollDelta(val wheel: Int, val pan: Int)

    fun reset() {
        remainderX = 0f
        remainderY = 0f
        scrollRemV = 0f
        scrollRemH = 0f
    }

    fun computePointerDelta(
        rawDx: Float,
        rawDy: Float,
        pointerSpeed: Float,
        sensitivity: Float
    ): MouseDelta {
        val magnitude = kotlin.math.hypot(rawDx, rawDy)
        // Subtle non-linear acceleration curve for fine precision at low speed and fast sweep at high speed
        val accelFactor = 1f + ((magnitude / 18f).coerceAtMost(2.5f)).pow(1.25f) * (sensitivity * 0.45f)
        val scale = pointerSpeed.coerceIn(0.3f, 4.0f) * accelFactor

        val scaledX = rawDx * scale + remainderX
        val scaledY = rawDy * scale + remainderY

        val intX = scaledX.roundToInt().coerceIn(-127, 127)
        val intY = scaledY.roundToInt().coerceIn(-127, 127)

        remainderX = scaledX - intX
        remainderY = scaledY - intY

        return MouseDelta(intX, intY)
    }

    fun computeScrollDelta(
        rawDx: Float,
        rawDy: Float,
        naturalScrolling: Boolean,
        sensitivity: Float
    ): ScrollDelta {
        // Standard touchpad scroll: 1 HID notch per ~26px of finger drag
        val pixelsPerNotch = (28f / sensitivity.coerceIn(0.4f, 2.5f)).coerceAtLeast(8f)
        val dirSign = if (naturalScrolling) -1f else 1f

        scrollRemV += (rawDy * dirSign) / pixelsPerNotch
        scrollRemH += (-rawDx * dirSign) / pixelsPerNotch

        val vNotches = if (abs(scrollRemV) >= 1f) {
            val whole = scrollRemV.toInt().coerceIn(-12, 12)
            scrollRemV -= whole
            // Windows wheel positive = scroll up, negative = scroll down
            -whole
        } else 0

        val hNotches = if (abs(scrollRemH) >= 1f) {
            val whole = scrollRemH.toInt().coerceIn(-12, 12)
            scrollRemH -= whole
            whole
        } else 0

        return ScrollDelta(wheel = vNotches, pan = hNotches)
    }

    companion object {
        fun signum(value: Float): Int = value.sign.toInt()
    }
}
