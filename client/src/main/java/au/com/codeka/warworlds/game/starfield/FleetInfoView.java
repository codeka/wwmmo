package au.com.codeka.warworlds.game.starfield;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetListRow;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.FleetUpgrade;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/** This view displays information about a fleet you've selected on the starfield view. */
public class FleetInfoView extends FrameLayout {
    private Context mContext;
    private View mView;
    private Fleet mFleet;

    public FleetInfoView(Context context) {
        this(context, null);
    }

    public FleetInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mView = inflate(context, R.layout.starfield_fleet_info_ctrl, null);
        addView(mView);

        final Button boostBtn = (Button) mView.findViewById(R.id.boost_btn);
        boostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                if (!mFleet.hasUpgrade("boost")) {
                    // toast?
                    return;
                }

                FleetManager.i.boostFleet(mFleet, new FleetManager.FleetBoostedHandler() {
                    @Override
                    public void onFleetBoosted(Fleet fleet) {
                        // update happens automatically anyway when star refreshes
                    }
                });
            }
        });
    }

    public void setFleet(final Fleet fleet) {
        mFleet = fleet;
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

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        Empire empire = EmpireManager.i.getEmpire(fleet.getEmpireID());
        if (empire != null) {
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));
        }

        fleetDesign.removeAllViews();
        FleetListRow.populateFleetNameRow(mContext, fleetDesign, fleet, design, 18.0f);
        fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        fleetDestination.removeAllViews();
        FleetListRow.populateFleetDestinationRow(mContext, fleetDestination, fleet, false);

        ArrayList<Integer> starIDs = new ArrayList<Integer>();
        starIDs.add(Integer.parseInt(fleet.getStarKey()));
        starIDs.add(Integer.parseInt(fleet.getDestinationStarKey()));
        SparseArray<Star> starSummaries = StarManager.i.getStars(starIDs);
        Star srcStar = starSummaries.get(Integer.parseInt(fleet.getStarKey()));
        Star destStar = starSummaries.get(Integer.parseInt(fleet.getDestinationStarKey()));
        if (srcStar != null && destStar != null) {
            float distanceInParsecs = Sector.distanceInParsecs(srcStar, destStar);
            float timeFromSourceInHours = fleet.getTimeFromSource();
            float timeToDestinationInHours = fleet.getTimeToDestination();

            float fractionRemaining = timeToDestinationInHours / (timeToDestinationInHours + timeFromSourceInHours);
            progressBar.setMax(1000);
            progressBar.setProgress(1000 - (int) (fractionRemaining * 1000.0f));

            String eta = String.format(Locale.ENGLISH, "<b>ETA</b>: %.1f pc in %s",
                    distanceInParsecs * fractionRemaining,
                    TimeFormatter.create().format(timeToDestinationInHours));
            progressText.setText(Html.fromHtml(eta));
        }

        FleetUpgrade.BoostFleetUpgrade fleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
        if (fleetUpgrade != null && !fleetUpgrade.isBoosting()) {
            boostBtn.setEnabled(true);
        } else {
            boostBtn.setEnabled(false);
        }
    }
}
