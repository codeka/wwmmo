package au.com.codeka.warworlds.server.store;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;

/** Storage for empires. */
public class EmpiresStore extends BaseStore {
  private static final Log log = new Log("EmpiresStore");

  /* package */ EmpiresStore(String fileName) {
    super(fileName);
  }

  @Nullable
  public Empire get(long id) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT value FROM protos WHERE id = ?")
            .param(0, id)
            .query()
    ) {
      if (res.next()) {
        return Empire.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public void put(long id, Empire empire) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO empires (id, empire) VALUES (?, ?)")
          .param(0, id)
          .param(1, empire.encode())
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
              "CREATE TABLE empires (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  empire BLOB)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
