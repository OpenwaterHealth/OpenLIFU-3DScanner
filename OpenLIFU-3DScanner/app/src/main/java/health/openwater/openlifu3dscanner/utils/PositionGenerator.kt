package health.openwater.openlifu3dscanner.utils

import kotlin.math.sqrt

object PositionGenerator {

    /**
     * Generate synthetic XYZ positions assuming the subject is at the origin
     * and the camera is always at a fixed distance [radius], looking toward it.
     *
     * For SpatialMatching we only need rough relative positions.
     */
    fun generatePositions(
        forwards: List<FloatArray>,
        radius: Float = 1.0f,
        smoothWindow: Int = 5
    ): List<FloatArray> {
        if (forwards.isEmpty()) return emptyList()

        // 1. Normalize and smooth the forward vectors
        val normForwards = forwards.map { normalize(it.copyOf()) }
        val smoothed = smooth(normForwards, smoothWindow)

        // 2. Map each forward direction to a camera position on a sphere
        return smoothed.map { f ->
            val n = normalize(f.copyOf())
            // subject is at (0,0,0), camera is radius away, looking at origin
            floatArrayOf(
                -n[0] * radius,
                -n[1] * radius,
                -n[2] * radius
            )
        }
    }

    private fun smooth(
        vectors: List<FloatArray>,
        window: Int
    ): List<FloatArray> {
        if (window <= 1) return vectors

        val smoothed = ArrayList<FloatArray>(vectors.size)
        val kernel = FloatArray(window) { 1f / window }

        for (i in vectors.indices) {
            val sum = floatArrayOf(0f, 0f, 0f)

            for (k in 0 until window) {
                val idx = (i - window / 2 + k).coerceIn(0, vectors.lastIndex)
                sum[0] += vectors[idx][0] * kernel[k]
                sum[1] += vectors[idx][1] * kernel[k]
                sum[2] += vectors[idx][2] * kernel[k]
            }

            smoothed.add(normalize(sum))
        }

        return smoothed
    }

    private fun normalize(v: FloatArray): FloatArray {
        val len = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
        return if (len > 0.0001f)
            floatArrayOf(v[0]/len, v[1]/len, v[2]/len)
        else
            floatArrayOf(0f, 0f, 0f)
    }
}