package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.BuildRequest;
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

  public static float getDeltaMineralsPerHour(Colony colony, long now) {
    float delta = colony.delta_minerals;
    for (BuildRequest br : colony.build_requests) {
      if (br.start_time < now && br.end_time > now) {
        delta += br.delta_minerals_per_hour;
      }
    }
    return delta;
  }
}
