package au.com.codeka.warworlds.server.json

import au.com.codeka.warworlds.common.Log
import com.google.gson.stream.JsonReader
import com.squareup.wire.WireField
import java.lang.Exception
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

class WireJsonDecoder {
  private val log = Log("WireTypeAdapter")

  fun decode(reader: JsonReader, cls: Class<*>): Any {
    try {
      return readObject(reader, Reflection.createKotlinClass(cls).starProjectedType)
    } catch (e: WireJsonException) {
      e.lineNumber = lineNo(reader)
      throw e
    } catch (e: Exception) {
      throw WireJsonException("Unexpected error", lineNo(reader), e)
    }
  }

  private fun readObject(reader: JsonReader, type: KType): Any {
    // TODO: what if it's not an object?
    reader.beginObject()

    val properties = findProperties(toKClass(type))
    val constructor = toKClass(type).primaryConstructor!!
    val args = mutableMapOf<KParameter, Any?>()

    while (reader.hasNext()) {
      val name = reader.nextName()
      val prop = properties[name]
      if (prop == null) {
        reader.skipValue()
        continue
      }

      for (n in constructor.parameters.indices) {
        val param = constructor.parameters[n]
        if (param.name == name) {
          args[param] = readValue(reader, prop.returnType)
        }
      }
    }
    reader.endObject()

    // Make sure we have all the required parameters before we call the constructor. It would
    // crash, but the message isn't all that helpful.
    val missingValues = ArrayList<String>()
    for (param in constructor.parameters) {
      if (!param.isOptional && !args.containsKey(param)) {
        missingValues.add(param.name!!)
      }
    }
    if (missingValues.isNotEmpty()) {
      throw WireJsonException("Missing required values: $missingValues")
    }

    return constructor.callBy(args)
  }

  private fun readValue(reader: JsonReader, type: KType): Any? {
    // Special case for enums, they'll be strings in the JSON file.
    if (type.withNullability(false).isSubtypeOf(Enum::class.starProjectedType)) {
      val value = reader.nextString()
      for (constant in type.jvmErasure.java.enumConstants) {
        if (constant.toString().lowercase() == value.lowercase()) {
          return constant
        }
      }
    }

    return when (type.jvmErasure) {
      Int::class -> {
        reader.nextInt()
      }
      Long::class -> {
        reader.nextLong()
      }
      Double::class -> {
        reader.nextDouble()
      }
      Float::class -> {
        reader.nextDouble().toFloat()
      }
      String::class -> {
        reader.nextString()
      }
      List::class -> {
        readRepeated(reader, type)
      }
      Boolean::class -> {
        reader.nextBoolean()
      }
      else -> {
        readObject(reader, type)
      }
    }
  }

  private fun readRepeated(reader: JsonReader, type: KType): Any {
    reader.beginArray()

    var arrayType: KType? = null
    for (arg in type.arguments) {
      arrayType = arg.type
    }

    val values = ArrayList<Any?>()
    while (reader.hasNext()) {
      values.add(readValue(reader, arrayType!!))
    }
    reader.endArray()

    return values
  }

  private fun findProperties(kClass: KClass<Any>): HashMap<String, KProperty<*>> {
    val properties = HashMap<String, KProperty<*>>()
    for (prop in kClass.memberProperties) {
      val field = prop.javaField ?: continue
      for (annotation in field.annotations) {
        if (annotation is WireField) {
          properties[prop.name] = prop
          break
        }
      }
    }

    return properties
  }

  @Suppress("UNCHECKED_CAST")
  private fun toKClass(type: KType): KClass<Any> {
    return type.classifier as KClass<Any>
  }

  /**
   * JsonReader doesn't have a public interface for getting line numbers, so we'll fake it.
   */
  private fun lineNo(jsonReader: JsonReader): Int {
    val field = jsonReader.javaClass.getDeclaredField("lineNumber")
    field.trySetAccessible()
    return field.get(jsonReader) as Int
  }
}