package au.com.codeka.warworlds.common.sim;

import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
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
      if (empireStorage.empire_id != null && empireStorage.empire_id.equals(empireId)) {
        return empireStorage;
      }
    }
    return null;
  }

  /** Gets the delta minerals per hour *at this time* for the given empty. */
  public static float getDeltaMineralsPerHour(Star star, long empireId, long now) {
    float delta = 0.0f;
    EmpireStorage storage = getStorage(star, empireId);
    if (storage != null) {
      delta += storage.minerals_delta_per_hour;
    }
    for (Planet planet : star.planets) {
      if (planet.colony != null
          && planet.colony.empire_id != null
          && planet.colony.empire_id.equals(empireId)) {
        for (BuildRequest br : planet.colony.build_requests) {
          if (br.start_time < now && br.end_time > now) {
            delta += br.delta_minerals_per_hour;
          }
        }
      }
    }
    return delta;
  }

  /**
   * Gets the index of the {@link EmpireStorage} for the empire with the given ID, or -1 if there's
   * no {@link EmpireStorage} for that empire.
   */
  public static int getStorageIndex(Star.Builder star, long empireId) {
    for (int i = 0; i < star.empire_stores.size(); i++) {
      if (star.empire_stores.get(i).empire_id != null
          && star.empire_stores.get(i).empire_id.equals(empireId)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Gets the coordinates string for the given star (something along the lines of "[1.23, 3.45]")
   */
  public static String getCoordinateString(Star star) {
    return String.format(Locale.US,
        "[%d.%02d, %d.%02d]",
        star.sector_x, Math.round(100 * star.offset_x / 1024.0f),
        star.sector_y, Math.round(100 * star.offset_y / 1024.0f));
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

  public static Vector2 directionBetween(Star.Builder from, Star to) {
    long sdx = to.sector_x - from.sector_x;
    long sdy = to.sector_y - from.sector_y;

    return new Vector2(
        to.offset_x - from.offset_x + (sdx * 1024.0f),
        to.offset_y - from.offset_y + (sdy * 1024.0f));
  }

  public static double distanceBetween(Star from, Star to) {
    return directionBetween(from, to).length();
  }

  public static double distanceBetween(Star.Builder from, Star to) {
    return directionBetween(from, to).length();
  }
}
