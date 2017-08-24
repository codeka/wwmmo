package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.StarModification;

/**
 * An exception that is thrown when a suspicious modification to a star happens.
 */
public class SuspiciousModificationException extends Exception {
  private final long starId;
  private final StarModification modification;

  public SuspiciousModificationException(
      long starId,
      StarModification modification,
      String fmt,
      Object... args) {
    super(String.format(fmt, args));
    this.starId = starId;
    this.modification = modification;
  }

  public long getStarId() {
    return starId;
  }

  public StarModification getModification() {
    return modification;
  }
}
