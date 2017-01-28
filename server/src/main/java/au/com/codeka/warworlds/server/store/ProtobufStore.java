package au.com.codeka.warworlds.server.store;

import com.squareup.wire.Message;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

/**
 * {@link ProtobufStore} is basically a map of key/value pairs where the key is an 64-bit int and
 * the value is a protocol buffer of type M.
 */
public class ProtobufStore<M extends Message<?, ?>> extends BaseStore {
  private static final Log log = new Log("ProtobufStore");
  private final ProtobufSerializer<M> serializer;

  /* package */ ProtobufStore(String fileName, Class<M> cls) {
    super(fileName);
    this.serializer = new ProtobufSerializer<>(cls);
  }

  @Nullable
  public M get(long id) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT value FROM protos WHERE id = ?")
            .param(0, id)
            .query()
    ) {
      if (res.next()) {
        return serializer.deserialize(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public void put(long id, M value) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO protos (id, value) VALUES (?, ?)")
          .param(0, id)
          .param(1, serializer.serialize(value))
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
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
