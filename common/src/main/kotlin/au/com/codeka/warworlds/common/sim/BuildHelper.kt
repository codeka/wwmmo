package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.proto.BuildRequest
import au.com.codeka.warworlds.common.proto.EmpireStorage
import au.com.codeka.warworlds.common.proto.Star
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object BuildHelper {
  fun getBuildRequests(star: Star): List<BuildRequest> {
    val buildRequests: ArrayList<BuildRequest> = ArrayList<BuildRequest>()
    for (planet in star.planets) {
      if (planet.colony?.build_requests != null) {
        buildRequests.addAll(planet.colony.build_requests)
      }
    }
    return buildRequests
  }

  fun getBuildRequests(star: MutableStar): List<MutableBuildRequest> {
    val buildRequests = ArrayList<MutableBuildRequest>()
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      buildRequests.addAll(colony.buildRequests)
    }
    return buildRequests
  }

  fun formatTimeRemaining(buildRequest: BuildRequest): String {
    val hours = (buildRequest.end_time!! - System.currentTimeMillis()).toFloat() / Time.HOUR
    val minutes = hours * 60.0f
    return when {
      minutes < 10.0f -> {
        String.format(Locale.US, "%d mins %02d sec",
            floor(minutes.toDouble()).toInt(),
            ceil((minutes - floor(minutes.toDouble())) * 60.0f).toInt())
      }
      minutes < 60.0f -> {
        String.format(Locale.US, "%d mins", minutes.roundToInt())
      }
      hours < 10.0f -> {
        String.format(Locale.US, "%.0f hrs %.0f mins",
            floor(hours.toDouble()), minutes - floor(hours.toDouble()) * 60.0f)
      }
      else -> {
        String.format(Locale.US, "%d hrs", hours.roundToInt())
      }
    }
  }

  /** Get the exact fraction that the given build has completed based on the time "now".  */
  fun getBuildProgress(buildRequest: BuildRequest, now: Long): Float {
    // Update the progress percentages for partial steps while we're here.
    var brStartTime = SimulationHelper.trimTimeToStep(now)
    val brEndTime = brStartTime + Simulation.STEP_TIME
    if (buildRequest.start_time!! > brStartTime) {
      brStartTime = buildRequest.start_time
    }
    if (now in (brStartTime + 1)..brEndTime) {
      val stepFraction = (now - brStartTime).toFloat() / Simulation.STEP_TIME.toFloat()
      return 1.0f.coerceAtMost(buildRequest.progress!! + stepFraction * buildRequest.progress_per_step!!)
    }
    return buildRequest.progress!!
  }

  fun getEmpireStorage(star: Star, empireId: Long): EmpireStorage? {
    for (storage in star.empire_stores) {
      if (storage.empire_id == empireId) {
        return storage
      }
    }
    return null
  }
}