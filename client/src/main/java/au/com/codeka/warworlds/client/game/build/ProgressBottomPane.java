package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.BuildHelper;
import au.com.codeka.warworlds.common.sim.DesignHelper;

/**
 * Bottom pane for an existing build. Shows the current progress, what we're blocked on (e.g. need
 * more minerals or population to go faster?) and allows you to cancel the build.
 */
public class ProgressBottomPane extends RelativeLayout implements BottomPaneContentView {
  interface Callback {
    void onCancelBuild();
  }

  private BuildRequest buildRequest;

  private final TextView timeRemaining;
  private final ProgressBar buildProgress;
  private final ProgressBar populationEfficiency;
  private final ProgressBar miningEfficiency;

  public ProgressBottomPane(
      Context context,
      BuildRequest buildRequest,
      Callback callback) {
    super(context);
    inflate(context, R.layout.build_progress_bottom_pane, this);

    this.buildRequest = buildRequest;

    ImageView buildIcon = findViewById(R.id.build_icon);
    TextView buildName = findViewById(R.id.build_name);
    timeRemaining = findViewById(R.id.build_time_remaining);
    buildProgress = findViewById(R.id.build_progress);
    populationEfficiency = findViewById(R.id.population_efficiency);
    miningEfficiency = findViewById(R.id.mining_efficiency);
    findViewById(R.id.cancel).setOnClickListener((View view) -> callback.onCancelBuild());

    Design design = DesignHelper.getDesign(buildRequest.design_type);
    BuildViewHelper.setDesignIcon(design, buildIcon);
    buildName.setText(DesignHelper.getDesignName(design, false));

    update();
  }

  @Override
  public void refresh(Star star) {
    // Get an updated build request
    for (Planet planet : star.planets) {
      if (planet.colony == null) {
        continue;
      }
      for (BuildRequest br : planet.colony.build_requests) {
        if (br.id.equals(buildRequest.id)) {
          buildRequest = br;
          break;
        }
      }
    }

    update();
  }

  private void update() {
    int progress =
        Math.round(BuildHelper.getBuildProgress(buildRequest, System.currentTimeMillis()) * 100);
    buildProgress.setProgress(progress);

    // These could be null if the star hasn't been simulated recently.
    if (buildRequest.population_efficiency != null) {
      populationEfficiency.setProgress(Math.round(buildRequest.population_efficiency * 100));
    }
    if (buildRequest.minerals_efficiency != null) {
      miningEfficiency.setProgress(Math.round(buildRequest.minerals_efficiency * 100));
    }

    String verb = "Building"; // (buildRequest.build_request_id == null ? "Building" : "Upgrading");
    timeRemaining.setText(Html.fromHtml(String.format(Locale.ENGLISH,
        "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
        verb, progress,
        BuildHelper.formatTimeRemaining(buildRequest))));
  }
}
