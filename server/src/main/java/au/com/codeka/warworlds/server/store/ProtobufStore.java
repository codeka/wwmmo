package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.squareup.wire.Message;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

/**
 * {@link ProtobufStore} is basically a map of key/value pairs where the key is an 64-bit int and
 * the value is a protocol buffer of type M.
 */
public class ProtobufStore<M extends Message<?, ?>> extends BaseStore<Long, M> {
  private final ProtobufSerializer<M> serializer;

  /* package */ ProtobufStore(Database db, Class<M> cls) {
    super(db);
    this.serializer = new ProtobufSerializer<>(cls);
  }

  @Nonnull
  @Override
  protected DatabaseEntry encodeKey(Long id) {
    return new DatabaseEntry(ByteBuffer.allocate(Long.BYTES + 1)
        .put(StoreHelper.KEY_MARKER).putLong(id).array());
  }

  @Override
  protected DatabaseEntry encodeValue(M value) {
    return serializer.serialize(value);
  }

  @Override
  protected Long decodeKey(DatabaseEntry databaseEntry) {
    ByteBuffer bb = ByteBuffer.wrap(databaseEntry.getData());
    if (bb.get() != StoreHelper.KEY_MARKER) {
      return null;
    }

    return bb.getLong();
  }

  @Override
  protected M decodeValue(DatabaseEntry databaseEntry) {
    return serializer.deserialize(databaseEntry);
  }
}
