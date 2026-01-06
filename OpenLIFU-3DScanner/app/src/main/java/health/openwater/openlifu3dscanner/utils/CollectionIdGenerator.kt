package health.openwater.openlifu3dscanner.utils

import java.security.SecureRandom

object CollectionIdGenerator {

    private val random = SecureRandom()

    // Short, readable words (no ambiguity)
    private val words = listOf(
        "NOVA", "ECHO", "LUNA", "ORION", "ATLAS",
        "COMET", "NEBULA", "LYRA", "VEGA", "SOLAR"
    )

    // Crockford Base32 (human-friendly)
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(): String {
        val word = words[random.nextInt(words.size)]
        val suffix = generateBase32Suffix()
        return "$word-$suffix"
    }

    private fun generateBase32Suffix(length: Int = 4): String {
        val now = System.currentTimeMillis()
        var value = now xor random.nextLong()

        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = ALPHABET[(value and 31).toInt()]
            value = value ushr 5
        }
        return String(chars)
    }
}
