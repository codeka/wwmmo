package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.util.RuntimeExceptionWrapper;

import java.io.File;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;

/** Wraps our reference to MapDB's data store objects. */
public class DataStore {
  private static final Log log = new Log("DataStore");
  public static final DataStore i = new DataStore();

  private Environment env;
  private UniqueNameStore uniqueEmpireNames;
  private ProtobufStore<Empire> empires;
  private AccountsStore accounts;

  private DataStore() {
    try {
      EnvironmentConfig envConfig = new EnvironmentConfig();
      envConfig.setAllowCreate(true);
      envConfig.setTransactional(true);
      env = new Environment(new File("data/store"), envConfig);

      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(true);
      dbConfig.setTransactional(true);

      Database db = env.openDatabase(null, "empires", dbConfig);
      empires = new ProtobufStore<>(db, Empire.class);

      db = env.openDatabase(null, "empireNames", dbConfig);
      uniqueEmpireNames = new UniqueNameStore(db);

      db = env.openDatabase(null, "accounts", dbConfig);
      accounts = new AccountsStore(db);
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

  public UniqueNameStore uniqueEmpireNames() {
    return uniqueEmpireNames;
  }

  public ProtobufStore<Empire> empires() {
    return empires;
  }

  public AccountsStore accounts() {
    return accounts;
  }
}
