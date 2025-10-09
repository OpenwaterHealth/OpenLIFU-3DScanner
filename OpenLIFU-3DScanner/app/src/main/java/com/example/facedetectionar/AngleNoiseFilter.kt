package com.example.facedetectionar

/**
 * Robust 1D angle denoiser for degrees in [-180, 180].
 * Pipeline: unwrap -> Hampel median outlier reject -> slew limit -> EMA -> deadband.
 */
class AngleNoiseFilter(
    private val windowSize: Int = 9,          // odd number recommended: 5–11
    private val deadband: Float = 0.2f,
    private val emaAlpha: Float = 0.25f, // 0.15–0.35 works well
    maxDegPerSec: Float = 180f,   // cap how fast the angle is allowed to change
    private val fpsHint: Float = 30f
) {
    private val win = ArrayDeque<Float>(windowSize.coerceAtLeast(3))
//    private val win = ArrayDeque<Float>(windowSize.coerceAtLeast(3) or 1)
    private val kHampel = 3.0f    // threshold in MADs
    private val madScale = 1.4826f
    private val maxStepPerFrame = (maxDegPerSec / fpsHint)

    private var lastRawUnwrapped = 0f
    private var lastEma = 0f
    private var hasInit = false

    fun reset() {
        win.clear()
        hasInit = false
        lastEma = 0f
        lastRawUnwrapped = 0f
    }

    /** Call this for each new raw angle in degrees [-180,180] or any deg; wrap is handled. */
    fun update(rawAngleDeg: Float): Float {
        // 1) Unwrap to keep continuity across -180/180 boundary.
        val a = unwrap(rawAngleDeg)

        // 2) Push into window
        if (win.size == 0) {
            // prime the window with the first value to avoid startup transients
            repeat(3) { win.add(a) }
        }
        win.add(a)
        if (win.size > 21) { // hard cap so window doesn't grow if constructor asked small
            win.removeFirst()
        }
        while (win.size > windowTargetSize()) win.removeFirst()

        // 3) Hampel: replace current sample if it’s an outlier vs median/MAD of the window
        val denoised = hampelReplace(win, kHampel, madScale)

        // 4) Slew limit per frame (assumes ~fpsHint)
        val prev = if (hasInit) lastEma else denoised
        val maxStep = maxStepPerFrame
        val step = clamp(denoised - prev, -maxStep, maxStep)
        val slew = prev + step

        // 5) EMA smoothing
        val ema = if (hasInit) lerp(prev, slew, emaAlpha) else slew

        // 6) Deadband: freeze tiny oscillations
        val out = if (hasInit && kotlin.math.abs(ema - lastEma) < deadband) lastEma else ema

        lastEma = out
        hasInit = true
        return normalize180(out)
    }

    // --- internals ---

    private fun windowTargetSize(): Int {
        // make sure window size is always odd (better for median)

        val n = windowSize.coerceAtLeast(3)
        return if (n % 2 == 0) n - 1 else n
    }
    private fun unwrap(curr: Float): Float {
        val c = normalize180(curr)
        if (!hasInit) {
            lastRawUnwrapped = c
            return c
        }
        var delta = c - normalize180(lastRawUnwrapped)
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        lastRawUnwrapped += delta
        return lastRawUnwrapped
    }

    private fun hampelReplace(window: ArrayDeque<Float>, k: Float, scale: Float): Float {
        // compute median & MAD
        val arr = window.toFloatArray()
        val med = median(arr)
        val absDev = FloatArray(arr.size) { i -> kotlin.math.abs(arr[i] - med) }
        val mad = median(absDev).coerceAtLeast(1e-6f)
        val x = arr.last()
        val thresh = k * scale * mad
        return if (kotlin.math.abs(x - med) > thresh) med else x
    }

    private fun median(a: FloatArray): Float {
        val b = a.copyOf()
        b.sort()
        val n = b.size
        val mid = n / 2
        return if (n % 2 == 1) b[mid] else 0.5f * (b[mid - 1] + b[mid])
    }

    private fun clamp(x: Float, lo: Float, hi: Float): Float =
        if (x < lo) lo else if (x > hi) hi else x

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun normalize180(d: Float): Float {
        var x = d % 360f
        if (x <= -180f) x += 360f
        if (x > 180f) x -= 360f
        return x
    }
}
