package au.com.codeka.warworlds.server.store;

import com.squareup.wire.Message;

import javax.annotation.Nullable;

/**
 * {@link ProtobufStore} is basically a map of key/value pairs where the key is an 64-bit int and
 * the value is a protocol buffer of type M.
 */
public class ProtobufStore<M extends Message<?, ?>> extends BaseStore {
  private final ProtobufSerializer<M> serializer;

  /* package */ ProtobufStore(String fileName, Class<M> cls) {
    super(fileName);
    this.serializer = new ProtobufSerializer<>(cls);
  }

  @Nullable
  public M get(long id) {
    return null;
  }

  public void put(long id, M value) {
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE protos (" +
              "  id INTEGER PRIMARY KEY," +
              "  value BLOB)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
