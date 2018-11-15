package au.com.codeka.warworlds.server.store.base;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.store.StoreException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for {@link StoreReader} and {@link StoreWriter} that handling building the query.
 */
@SuppressWarnings("unchecked")
class StatementBuilder<T extends StatementBuilder> implements AutoCloseable {
  private static final Log log = new Log("StatementBuilder");

  @Nullable protected final Transaction transaction;
  protected PreparedStatement stmt;
  protected Connection conn;
  private String sql;
  private ArrayList<Object> params;

  /**
   * We store any errors we get building the statement and throw it when it comes time to execute.
   * In reality, it should be rare, since it would be an indication of programming error.
   */
  protected SQLException e;

  StatementBuilder(SQLiteConnectionPoolDataSource dataSource, @Nullable Transaction transaction) {
    this.transaction = transaction;

    try {
      if (transaction == null) {
        conn = dataSource.getPooledConnection().getConnection();
      } else {
        conn = transaction.getConnection();
      }
    } catch (SQLException e) {
      log.error("Unexpected.", e);
    }
  }

  public T stmt(String sql) {
    this.sql = checkNotNull(sql);
    try {
      stmt = conn.prepareStatement(sql);
      params = new ArrayList<>();
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
      saveParam(index, value);
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
      saveParam(index, value);
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
      saveParam(index, value);
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
      saveParam(index, value);
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
      saveParam(index, value);
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
      log.debug("%.2fms %s", (endTime - startTime) / 1000000.0, debugSql(sql, params));
    }
  }

  @Override
  public void close() throws StoreException {
    if (transaction != null) {
      try {
        conn.close();
      } catch(SQLException e) {
        throw new StoreException(e);
      }
    }
  }

  private void saveParam(int index, Object value) {
    while (params.size() <= index) {
      params.add(null);
    }
    params.set(index, value);
  }

  private static String debugSql(String sql, ArrayList<Object> params) {
    sql = sql.replace("\n", " ");
    sql = sql.replaceAll(" +", " ");
    if (sql.length() > 70) {
      sql = sql.substring(0, 68) + "...";
    }

    for (int i = 0; i < params.size(); i++) {
      sql += " ; ";
      sql += params.get(i);
    }

    return sql;
  }
}
