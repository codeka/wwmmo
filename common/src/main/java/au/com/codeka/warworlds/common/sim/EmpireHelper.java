package au.com.codeka.warworlds.common.sim;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;

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

  /** Gets the {@link EmpireStorage.Builder} for the given empire. */
  @Nullable
  public static EmpireStorage.Builder getStore(Star.Builder star, @Nullable Long empireId) {
    int index = getStoreIndex(star, empireId);
    if (index < 0) {
      return null;
    }

    return star.empire_stores.get(index).newBuilder();
  }

  public static int getStoreIndex(Star.Builder star, @Nullable Long empireId) {
    for (int i = 0; i < star.empire_stores.size(); i++) {
      EmpireStorage store = star.empire_stores.get(i);
      if (store.empire_id == null && empireId == null) {
        return i;
      }
      if (store.empire_id != null && store.empire_id.equals(empireId)) {
        return i;
      }
    }

    return -1;
  }
}
