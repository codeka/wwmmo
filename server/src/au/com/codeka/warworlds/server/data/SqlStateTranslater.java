package au.com.codeka.warworlds.server.data;

import java.sql.SQLException;

/**
 * Helper class for translating postgresql error states into usable codes.
 * <p>
 * The errors that postgresql can report are documented here:
 * http://www.postgresql.org/docs/current/static/errcodes-appendix.html
 */
public class SqlStateTranslater {
  public static ErrorCode translate(SQLException e) {
    return translate(e.getSQLState());
  }

  public static ErrorCode translate(String sqlState) {
    for (ErrorCode errorCode : ErrorCode.values()) {
      if (errorCode.isError(sqlState)) {
        return errorCode;
      }
    }
    return ErrorCode.UnknownError;
  }

  public static boolean isConstraintViolation(SQLException e) {
    return isConstraintViolation(e.getSQLState());
  }

  public static boolean isConstraintViolation(String sqlState) {
    return translate(sqlState) == ErrorCode.ConstraintViolation;
  }

  public static boolean isRetryable(SQLException e) {
    return isRetryable(e.getSQLState());
  }

  public static boolean isRetryable(String sqlState) {
    ErrorCode errorCode = translate(sqlState);
    // TODO: there may be more conditions here where we can retry.
    return errorCode == ErrorCode.TransactionRollback;
  }

  /**
   * These are the error codes we actually support. There's many more, but we just add the ones
   * we actually care about here.
   */
  public enum ErrorCode {
    UnknownError(""),
    TableDoesNotExist("42P01"),
    TransactionRollback("40*"),
    ConstraintViolation("23*");

    private String sqlState;

    private ErrorCode(String sqlState) {
      this.sqlState = sqlState;
    }

    public boolean isError(String sqlState) {
      if (this.sqlState.endsWith("*")) {
        return sqlState.startsWith(this.sqlState.substring(0, this.sqlState.length() - 1));
      }
      return this.sqlState.equals(sqlState);
    }
  }
}
