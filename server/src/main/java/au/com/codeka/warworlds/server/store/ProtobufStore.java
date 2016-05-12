package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.squareup.wire.Message;

import java.nio.ByteBuffer;

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

  @Override
  protected DatabaseEntry encodeKey(Long id) {
    return new DatabaseEntry(ByteBuffer.allocate(Long.BYTES).putLong(id).array());
  }

  @Override
  protected DatabaseEntry encodeValue(M value) {
    return serializer.serialize(value);
  }

  @Override
  protected M decodeValue(DatabaseEntry databaseEntry) {
    return serializer.deserialize(databaseEntry);
  }
}
