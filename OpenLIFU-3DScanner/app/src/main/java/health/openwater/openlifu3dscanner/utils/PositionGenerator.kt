package health.openwater.openlifu3dscanner.utils

import kotlin.math.sqrt

object PositionGenerator {
    /**
     * Generate synthetic XYZ positions based on forward vectors.
     *
     * @param forwards List of forward vectors [fx, fy, fz]
     * @param stepSize How far the camera "moves" per frame (in meters)
     * @param smoothWindow Moving average smoothing window size
     *
     * @return List of synthetic XYZ positions
     */
    fun generatePositions(
        forwards: List<FloatArray>,
        stepSize: Float = 0.15f,
        smoothWindow: Int = 5
    ): List<FloatArray> {

        if (forwards.isEmpty()) return emptyList()

        val normForwards = forwards.map { normalize(it.copyOf()) }

        // Smooth with moving average
        val smoothed = smooth(normForwards, smoothWindow)

        val positions = ArrayList<FloatArray>()
        var pos = floatArrayOf(0f, 0f, 0f)

        for (f in smoothed) {
            pos = floatArrayOf(
                pos[0] + f[0] * stepSize,
                pos[1] + f[1] * stepSize,
                pos[2] + f[2] * stepSize
            )
            positions.add(pos)
        }

        return positions
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