package au.com.codeka.warworlds.server.store;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.Map;

/** Wraps our reference to MapDB's data store objects. */
public class DataStore {
  public static final DataStore i = new DataStore();

  private final DB db;

  private DataStore() {
    db = DBMaker
        .fileDB("data/store.db")
        .transactionEnable()
        .make();
  }

  /**
   * Gets the map of empire name to ID, useful to determine whether a name already exists or not.
   */
  public Map<String, Long> empireNames() {
    return null;
  }
}
