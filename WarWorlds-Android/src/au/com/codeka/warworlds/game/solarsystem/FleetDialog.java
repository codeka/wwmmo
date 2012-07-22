package au.com.codeka.warworlds.game.solarsystem;

import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.UniverseElementDialog;
import au.com.codeka.warworlds.model.Star;

public class FleetDialog extends UniverseElementDialog {
    private SolarSystemActivity mActivity;

    public static final int ID = 1003;

    public FleetDialog(SolarSystemActivity activity) {
        super(activity);
        mActivity = activity;
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

    public void setStar(Star star) {
        FleetList fleetList = (FleetList) findViewById(R.id.fleet_list);
        fleetList.refresh(mActivity, star, star.getFleets());
    }
}
