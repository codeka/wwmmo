package au.com.codeka.warworlds.server.store;

import java.io.File;

import au.com.codeka.warworlds.common.Log;

/** Wraps our references to the various data store objects. */
public class DataStore {
  private static final Log log = new Log("DataStore");
  public static final DataStore i = new DataStore();

  private final AccountsStore accounts = new AccountsStore("accounts.db");
  private final AdminUsersStore adminUsers = new AdminUsersStore("admin.db");
  private final ChatStore chat = new ChatStore("chat.db");
  private final EmpiresStore empires = new EmpiresStore("empires.db");
  private final SequenceStore seq = new SequenceStore("seq.db");
  private final SectorsStore sectors = new SectorsStore("sectors.db");
  private final StarsStore stars = new StarsStore("stars.db");
  private final StatsStore stats = new StatsStore("stats.db");
  private final SuspiciousEventStore suspiciousEvents = new SuspiciousEventStore("suss-events.db");

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
      accounts.open();
      adminUsers.open();
      chat.open();
      empires.open();
      seq.open();
      sectors.open();
      stars.open();
      stats.open();
      suspiciousEvents.open();
    } catch (StoreException e) {
      log.error("Error creating databases.", e);
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      suspiciousEvents.close();
      stats.close();
      stars.close();
      sectors.close();
      seq.close();
      empires.close();
      chat.close();
      adminUsers.close();
      accounts.close();
    } catch (StoreException e) {
      log.error("Error closing databases.", e);
    }
  }

  public AdminUsersStore adminUsers() {
    return adminUsers;
  }

  public AccountsStore accounts() {
    return accounts;
  }

  public ChatStore chat() {
    return chat;
  }

  public EmpiresStore empires() {
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

  public StatsStore stats() {
    return stats;
  }

  public SuspiciousEventStore suspiciousEvents() {
    return suspiciousEvents;
  }
}
