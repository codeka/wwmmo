package au.com.codeka.warworlds.client.game.starfield;

import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Various helper methods for working in the starfield.
 */
public class StarfieldHelper {
  /**
   * Calcaulates a {@link Vector2} that points from the "from" star to the "to" star.
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

  public static double distanceBetween(Star from, Star to) {
    return directionBetween(from, to).length();
  }
}
