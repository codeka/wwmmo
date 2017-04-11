package au.com.codeka.warworlds.server.store;

import java.sql.SQLException;

/**
 * Exception that is thrown from the some of the store classes when an unrecoverable error occurs.
 */
public class StoreException extends Exception {
  /** Constructs a new {@link StoreException} that wraps the given {@link SQLException}. */
  public StoreException(SQLException e) {
    super(e.getMessage(), e);
  }

  /** Constructs a new {@link StoreException} with the given exception message. */
  public StoreException(String msg) {
    super(msg);
  }

  /** Constructs a new {@link StoreException} with the given exception. */
  public StoreException(Exception e) {
    super(e);
  }
}
