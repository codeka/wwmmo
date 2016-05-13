package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.DatabaseEntry;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

/**
 * A mapdb serializer for serializing protocol buffer messages.
 */
public class ProtobufSerializer<M extends Message<?, ?>> {
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

  public DatabaseEntry serialize(M value) {
    return new DatabaseEntry(value.encode());
  }

  public M deserialize(DatabaseEntry entry) {
    try {
      return protoAdapter.decode(entry.getData());
    } catch (IOException e) {
      log.error("Exception deserializing protobuf.", e);
      throw new RuntimeException(e);
    }
  }
}
