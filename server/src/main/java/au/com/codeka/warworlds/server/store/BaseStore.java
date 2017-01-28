package au.com.codeka.warworlds.server.store;

import org.sqlite.SQLiteConfig;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Locale;

import javax.annotation.Nullable;
import javax.sql.PooledConnection;

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

  private SQLiteConnectionPoolDataSource dataSource;

  BaseStore(String fileName) {
    this.fileName = checkNotNull(fileName);
  }

  void open() throws StoreException {
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

  void close() throws StoreException {
    // TODO: close? dataSource
  }

  StoreReader newReader() {
    return new StoreReader();
  }

  StoreWriter newWriter() {
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

  /**
   * Base class for {@link StoreReader} and {@link StoreWriter} that handling building the query.
   */
  class StatementBuilder<T extends StatementBuilder> {
    String sql;
    PreparedStatement stmt;
    PooledConnection pooledConnection;

    /**
     * We store any errors we get building the statement and throw it when it comes time to execute.
     * In reality, it should be rare, since it would be an indication of programming error.
     */
    protected SQLException e;

    private StatementBuilder() {
      try {
        pooledConnection = dataSource.getPooledConnection();
      } catch (SQLException e) {
        log.error("Unexpected.", e);
      }
    }

    @SuppressWarnings("unchecked")
    T stmt(String sql) {
      this.sql = checkNotNull(sql);
      try {
        stmt = pooledConnection.getConnection().prepareStatement(sql);
      } catch (SQLException e) {
        log.error("Unexpected error preparing statement.", e);
        this.e = e;
      }
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    T param(int index, @Nullable String value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        if (value == null) {
          stmt.setNull(index + 1, Types.VARCHAR);
        } else {
          stmt.setString(index + 1, value);
        }
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    T param(int index, @Nullable Double value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        if (value == null) {
          stmt.setNull(index + 1, Types.DOUBLE);
        } else {
          stmt.setDouble(index + 1, value);
        }
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    T param(int index, @Nullable Long value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        if (value == null) {
          stmt.setNull(index + 1, Types.INTEGER);
        } else {
          stmt.setLong(index + 1, value);
        }
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    T param(int index, @Nullable Integer value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        if (value == null) {
          stmt.setNull(index + 1, Types.INTEGER);
        } else {
          stmt.setInt(index + 1, value);
        }
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    T param(int index, @Nullable byte[] value) {
      checkNotNull(stmt, "stmt() must be called before param()");
      try {
        if (value == null) {
          stmt.setNull(index + 1, Types.BLOB);
        } else {
          stmt.setBytes(index + 1, value);
        }
      } catch (SQLException e) {
        log.error("Unexpected error setting parameter.", e);
        this.e = e;
      }
      return (T) this;
    }

    void execute() throws StoreException {
      if (e != null) {
        throw new StoreException(e);
      }

      checkNotNull(stmt, "stmt() must be called before param()");
      long startTime = System.nanoTime();
      try {
        stmt.execute();
      } catch (SQLException e) {
        throw new StoreException(e);
      } finally {
        long endTime = System.nanoTime();
        log.debug("%.2fms %s", (endTime - startTime) / 1000000.0, debugSql(sql));
      }
    }

    private String debugSql(String sql) {
      sql = sql.replace("\n", " ");
      sql = sql.replaceAll(" +", " ");
      if (sql.length() > 70) {
        sql = sql.substring(0, 68) + "...";
      }
      return sql;
    }
  }

  class QueryResult implements AutoCloseable {
    private final PooledConnection pooledConn;
    private final ResultSet rs;

    QueryResult(PooledConnection pooledConn, ResultSet rs) {
      this.pooledConn = pooledConn;
      this.rs = rs;
    }

    boolean next() throws StoreException {
      try {
        return rs.next();
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }

    int getInt(int columnIndex) throws StoreException {
      try {
        return rs.getInt(columnIndex + 1);
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }

    long getLong(int columnIndex) throws StoreException {
      try {
        return rs.getLong(columnIndex + 1);
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }

    byte[] getBytes(int columnIndex) throws StoreException {
      try {
        return rs.getBytes(columnIndex + 1);
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }

    @Override
    public void close() throws Exception {
      rs.close();
      pooledConn.close();
    }
  }

  /** A helper class for reading from the data store. */
  class StoreReader extends StatementBuilder<StoreReader> {
    QueryResult query() throws StoreException {
      execute();
      try {
        return new QueryResult(pooledConnection, stmt.getResultSet());
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }
  }

  /** A helper class for writing to the data store. */
  class StoreWriter extends StatementBuilder<StoreWriter> {
    @Override
    public void execute() throws StoreException {
      super.execute();

      try {
        pooledConnection.close();
      } catch (SQLException e) {
        throw new StoreException(e);
      }
    }
  }
}
