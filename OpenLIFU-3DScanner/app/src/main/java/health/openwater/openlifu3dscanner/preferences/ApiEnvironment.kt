package health.openwater.openlifu3dscanner.preferences

enum class ApiEnvironment(
    val key: String,
    val baseUrl: String,
    val enabled: Boolean = true
) {
    PRODUCTION("prod", "https://api.openwater.health/"),
    DEV("dev", "https://dev.api.openwater.health/"),
    SANDBOX("sandbox", "https://sandbox.api.openwater.health/", enabled = false);

    companion object {
        val DEFAULT = PRODUCTION
        fun fromKey(key: String) = entries.find { it.key == key } ?: DEFAULT
    }
}
