package au.com.codeka.warworlds.server.store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import au.com.codeka.warworlds.common.Log;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base storage object for {@link ProtobufStore} and some of the other stores.
 *
 * <p>Each {@link BaseStore} represents a single on-disk file, opening the store will load the
 * database, ensure that it's the correct version and upgrade the version if it's not. You can then
 * use {@link #newReader} and {@link #newWriter} for creating readers/writers into the data store.
 */
public abstract class BaseStore {
  private static final Log log = new Log("BaseStore");
  private final String fileName;

  private Connection conn;

  BaseStore(String fileName) {
    this.fileName = checkNotNull(fileName);
  }

  void open() throws StoreException {
    try {
      conn = DriverManager.getConnection("jdbc:sqlite:data/store/" + fileName);
    } catch(SQLException e) {
      throw new StoreException(e);
    }
    ensureVersion();
  }

  void close() throws StoreException {
    try {
      conn.close();
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  protected StoreReader newReader() {
    return new StoreReader();
  }

  protected StoreWriter newWriter() {
    return new StoreWriter();
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
    try (Statement stmt = conn.createStatement()) {
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
      try (Statement stmt = conn.createStatement()) {
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

  /** A helper class for reading from the data store. */
  protected class StoreReader {

  }

  /** A helper class for writing to the data store. */
  protected class StoreWriter {
    private PreparedStatement stmt;

    /**
     * We store any errors we get building the statement and throw it when it comes time to execute.
     * In reality, it should be rare, since it would be an indication of programming error. */
    private SQLException e;

    private StoreWriter() {
    }

    public StoreWriter stmt(String sql) {
      try {
        stmt = conn.prepareStatement(sql);
      } catch (SQLException e) {
        log.error("Unexpected error preparing statement.", e);
        this.e = e;
      }
      return this;
    }

    public StoreWriter param(int index, String value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        stmt.setString(index, value);
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return this;
    }

    public void execute() throws StoreException {
      if (e != null) {
        throw new StoreException(e);
      }

      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        stmt.execute();
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }
  }
}
