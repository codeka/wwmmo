package au.com.codeka.warworlds.game.starfield;

import java.util.Locale;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

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
    }

    public void setFleet(final Fleet fleet) {
        mFleet = fleet;
        if (fleet == null) {
            return;
        }

        final ImageView fleetIcon = (ImageView) findViewById(R.id.fleet_icon);
        final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        final TextView fleetDesign = (TextView) findViewById(R.id.fleet_design);
        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        final TextView fleetDetails = (TextView) findViewById(R.id.fleet_details);

        empireName.setText("");
        empireIcon.setImageBitmap(null);

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        EmpireManager.i.fetchEmpire(fleet.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
            @Override
            public void onEmpireFetched(Empire empire) {
                if (mFleet == null || !mFleet.getKey().equals(fleet.getKey())) {
                    // if it's  not the same fleet as the one we're displaying, ignore it...
                    return;
                }
                empireName.setText(empire.getDisplayName());
                empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));
            }
        });

        fleetDesign.setText(design.getDisplayName());
        fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        String eta = "???";
        Star srcStar = SectorManager.getInstance().findStar(fleet.getStarKey());
        Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
        if (srcStar != null && destStar != null) {
            float timeRemainingInHours = fleet.getTimeToDestination(srcStar, destStar);
            eta = TimeInHours.format(timeRemainingInHours);
        }

        String details = String.format(Locale.ENGLISH,
            "<b>Ships:</b> %d<br />" +
            "<b>Speed:</b> %.2f pc/hr<br />" +
            "<b>Destination:</b> %s<br />" +
            "<b>ETA:</b> %s",
            (int) Math.ceil(fleet.getNumShips()), design.getSpeedInParsecPerHour(),
            (destStar == null ? "???" : destStar.getName()),
            eta);
        fleetDetails.setText(Html.fromHtml(details));
    }
}
