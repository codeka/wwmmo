package au.com.codeka.warworlds.server.data;

/**
 * This is a special exception we can throw when we detect concurrent modification of, for example
 * a star or something. Our high-level code can retry the whole request on this condition.
 */
public class SqlConcurrentModificationException extends Exception {
  private static final long serialVersionUID = 1L;
}
