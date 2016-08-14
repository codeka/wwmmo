package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.squareup.wire.Message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

/**
 * {@link StringProtobufStore} is basically a map of key/value pairs where the key is a string and
 * the value is a protocol buffer of type M.
 */
public class StringProtobufStore<M extends Message<?, ?>> extends BaseStore<String, M> {
  private final ProtobufSerializer<M> serializer;
  private static final Charset charset = Charset.forName("utf-8");

  /* package */ StringProtobufStore(Database db, Class<M> cls) {
    super(db);
    this.serializer = new ProtobufSerializer<>(cls);
  }

  @Nonnull
  @Override
  protected DatabaseEntry encodeKey(String id) {
    return new DatabaseEntry(id.getBytes(charset));
  }

  @Override
  protected DatabaseEntry encodeValue(M value) {
    return serializer.serialize(value);
  }

  @Override
  protected String decodeKey(DatabaseEntry databaseEntry) {
    return new String(databaseEntry.getData(), charset);
  }

  @Override
  protected M decodeValue(DatabaseEntry databaseEntry) {
    return serializer.deserialize(databaseEntry);
  }
}
