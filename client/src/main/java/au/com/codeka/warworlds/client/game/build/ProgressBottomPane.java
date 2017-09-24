package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.text.Html;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import java.util.Locale;

/**
 * Bottom pane for an existing build. Shows the current progress, what we're blocked on (e.g. need
 * more minerals or population to go faster?) and allows you to cancel the build.
 */
public class ProgressBottomPane extends RelativeLayout implements BottomPaneContentView {
  interface Callback {
    void onCancelBuild();
  }

  private Star star;
  private Colony colony;
  private BuildRequest buildRequest;
  private final Callback callback;

  private final ImageView buildIcon;
  private final TextView buildName;
  private final TextView timeRemaining;
  private final ProgressBar buildProgress;

  public ProgressBottomPane(
      Context context,
      Star star,
      Colony colony,
      BuildRequest buildRequest,
      Callback callback) {
    super(context);
    inflate(context, R.layout.build_progress_bottom_pane, this);

    this.star = star;
    this.colony = colony;
    this.buildRequest = buildRequest;
    this.callback = callback;

    buildIcon = findViewById(R.id.build_icon);
    buildName = findViewById(R.id.build_name);
    timeRemaining = findViewById(R.id.build_time_remaining);
    buildProgress = findViewById(R.id.build_progress);

    Design design = DesignHelper.getDesign(buildRequest.design_type);
    BuildHelper.setDesignIcon(design, buildIcon);
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
          this.star = star;
          this.colony = planet.colony;
          this.buildRequest = br;
          break;
        }
      }
    }

    update();
  }

  private void update() {
    buildProgress.setProgress((int)(buildRequest.progress * 100));

    String verb = "Building"; // (buildRequest.build_request_id == null ? "Building" : "Upgrading");
    timeRemaining.setText(Html.fromHtml(String.format(Locale.ENGLISH,
        "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
        verb, Math.round(buildRequest.progress * 100.0f),
        BuildHelper.formatTimeRemaining(buildRequest))));
  }
}
