package au.com.codeka.warworlds.server.store.base;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.annotation.Nullable;
import javax.sql.PooledConnection;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.store.StoreException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for {@link StoreReader} and {@link StoreWriter} that handling building the query.
 */
@SuppressWarnings("unchecked")
class StatementBuilder<T extends StatementBuilder> {
  private static final Log log = new Log("StatementBuilder");

  protected PreparedStatement stmt;
  protected PooledConnection pooledConnection;
  private String sql;

  /**
   * We store any errors we get building the statement and throw it when it comes time to execute.
   * In reality, it should be rare, since it would be an indication of programming error.
   */
  protected SQLException e;

  StatementBuilder(SQLiteConnectionPoolDataSource dataSource) {
    try {
      pooledConnection = dataSource.getPooledConnection();
    } catch (SQLException e) {
      log.error("Unexpected.", e);
    }
  }

  public T stmt(String sql) {
    this.sql = checkNotNull(sql);
    try {
      stmt = pooledConnection.getConnection().prepareStatement(sql);
    } catch (SQLException e) {
      log.error("Unexpected error preparing statement.", e);
      this.e = e;
    }
    return (T) this;
  }

  public T param(int index, @Nullable String value) {
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

  public T param(int index, @Nullable Double value) {
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

  public T param(int index, @Nullable Long value) {
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

  public T param(int index, @Nullable Integer value) {
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

  public T param(int index, @Nullable byte[] value) {
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

  public void execute() throws StoreException {
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
