package au.com.codeka.warworlds.client.game.build;

import android.graphics.Color;

import androidx.annotation.Nullable;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.BuildHelper;
import au.com.codeka.warworlds.common.sim.StarModifier;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;

public class BuildTimeCalculator {
  private static final Log log = new Log("BuildTimeCalculator");

  interface Callback {
    void onCalculated(String buildTime, String buildMinerals, int mineralsColor);
  }

  private final Star star;
  private final Colony colony;

  public BuildTimeCalculator(Star star, Colony colony) {
    this.star = star;
    this.colony = colony;
  }

  public void calculateBuildTime(final Design design, int count, Callback callback) {
    calculateTime(design, null, count, callback);
  }

  public void calculateUpgradeTime(final Design design, Building building, Callback callback) {
    calculateTime(design, building, 1, callback);
  }

  private void calculateTime(
      final Design design, @Nullable Building building, int count, Callback callback) {
    App.i.getTaskRunner().runTask(() -> {
      // Add the build request to a temporary copy of the star, simulate it and figure out the
      // build time.
      Star.Builder starBuilder = star.newBuilder();

      Empire myEmpire = EmpireManager.i.getMyEmpire();
      try {

        new StarModifier(() -> 0).modifyStar(starBuilder,
            new StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
                .empire_id(myEmpire.id)
                .colony_id(colony.id)
                .count(count)
                .building_id(building == null ? null : building.id)
                .design_type(design.type)
                .build());
      } catch (SuspiciousModificationException e) {
        log.error("Suspicious modification?", e);
        return;
      }
      // find the build request with ID 0, that's our guy

      Star updatedStar = starBuilder.build();
      for (BuildRequest buildRequest : BuildHelper.getBuildRequests(updatedStar)) {
        if (buildRequest.id == 0) {
          App.i.getTaskRunner().runTask(() -> {
            String buildTime = BuildHelper.formatTimeRemaining(buildRequest);
            String mineralsTime;
            int mineralsColor;
            EmpireStorage newEmpireStorage = BuildHelper.getEmpireStorage(updatedStar, myEmpire.id);
            EmpireStorage oldEmpireStorage = BuildHelper.getEmpireStorage(star, myEmpire.id);
            if (newEmpireStorage != null && oldEmpireStorage != null) {
              float mineralsDelta = newEmpireStorage.minerals_delta_per_hour
                  - oldEmpireStorage.minerals_delta_per_hour;
              mineralsTime = String.format(Locale.US, "%s%.1f/hr",
                  mineralsDelta < 0 ? "-" : "+", Math.abs(mineralsDelta));
              mineralsColor = mineralsDelta < 0 ? Color.RED : Color.GREEN;
            } else {
              mineralsTime = "";
              mineralsColor = Color.WHITE;
            }

            callback.onCalculated(buildTime, mineralsTime, mineralsColor);
          }, Threads.UI);
          break;
        }
      }
    }, Threads.BACKGROUND);
  }

}
