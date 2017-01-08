package au.com.codeka.warworlds.common.sim;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A helper for working with {@link Star}s.
 */
public class StarHelper {
  /**
   * Gets the {@link EmpireStorage} for the empire with the given ID.
   */
  @Nullable
  public static EmpireStorage getStorage(Star star, long empireId) {
    for (EmpireStorage empireStorage : star.empire_stores) {
      if (empireStorage.empire_id.equals(empireId)) {
        return empireStorage;
      }
    }
    return null;
  }
}
