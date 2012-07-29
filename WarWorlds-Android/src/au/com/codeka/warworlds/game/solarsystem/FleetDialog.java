package au.com.codeka.warworlds.game.solarsystem;

import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.FleetMoveDialog;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.UniverseElementActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;

public class FleetDialog extends Dialog implements DialogManager.DialogConfigurable {
    public static final int ID = 1003;

    public FleetDialog(Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_fleet_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);
    }

    @Override
    public void setBundle(final Activity activity, Bundle bundle) {
        Star star = bundle.getParcelable("au.com.codeka.warworlds.Star");
        refresh(activity, star);

        // when the star in the solar system activity changes, we want to refresh ourselves as well.
        ((SolarSystemActivity) activity).addUpdatedListener(new UniverseElementActivity.OnUpdatedListener() {
            @Override
            public void onStarUpdated(Star star, Planet selectedPlanet, Colony colony) {
                refresh(activity, star);
            }
            @Override
            public void onSectorUpdated() {
            }
        });

        final FleetList fleetList = (FleetList) findViewById(R.id.fleet_list);
        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                Bundle args = new Bundle();
                args.putParcelable("au.com.codeka.warworlds.Fleet", fleet);
                DialogManager.getInstance().show(activity, FleetSplitDialog.class, args,
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                ((SolarSystemActivity) activity).refresh();
                            }
                        });
            }

            @Override
            public void onFleetMove(Star star, Fleet fleet) {
                Bundle args = new Bundle();
                args.putString("au.com.codeka.warworlds.StarKey", fleet.getStarKey());
                args.putString("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                DialogManager.getInstance().show(activity, FleetMoveDialog.class, args);
            }
        });
    }

    private void refresh(Activity activity, Star star) {
        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        stars.put(star.getKey(), star);

        final FleetList fleetList = (FleetList) findViewById(R.id.fleet_list);
        fleetList.refresh(activity, star.getFleets(), stars);
    }
}
