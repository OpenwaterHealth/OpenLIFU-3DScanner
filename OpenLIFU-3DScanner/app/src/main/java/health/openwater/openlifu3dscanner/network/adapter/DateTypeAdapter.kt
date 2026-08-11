package health.openwater.openlifu3dscanner.network.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTypeAdapter : JsonDeserializer<Date>, JsonSerializer<Date> {

    private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val DATE_FORMAT_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz"

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val rfc1123Formatter = SimpleDateFormat(DATE_FORMAT_RFC1123, Locale.US)

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date? {
        val s = json.asString
        return try { dateFormatter.parse(s) } catch (_: Exception) { rfc1123Formatter.parse(s) }
    }

    override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val dateFormatAsString: String = dateFormatter.format(src)
        return JsonPrimitive(dateFormatAsString)
    }
}