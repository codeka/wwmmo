package au.com.codeka.warworlds.common.sim

/**
 * Helper class used by the simulation and a few other bits and pieces.
 */
object SimulationHelper {
  /** Trims a time to the step time.  */
  fun trimTimeToStep(time: Long): Long {
    return time / Simulation.STEP_TIME * Simulation.STEP_TIME
  }

  /** Return true if the given value is invalid: NaN or infinite.  */
  fun isInvalid(n: Float): Boolean {
    return java.lang.Float.isNaN(n) || java.lang.Float.isInfinite(n)
  }
}