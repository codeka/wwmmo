package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Locale;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildViewHelper;
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.TimeFormatter;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.sim.StarHelper;

/**
 * Bottom pane for when you have a fleet selected.
 */
public class FleetSelectedBottomPane extends FrameLayout {
  private static final long REFRESH_DELAY_MS = 1000L;

  private final Handler handler = new Handler();
  private Star star;
  private Fleet fleet;

  public FleetSelectedBottomPane(
      Context context,
      Star star,
      Fleet fleet) {
    super(context, null);

    inflate(context, R.layout.starfield_bottom_pane_fleet, this);

    this.fleet = fleet;
    this.star = star;

    final ImageView fleetIcon = findViewById(R.id.fleet_icon);
    final ImageView empireIcon = findViewById(R.id.empire_icon);
    final TextView fleetDesign = findViewById(R.id.fleet_design);
    final TextView empireName = findViewById(R.id.empire_name);
    final TextView fleetDestination = findViewById(R.id.fleet_destination);
    final Button boostBtn = findViewById(R.id.boost_btn);

    empireName.setText("");
    empireIcon.setImageBitmap(null);

    Design design = DesignHelper.getDesign(fleet.design_type);
    Empire empire = EmpireManager.i.getEmpire(fleet.empire_id);
    if (empire != null) {
      empireName.setText(empire.display_name);
      Picasso.get()
          .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 20, 20))
          .into(empireIcon);
    }

    fleetDesign.setText(FleetListHelper.getFleetName(fleet, design/*, 18.0f*/));
    fleetDestination.setText(FleetListHelper.getFleetDestination(star, fleet, false));
    BuildViewHelper.setDesignIcon(design, fleetIcon);

    //FleetUpgrade.BoostFleetUpgrade fleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
    //if (fleetUpgrade != null && !fleetUpgrade.isBoosting()) {
    //  boostBtn.setEnabled(true);
    //} else {
    boostBtn.setEnabled(false);
    // }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    refreshFleet();
  }

  private void refreshFleet() {
    if (!isAttachedToWindow()) {
      return;
    }

    final ProgressBar progressBar = findViewById(R.id.progress_bar);
    final TextView progressText = findViewById(R.id.progress_text);

    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (destStar != null) {
      double distanceInParsecs = StarHelper.distanceBetween(star, destStar);
      long startTime = fleet.state_start_time;
      long eta = fleet.eta;

      final float fractionRemaining =
          1.0f - (float) (System.currentTimeMillis() - startTime) / (float) (eta - startTime);
      progressBar.setMax(1000);
      progressBar.setProgress(1000 - (int) (fractionRemaining * 1000.0f));

      String msg = String.format(Locale.ENGLISH, "<b>ETA</b>: %.1f pc in %s",
          distanceInParsecs * fractionRemaining,
          TimeFormatter.create().format(eta - System.currentTimeMillis()));
      progressText.setText(Html.fromHtml(msg));
    }

    handler.postDelayed(this::refreshFleet, REFRESH_DELAY_MS);
  }
}
