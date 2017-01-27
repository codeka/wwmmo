package au.com.codeka.warworlds.server.store;

import java.io.File;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;

/** Wraps our references to the various data store objects. */
public class DataStore {
  private static final Log log = new Log("DataStore");
  public static final DataStore i = new DataStore();

  private AccountsStore accounts;
  private ProtobufStore<Empire> empires;
  private SectorsStore sectors;
  private StarsStore stars;
  private AdminUsersStore adminUsers;
  private SequenceStore seq;

  private DataStore() {
  }

  public void open() {
    // Make sure we open the sqlite JDBC driver.
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Error loading SQLITE JDBC driver.", e);
    }

    File home = new File("data/store");
    if (!home.exists()) {
      if (!home.mkdirs()) {
        throw new RuntimeException("Error creating directories for data store.");
      }
    }

    try {
      adminUsers = new AdminUsersStore("admin.db");
      adminUsers.open();

      accounts = new AccountsStore("accounts.db");
      accounts.open();

      empires = new ProtobufStore<>("empires.db", Empire.class);
      empires.open();

      stars = new StarsStore("stars.db");
      stars.open();

      seq = new SequenceStore("seq.db");
      seq.open();

      sectors = new SectorsStore("sectors.db", stars);
      sectors.open();
    } catch (StoreException e) {
      log.error("Error creating databases.", e);
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      stars.close();
//      uniqueEmpireNames.close();
//      accounts.close();
//      empires.close();
    } catch (StoreException e) {
      log.error("Error closing databases.", e);
    }
  }

  public AccountsStore accounts() {
    return accounts;
  }

  public ProtobufStore<Empire> empires() {
    return empires;
  }

  public SequenceStore seq() {
    return seq;
  }

  public SectorsStore sectors() {
    return sectors;
  }

  public StarsStore stars() {
    return stars;
  }

  public AdminUsersStore adminUsers() {
    return adminUsers;
  }
}
