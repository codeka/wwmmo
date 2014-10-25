package au.com.codeka.warworlds.server.data;

/**
 * An exception that is thrown when we have problems working out if the database schema is correct.
 * Usually this means your database credentials aren't specified correctly.
 */
public class SchemaException extends Exception {
  private static final long serialVersionUID = 1L;

  public SchemaException(String message) {
    super(message);
  }

  public SchemaException(String message, Throwable innerException) {
    super(message, innerException);
  }
}
