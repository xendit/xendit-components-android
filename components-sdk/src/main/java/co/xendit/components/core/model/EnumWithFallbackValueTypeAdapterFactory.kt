package co.xendit.components.core.model

import co.xendit.components.util.XLogger
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
internal annotation class FallbackValue

internal object EnumWithFallbackValueTypeAdapterFactory : TypeAdapterFactory {
  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType
    if (!rawType.isEnum) return null

    val enumConstants = rawType.enumConstants?.toList().orEmpty()
    val fallbacks =
      enumConstants.mapNotNull { enumValue ->
        val name = (enumValue as? Enum<*>)?.name ?: return@mapNotNull null
        val field = runCatching { rawType.getField(name) }.getOrNull() ?: return@mapNotNull null
        if (!field.isAnnotationPresent(FallbackValue::class.java)) return@mapNotNull null
        @Suppress("UNCHECKED_CAST")
        enumValue as T
      }

    val delegate = gson.getDelegateAdapter(this, type)
    if (fallbacks.isEmpty()) return delegate
    if (fallbacks.size > 1) {
      throw IllegalArgumentException(
        "Only one enum value can be annotated with @${FallbackValue::class.java.simpleName} for ${rawType.name}"
      )
    }

    val fallbackValue = fallbacks.single()
    return object : TypeAdapter<T>() {
      @Throws(IOException::class)
      override fun write(writer: JsonWriter, value: T) {
        delegate.write(writer, value)
      }

      @Throws(IOException::class)
      override fun read(reader: JsonReader): T {
        if (reader.peek() == JsonToken.NULL) {
          reader.nextNull()
          @Suppress("UNCHECKED_CAST")
          return null as T
        }
        val rawString = reader.nextString()
        val fromDelegate =
          runCatching { delegate.fromJsonTree(JsonPrimitive(rawString)) }.getOrNull()
        return if (fromDelegate != null) {
          fromDelegate
        } else {
          XLogger.w("Unknown enum value: $rawString for ${rawType.name}")
          fallbackValue
        }
      }
    }
  }
}

