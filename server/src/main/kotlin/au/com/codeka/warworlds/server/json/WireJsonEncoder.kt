package au.com.codeka.warworlds.server.json

import au.com.codeka.warworlds.common.Log
import com.google.gson.stream.JsonWriter
import java.lang.Exception
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

class WireJsonEncoder {
  private val log = Log("WireJsonEncoder")

  fun encode(writer: JsonWriter, value: Any) {
    try {
      return writeObject(writer, value)
    } catch (e: WireJsonException) {
      throw e
    } catch (e: Exception) {
      throw WireJsonException("Unexpected error", e = e)
    }
  }

  private fun writeObject(writer: JsonWriter, value: Any) {
    writer.beginObject()

    val properties = findProperties(value::class)
    for ((name, prop) in properties) {
      val v = prop.getter.call(value)

      writer.name(name)
      writeValue(writer, v, prop.returnType)
    }

    writer.endObject()
  }

  private fun writeValue(writer: JsonWriter, value: Any?, type: KType) {
    if (value == null) {
      writer.nullValue()
      return
    }

    if (type.withNullability(false).isSubtypeOf(Enum::class.starProjectedType)) {
      // For enums, we'll just write the value as a string directly.
      writer.value(value.toString())
      return
    }

    when (type.jvmErasure) {
      Int::class -> {
        writer.value(value as Number)
      }
      Long::class -> {
        writer.value(value as Long)
      }
      Double::class -> {
        writer.value(value as Double)
      }
      Float::class -> {
        writer.value(value as Float)
      }
      String::class -> {
        writer.value(value as String)
      }
      List::class -> {
        writeArray(writer, value as List<Any>, type)
      }
      Boolean::class -> {
        writer.value(value as Boolean)
      }
      else -> {
        writeObject(writer, value)
      }
    }
  }

  private fun writeArray(writer: JsonWriter, array: List<Any>, type: KType) {
    var arrayType: KType? = null
    for (arg in type.arguments) {
      arrayType = arg.type
    }

    writer.beginArray()
    for (elem in array) {
      writeValue(writer, elem, arrayType!!)
    }
    writer.endArray()
  }
}