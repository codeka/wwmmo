package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.StarModification;

/**
 * An exception that is thrown when a suspicious modification to a star happens.
 */
public class SuspiciousModificationException extends Exception {
  private final StarModification modification;

  public SuspiciousModificationException(
      StarModification modification,
      String fmt,
      Object... args) {
    super(String.format(fmt, args));
    this.modification = modification;
  }

  public StarModification getModification() {
    return modification;
  }
}
