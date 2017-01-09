package au.com.codeka.warworlds.common.sim;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Vector2;
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

  /**
   * Gets the index of the {@link EmpireStorage} for the empire with the given ID, or -1 if there's
   * no {@link EmpireStorage} for that empire.
   */
  public static int getStorageIndex(Star.Builder star, long empireId) {
    for (int i = 0; i < star.empire_stores.size(); i++) {
      if (star.empire_stores.get(i).empire_id.equals(empireId)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Calculates a {@link Vector2} that points from the "from" star to the "to" star.
   *
   * <p>For practical reasons, we assume that the given stars are not <em>too</em> far away (that
   * is, that the distance betwen them can be represented by a 32-bit floating point value).
   */
  public static Vector2 directionBetween(Star from, Star to) {
    long sdx = to.sector_x - from.sector_x;
    long sdy = to.sector_y - from.sector_y;

    return new Vector2(
        to.offset_x - from.offset_x + (sdx * 1024.0f),
        to.offset_y - from.offset_y + (sdy * 1024.0f));
  }

  public static Vector2 directionBetween(Star.Builder from, Star.Builder to) {
    long sdx = to.sector_x - from.sector_x;
    long sdy = to.sector_y - from.sector_y;

    return new Vector2(
        to.offset_x - from.offset_x + (sdx * 1024.0f),
        to.offset_y - from.offset_y + (sdy * 1024.0f));
  }

  public static double distanceBetween(Star from, Star to) {
    return directionBetween(from, to).length();
  }

  public static double distanceBetween(Star.Builder from, Star.Builder to) {
    return directionBetween(from, to).length();
  }
}
