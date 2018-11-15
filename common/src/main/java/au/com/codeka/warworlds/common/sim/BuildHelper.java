package au.com.codeka.warworlds.common.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

import static au.com.codeka.warworlds.common.sim.Simulation.STEP_TIME;
import static au.com.codeka.warworlds.common.sim.SimulationHelper.trimTimeToStep;

public class BuildHelper {

  public static List<BuildRequest> getBuildRequests(Star star) {
    ArrayList<BuildRequest> buildRequests = new ArrayList<>();
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.build_requests != null) {
        buildRequests.addAll(planet.colony.build_requests);
      }
    }
    return buildRequests;
  }

  public static String formatTimeRemaining(BuildRequest buildRequest) {
    float hours = (float)(buildRequest.end_time - System.currentTimeMillis()) / Time.HOUR;
    float minutes = hours * 60.0f;
    if (minutes < 10.0f) {
      return String.format(Locale.US, "%d mins %02d sec",
          (int) Math.floor(minutes), (int) Math.ceil((minutes - Math.floor(minutes)) * 60.0f));
    } else if (minutes < 60.0f) {
      return String.format(Locale.US, "%d mins", Math.round(minutes));
    } else if (hours < 10.0f) {
      return String.format(Locale.US, "%.0f hrs %.0f mins", Math.floor(hours), minutes - (Math.floor(hours) * 60.0f));
    } else {
      return String.format(Locale.US, "%d hrs", Math.round(hours));
    }
  }

  /** Get the exact fraction that the given build has completed based on the time "now". */
  public static float getBuildProgress(BuildRequest buildRequest, long now) {
    // Update the progress percentages for partial steps while we're here.
    long brStartTime = trimTimeToStep(now);
    long brEndTime = brStartTime + STEP_TIME;
    if (buildRequest.start_time > brStartTime) {
      brStartTime = buildRequest.start_time;
    }
    if (now > brStartTime && now <= brEndTime) {
      float stepFraction = (float)(now - brStartTime) / ((float) STEP_TIME);
      return Math.min(1.0f, buildRequest.progress + stepFraction * buildRequest.progress_per_step);
    }

    return buildRequest.progress;
  }

  @Nullable
  public static EmpireStorage getEmpireStorage(Star star, long empireId) {
    for (EmpireStorage storage : star.empire_stores) {
      if (storage.empire_id != null && storage.empire_id.equals(empireId)) {
        return storage;
      }
    }
    return null;
  }
}
