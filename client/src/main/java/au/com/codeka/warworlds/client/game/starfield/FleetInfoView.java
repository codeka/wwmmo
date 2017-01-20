package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildHelper;
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.TimeFormatter;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.StarHelper;

/** This view displays information about a fleet you've selected on the starfield view. */
public class FleetInfoView extends FrameLayout {
  private static final long REFRESH_DELAY_MS = 1000L;

  private final Handler handler = new Handler();
  private View view;
  private Star star;
  private Fleet fleet;

  public FleetInfoView(Context context) {
    this(context, null);
  }

  public FleetInfoView(Context context, AttributeSet attrs) {
    super(context, attrs);

    view = inflate(context, R.layout.starfield_fleet_info, null);
    addView(view);

    final Button boostBtn = (Button) view.findViewById(R.id.boost_btn);
    boostBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (fleet == null) {
          return;
        }

        //if (!fleet.hasUpgrade("boost")) {
       //   // toast?
        //  return;
       // }

       // FleetManager.i.boostFleet(fleet, new FleetManager.FleetBoostedHandler() {
       //   @Override
       //   public void onFleetBoosted(Fleet fleet) {
       //     // update happens automatically anyway when star refreshes
       ///   }
      //  });
      }
    });
  }

  public void setFleet(final Star star, final Fleet fleet) {
    this.fleet = fleet;
    this.star = star;
    if (fleet == null) {
      return;
    }

    final ImageView fleetIcon = (ImageView) findViewById(R.id.fleet_icon);
    final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
    final TextView fleetDesign = (TextView) findViewById(R.id.fleet_design);
    final TextView empireName = (TextView) findViewById(R.id.empire_name);
    final TextView fleetDestination = (TextView) findViewById(R.id.fleet_destination);
    final Button boostBtn = (Button) findViewById(R.id.boost_btn);

    empireName.setText("");
    empireIcon.setImageBitmap(null);

    Design design = DesignHelper.getDesign(fleet.design_type);
    Empire empire = EmpireManager.i.getEmpire(fleet.empire_id);
    if (empire != null) {
      empireName.setText(empire.display_name);
      Picasso.with(getContext())
          .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 20, 20))
          .into(empireIcon);
    }

    fleetDesign.setText(FleetListHelper.getFleetName(fleet, design/*, 18.0f*/));
    fleetDestination.setText(FleetListHelper.getFleetDestination(star, fleet, false));
    BuildHelper.setDesignIcon(design, fleetIcon);

    //FleetUpgrade.BoostFleetUpgrade fleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
    //if (fleetUpgrade != null && !fleetUpgrade.isBoosting()) {
    //  boostBtn.setEnabled(true);
    //} else {
      boostBtn.setEnabled(false);
   // }
    refreshFleet();
  }

  private void refreshFleet() {
    if (!isAttachedToWindow()) {
      return;
    }

    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    final TextView progressText = (TextView) findViewById(R.id.progress_text);

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
          TimeFormatter.create().format(eta - startTime));
      progressText.setText(Html.fromHtml(msg));
    }

    handler.postDelayed(this::refreshFleet, REFRESH_DELAY_MS);
  }
}
