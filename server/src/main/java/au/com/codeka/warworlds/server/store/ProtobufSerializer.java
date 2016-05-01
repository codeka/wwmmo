package au.com.codeka.warworlds.server.store;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * A mapdb serializer for serializing protocol buffer messages.
 */
public class ProtobufSerializer<M extends Message<?, ?>> implements Serializer<M>, Serializable {
  private ProtoAdapter<M> protoAdapter;

  public ProtobufSerializer(Class<M> cls) {
    try {
      Field f = cls.getField("ADAPTER");
      protoAdapter = (ProtoAdapter<M>) f.get(null);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected exception getting ADAPTER: " + e.getMessage());
    }
  }

  @Override
  public void serialize(@NotNull DataOutput2 out, @NotNull M value) throws IOException {
    out.write(value.encode());
  }

  @Override
  public M deserialize(@NotNull DataInput2 input, int available) throws IOException {
    byte[] buffer = new byte[available];
    input.readFully(buffer);
    return protoAdapter.decode(buffer);
  }
}
