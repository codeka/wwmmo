package au.com.codeka.warworlds.server.store.base;

import org.sqlite.SQLiteConfig;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import javax.sql.PooledConnection;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.store.StoreException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base storage object for interfacing with the data store.
 *
 * <p>Each {@link BaseStore} represents a single on-disk file, opening the store will load the
 * database, ensure that it's the correct version and upgrade the version if it's not. You can then
 * use {@link #newReader} and {@link #newWriter} for creating readers/writers into the data store.
 */
public abstract class BaseStore {
  private static final Log log = new Log("BaseStore");
  private final String fileName;

  private SQLiteConnectionPoolDataSource dataSource;

  protected BaseStore(String fileName) {
    this.fileName = checkNotNull(fileName);
  }

  public void open() throws StoreException {
    try {
      SQLiteConfig config = new SQLiteConfig();

      // Disable fsync calls, trusting that the filesystem will do the right thing. It's not always
      // the best assumption, but we are file with losing ~1 day of data (basically, the time
      // between backups). Additionally, switch to write-ahead-logging for the journal. We could
      // turn it off completely as well, and rely on backups in the event of data loss, but that's
      // slightly more painful for development (where "crashes" are more likely).
      config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
      config.setJournalMode(SQLiteConfig.JournalMode.WAL);

      // Increase the cache size, we have plenty of memory on the server.
      config.setCacheSize(2048);

      // Enforce foreign key constraints (for some reason, the default is off)
      config.enforceForeignKeys(true);

      dataSource = new SQLiteConnectionPoolDataSource(config);
      dataSource.getPooledConnection();
      dataSource.setUrl("jdbc:sqlite:data/store/" + fileName);
    } catch(SQLException e) {
      throw new StoreException(e);
    }
    ensureVersion();
  }

  public void close() throws StoreException {
    // TODO: close? dataSource
  }

  protected StoreReader newReader() {
    return new StoreReader(dataSource);
  }

  protected StoreWriter newWriter() {
    return new StoreWriter(dataSource);
  }

  /**
   * Called when the database is opened. Sub classes should implement this to perform any on-disk
   * upgrades required to get the database up to the current version.
   *
   * @param diskVersion The version of the database on-disk. A value of 0 indicates that the data
   *                    is brand-new on disk and all data structures should be created.
   * @return The new version to return as the on-disk version. Must be > 0 and >= the value passed
   *         in as diskVersion.
   */
  protected abstract int onOpen(int diskVersion) throws StoreException;

  /** Check that the version of the database on disk is the same as the version we expect. */
  private void ensureVersion() throws StoreException {
    int currVersion = 0;
    PooledConnection conn = null;
    try {
      conn = dataSource.getPooledConnection();
    } catch (SQLException e) {
      return;
    }
    try (Statement stmt = conn.getConnection().createStatement()) {
      String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='version'";
      try (ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
          try (ResultSet rs2 = stmt.executeQuery("SELECT val FROM version")) {
            if (rs2.next()) {
              currVersion = rs2.getInt(1);
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new StoreException(e);
    }

    log.debug("%s version: %d", fileName, currVersion);
    int newVersion = onOpen(currVersion);
    if (newVersion == 0 || newVersion < currVersion) {
      throw new StoreException(String.format(Locale.US,
          "Version returned from onOpen must be > 0 and >= the previous value. newVersion=%d, currVersion=%d",
          newVersion, currVersion));
    }

    // Store the new version on disk as well
    if (newVersion != currVersion) {
      try (Statement stmt = conn.getConnection().createStatement()) {
        if (currVersion == 0) {
          stmt.executeUpdate("CREATE TABLE version (val INTEGER)");
          stmt.executeUpdate("INSERT INTO version (val) VALUES (0)");
        }
        stmt.executeUpdate(String.format(Locale.US, "UPDATE version SET val=%d", newVersion));
        log.debug("%s new version: %d", fileName, newVersion);
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }
  }
}
