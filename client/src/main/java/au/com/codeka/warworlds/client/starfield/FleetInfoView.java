package au.com.codeka.warworlds.client.starfield;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.build.BuildHelper;
import au.com.codeka.warworlds.client.ctrl.FleetListHelper;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This view displays information about a fleet you've selected on the starfield view.
 */
public class FleetInfoView extends FrameLayout {
  private Context context;
  private View view;
  private Star star;
  private Fleet fleet;

  public FleetInfoView(Context context) {
    this(context, null);
  }

  public FleetInfoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;

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
    if (fleet == null) {
      return;
    }

    final ImageView fleetIcon = (ImageView) findViewById(R.id.fleet_icon);
    final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
    final LinearLayout fleetDesign = (LinearLayout) findViewById(R.id.fleet_design);
    final TextView empireName = (TextView) findViewById(R.id.empire_name);
    final LinearLayout fleetDestination = (LinearLayout) findViewById(R.id.fleet_destination);
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    final TextView progressText = (TextView) findViewById(R.id.progress_text);
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

    fleetDesign.removeAllViews();
    FleetListHelper.populateFleetNameRow(context, fleetDesign, fleet, design, 18.0f);
    BuildHelper.setDesignIcon(design, fleetIcon);

    fleetDestination.removeAllViews();
    FleetListHelper.populateFleetDestinationRow(context, fleetDestination, star, fleet, false);

    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (destStar != null) {
      float distanceInParsecs = 1.0f;//Sector.distanceInParsecs(srcStar, destStar);
      float timeFromSourceInHours = 1.0f;//fleet.getTimeFromSource();
      float timeToDestinationInHours = 1.0f;//fleet.getTimeToDestination();

      final float fractionRemaining =
          timeToDestinationInHours / (timeToDestinationInHours + timeFromSourceInHours);
      progressBar.setMax(1000);
      progressBar.setProgress(1000 - (int) (fractionRemaining * 1000.0f));

      String eta = String.format(Locale.ENGLISH, "<b>ETA</b>: %.1f pc in %s",
          distanceInParsecs * fractionRemaining,
          /*TimeFormatter.create().format(timeToDestinationInHours)*/ "1 hr");
      progressText.setText(Html.fromHtml(eta));
    }

    //FleetUpgrade.BoostFleetUpgrade fleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
    //if (fleetUpgrade != null && !fleetUpgrade.isBoosting()) {
    //  boostBtn.setEnabled(true);
    //} else {
      boostBtn.setEnabled(false);
   // }
  }
}
