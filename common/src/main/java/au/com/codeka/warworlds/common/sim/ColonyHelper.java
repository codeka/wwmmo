package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Planet;

/**
 * Helper for accessing information about a {@link Colony}.
 */
public class ColonyHelper {
  public static int getMaxPopulation(Planet planet) {
    if (planet.colony == null) {
      return 0;
    }
    // TODO: apply boosts from buildings and stuff.
    return planet.population_congeniality;
  }
}
