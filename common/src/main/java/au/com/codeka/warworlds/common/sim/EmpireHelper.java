package au.com.codeka.warworlds.common.sim;

import javax.annotation.Nullable;

/**
 * Helper for working with empires and empire IDs.
 */
public class EmpireHelper {
  public static boolean isSameEmpire(@Nullable Long lhs, @Nullable Long rhs) {
    if (lhs == null && rhs == null) {
      return true;
    }
    if (lhs == null || rhs == null) {
      return false;
    }

    return lhs.equals(rhs);
  }
}
