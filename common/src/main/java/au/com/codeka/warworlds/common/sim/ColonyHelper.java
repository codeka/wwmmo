package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Planet;

/**
 * Helper for accessing information about a {@link Colony}.
 */
public class ColonyHelper {
  public static int getMaxPopulation(Planet planet) {
    if (planet.colony == null) {
      return 0;
    }

    int population = planet.population_congeniality;

    if (planet.colony.buildings != null) {
      for (Building building : planet.colony.buildings) {
        Design design = DesignHelper.getDesign(building.design_type);
        for (Design.Effect effect : design.effect) {
          if (effect.type == Design.EffectType.POPULATION_BOOST) {
            float extraPopulation = Math.min(effect.minimum, population * effect.bonus);
            population += extraPopulation;
          }
        }
      }
    }

    return population;
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
