package au.com.codeka.warworlds.server.store;

import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCharArray;

import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;

/** Wraps our reference to MapDB's data store objects. */
public class DataStore {
  public static final DataStore i = new DataStore();

  private final DB db;
  private final HTreeMap<String, Long> empireNames;
  private final HTreeMap<Long, Empire> empires;
  private final HTreeMap<String, Account> accounts;

  private DataStore() {
    db = DBMaker
        .fileDB("data/store.db")
        .transactionEnable()
        .closeOnJvmShutdown()
        .make();

    empireNames = db.hashMap("EmpireNames", Serializer.STRING, Serializer.LONG).createOrOpen();
    empires = db.hashMap(
        "Empires", Serializer.LONG, new ProtobufSerializer<>(Empire.class)).createOrOpen();
    accounts = db.hashMap(
        "Accounts", Serializer.STRING, new ProtobufSerializer<>(Account.class)).createOrOpen();
  }

  public Atomic.Long idGenerator(String name) {
    return db.atomicLong(name).createOrOpen();
  }

  public HTreeMap<String, Long> empireNames() {
    return empireNames;
  }

  public HTreeMap<Long, Empire> empires() {
    return empires;
  }

  public HTreeMap<String, Account> accounts() {
    return accounts;
  }
}
