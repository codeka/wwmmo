package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;

/**
 * Some helper methods for working with fleets.
 */
public class FleetHelper {
  /* Is the given {@link Fleet} friendly to the given {@link Empire}? */
  public static boolean isFriendly(Fleet fleet, Empire empire) {
    return isFriendly(fleet, empire.id);
  }

  /* Is the given {@link Fleet} friendly to the given {@link Empire}? */
  public static boolean isFriendly(Fleet fleet, Long empireId) {
    if (fleet.empire_id == null && empireId == null) {
      // Natives are friendly to each other.
      return true;
    }
    if (fleet.empire_id == null || empireId == null) {
      // Natives are not friendly to anybody else.
      return false;
    }
    // TODO: alliance?
    return fleet.empire_id.equals(empireId);
  }

  public static boolean isFriendly(Fleet fleet1, Fleet fleet2) {
    return isFriendly(fleet1, fleet2.empire_id);
  }
}
