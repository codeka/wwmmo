package au.com.codeka.warworlds.server.store;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import au.com.codeka.warworlds.common.proto.Account;

/** Wraps our reference to MapDB's data store objects. */
public class DataStore {
  public static final DataStore i = new DataStore();

  private final DB db;
  private final HTreeMap<String, Long> empireNames;
  private final HTreeMap<String, Account> accounts;

  private DataStore() {
    db = DBMaker
        .fileDB("data/store.db")
        .transactionEnable()
        .make();

    empireNames = db.hashMap("EmpireNames", Serializer.STRING, Serializer.LONG).createOrOpen();
    accounts = db.hashMap(
        "Accounts", Serializer.STRING, new ProtobufSerializer<Account>(Account.class)).createOrOpen();
  }

  /**
   * Gets the map of empire name to ID, useful to determine whether a name already exists or not.
   */
  public HTreeMap<String, Long> empireNames() {
    return empireNames;
  }

  public HTreeMap<String, Account> accounts() {
    return accounts;
  }
}
