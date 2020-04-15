package au.com.codeka.warworlds.common.sim;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

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

  public static boolean isOwnedBy(Fleet fleet, Empire empire) {
    return isOwnedBy(fleet, empire == null ? null : empire.id);
  }

  public static boolean isOwnedBy(Fleet fleet, Long empireId) {
    if (fleet.empire_id == null && empireId == null) {
      return true;
    }
    if (fleet.empire_id == null || empireId == null) {
      return false;
    }
    return fleet.empire_id.equals(empireId);
  }

  @Nullable
  public static Fleet findFleet(Star star, long fleetId) {
    for (Fleet fleet : star.fleets) {
      if (fleet.id.equals(fleetId)) {
        return fleet;
      }
    }
    return null;
  }

  public static boolean hasEffect(Fleet fleet, Design.EffectType effectType) {
    Design design = DesignHelper.getDesign(fleet.design_type);
    for (Design.Effect effect : design.effect) {
      if (effect.type == effectType) {
        return true;
      }
    }
    return false;
  }
}
