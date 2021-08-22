package au.com.codeka.warworlds.client.store

import au.com.codeka.warworlds.common.Log
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import java.io.IOException

/** Helper for serializing and deserializing protobuf messages in a generic fashion.  */
internal class ProtobufSerializer<M : Message<*, *>>(cls: Class<M>) {
  private var protoAdapter: ProtoAdapter<M> = createAdapter(cls)

  fun serialize(value: M): ByteArray {
    return value.encode()
  }

  fun deserialize(value: ByteArray): M {
    return try {
      protoAdapter.decode(value)
    } catch (e: IOException) {
      log.error("Exception deserializing protobuf.", e)
      throw RuntimeException(e)
    }
  }

  companion object {
    private val log = Log("ProtobufSerializer")

    @Suppress("unchecked_cast")
    fun <M : Message<*, *>?> createAdapter(cls: Class<M>): ProtoAdapter<M> {
      val f = cls.getField("ADAPTER")
      return f.get(null) as ProtoAdapter<M>
    }
  }
}