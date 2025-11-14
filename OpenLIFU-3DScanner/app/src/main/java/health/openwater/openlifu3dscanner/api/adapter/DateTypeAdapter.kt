package health.openwater.openlifu3dscanner.api.adapter

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
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date? {
        return dateFormatter.parse(json.asString)
    }

    override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val dateFormatAsString: String = dateFormatter.format(src)
        return JsonPrimitive(dateFormatAsString)
    }
}