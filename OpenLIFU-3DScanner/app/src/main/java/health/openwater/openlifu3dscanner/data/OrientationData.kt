package health.openwater.openlifu3dscanner.data

data class OrientationData(
    val quaternion: FloatArray,         // [x, y, z, w]
    val rotationMatrix: FloatArray,     // 9 elements
    val forward: FloatArray,            // 3 elements
    val up: FloatArray,                 // 3 elements
    val right: FloatArray               // 3 elements
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrientationData

        if (!quaternion.contentEquals(other.quaternion)) return false
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (!forward.contentEquals(other.forward)) return false
        if (!up.contentEquals(other.up)) return false
        if (!right.contentEquals(other.right)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quaternion.contentHashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + forward.contentHashCode()
        result = 31 * result + up.contentHashCode()
        result = 31 * result + right.contentHashCode()
        return result
    }
}