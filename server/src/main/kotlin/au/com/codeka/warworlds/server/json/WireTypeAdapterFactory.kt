package au.com.codeka.warworlds.server.json

import au.com.codeka.warworlds.common.Log
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.wire.WireField
import java.lang.Exception
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

/**
 * The [TypeAdapterFactory] that wire has implemented doesn't work unless you have the java-interop
 * option turned on. I don't really want to do that, so I've had to build my own.
 * See: https://github.com/square/wire/issues/1794
 */
class WireTypeAdapterFactory : TypeAdapterFactory {
  private val log = Log("WireTypeAdapterFactory")

  override fun <T : Any?> create(gson: Gson?, type: TypeToken<T>): TypeAdapter<T> {
    return WireTypeAdapter(gson, type)
  }

  class WireTypeAdapter<T>(
    private val gson: Gson?, private val type: TypeToken<T>) : TypeAdapter<T>() {

    override fun write(out: JsonWriter?, value: T) {
      //
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(reader: JsonReader): T? {
      return WireJsonDecoder().decode(reader, type.rawType) as T
    }
  }
}
