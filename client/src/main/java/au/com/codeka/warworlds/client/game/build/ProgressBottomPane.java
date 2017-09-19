package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

/**
 * Bottom pane for an existing build. Shows the current progress, what we're blocked on (e.g. need
 * more minerals or population to go faster?) and allows you to cancel the build.
 */
public class ProgressBottomPane extends RelativeLayout {
  interface Callback {
    void onCancelBuild();
  }

  private final Star star;
  private final Colony colony;
  private final BuildRequest buildRequest;
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
    buildProgress.setProgress((int)(buildRequest.progress * 100));
  }
}
