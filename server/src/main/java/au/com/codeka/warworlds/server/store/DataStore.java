package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;

import java.io.File;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;

/** Wraps our reference to MapDB's data store objects. */
public class DataStore {
  private static final Log log = new Log("DataStore");
  public static final DataStore i = new DataStore();

  private Environment env;
  private AccountsStore accounts;
  private ProtobufStore<Empire> empires;
  private SectorsStore sectors;
  private ProtobufStore<Star> stars;
  private StarQueueSecondaryStore starsQueue;
  private StarEmpireSecondaryStore starEmpireSecondaryStore;
  private UniqueNameStore uniqueEmpireNames;

  private DataStore() {
    try {
      EnvironmentConfig envConfig = new EnvironmentConfig();
      envConfig.setAllowCreate(true);
      envConfig.setTransactional(true);
      env = new Environment(new File("data/store"), envConfig);

      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(true);
      dbConfig.setTransactional(true);

      Database db = env.openDatabase(null, "accounts", dbConfig);
      accounts = new AccountsStore(db);

      db = env.openDatabase(null, "empires", dbConfig);
      empires = new ProtobufStore<>(db, Empire.class);

      db = env.openDatabase(null, "stars", dbConfig);
      stars = new ProtobufStore<>(db, Star.class);
      starsQueue = new StarQueueSecondaryStore(env, db, stars);
      starEmpireSecondaryStore = new StarEmpireSecondaryStore(env, db, stars);

      db = env.openDatabase(null, "sectors", dbConfig);
      sectors = new SectorsStore(db, stars);

      db = env.openDatabase(null, "uniqueEmpireNames", dbConfig);
      uniqueEmpireNames = new UniqueNameStore(db);
    } catch (DatabaseException e) {
      log.error("Error creating databases.", e);
      throw new RuntimeException(e);
    }
  }

  public void close() {
    uniqueEmpireNames.close();
    accounts.close();
    empires.close();
    env.close();
  }

  public Transaction beginTransaction() {
    return env.beginTransaction(null, null);
  }

  public AccountsStore accounts() {
    return accounts;
  }

  public ProtobufStore<Empire> empires() {
    return empires;
  }

  public SectorsStore sectors() {
    return sectors;
  }

  public ProtobufStore<Star> stars() {
    return stars;
  }

  public StarQueueSecondaryStore starsQueue() {
    return starsQueue;
  }

  public StarEmpireSecondaryStore starEmpireSecondaryStore() {
    return starEmpireSecondaryStore;
  }

  public UniqueNameStore uniqueEmpireNames() {
    return uniqueEmpireNames;
  }
}
