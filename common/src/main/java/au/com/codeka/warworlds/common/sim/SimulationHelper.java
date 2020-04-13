package au.com.codeka.warworlds.common.sim;

import static au.com.codeka.warworlds.common.sim.Simulation.STEP_TIME;

/**
 * Helper class used by the simulation and a few other bits and pieces.
 */
public class SimulationHelper {
  /** Trims a time to the step time. */
  public static long trimTimeToStep(long time) {
    return (time / STEP_TIME) * STEP_TIME;
  }

  /** Return true if the given value is invalid: NaN or infinite. */
  public static boolean isInvalid(float n) {
    return Float.isNaN(n) || Float.isInfinite(n);
  }
}
