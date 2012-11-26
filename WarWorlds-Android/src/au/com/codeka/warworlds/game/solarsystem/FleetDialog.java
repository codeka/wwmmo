package au.com.codeka.warworlds.game.solarsystem;

import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.FleetMoveDialog;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class FleetDialog extends Dialog implements DialogManager.DialogConfigurable,
                                                   StarManager.StarFetchedHandler{
    public static final int ID = 1003;
    Context mContext;

    public FleetDialog(Activity activity) {
        super(activity);
        mContext = activity;
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
    public void onStop() {
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    @Override
    public void setBundle(final Activity activity, Bundle bundle) {
        final String starKey = bundle.getString("au.com.codeka.warworlds.StarKey");

        StarManager.getInstance().requestStar(mContext, starKey, false, this);
        StarManager.getInstance().addStarUpdatedListener(starKey, this);

        final FleetList fleetList = (FleetList) findViewById(R.id.fleet_list);
        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                // won't be called here...
            }

            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                Bundle args = new Bundle();
                args.putParcelable("au.com.codeka.warworlds.Fleet", fleet);
                DialogManager.getInstance().show(activity, FleetSplitDialog.class, args);
            }

            @Override
            public void onFleetMove(Star star, Fleet fleet) {
                Bundle args = new Bundle();
                args.putParcelable("au.com.codeka.warworlds.Fleet", fleet);
                DialogManager.getInstance().show(activity, FleetMoveDialog.class, args);
            }

            @Override
            public void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance) {
                EmpireManager.getInstance().getEmpire().updateFleetStance(mContext, star,
                                                                          fleet, newStance);
            }
        });
    }

    @Override
    public void onStarFetched(Star s) {
        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        stars.put(s.getKey(), s);

        final FleetList fleetList = (FleetList) findViewById(R.id.fleet_list);
        fleetList.refresh(s.getFleets(), stars);
    }
}
