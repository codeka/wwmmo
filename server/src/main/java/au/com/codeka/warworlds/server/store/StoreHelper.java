package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.DatabaseEntry;

/**
 * Some helper methods used by various bits of the store interface.
 */
public class StoreHelper {
  /** The first byte of a key will be KEY_MARKER if the key is for a 'normal' value. */
  public static final byte KEY_MARKER = 0;

  /** The first byte of the key will be SEQUENCE_MARKER if the key is for a sequence. */
  public static final byte SEQUENCE_MARKER = 1;

  /** Returns true if the given {@link DatabaseEntry} is a key. */
  public static boolean isKey(DatabaseEntry entry) {
    return entry.getData()[0] == KEY_MARKER;
  }
}
