package au.com.codeka.warworlds.client.store;

import au.com.codeka.warworlds.common.Log;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import java.io.IOException;
import java.lang.reflect.Field;

/** Helper for serializing and deserializing protobuf messages in a generic fashion. */
class ProtobufSerializer<M extends Message<?, ?>> {
  private static final Log log = new Log("ProtobufSerializer");
  private ProtoAdapter<M> protoAdapter;

  public ProtobufSerializer(Class<M> cls) {
    try {
      Field f = cls.getField("ADAPTER");
      protoAdapter = (ProtoAdapter<M>) f.get(null);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected exception getting ADAPTER: " + e.getMessage());
    }
  }

  public byte[] serialize(M value) {
    return value.encode();
  }

  public M deserialize(byte[] value) {
    try {
      return protoAdapter.decode(value);
    } catch (IOException e) {
      log.error("Exception deserializing protobuf.", e);
      throw new RuntimeException(e);
    }
  }
}
