package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;

public class SolarSystemFleetDialog extends Dialog {
    private static Logger log = LoggerFactory.getLogger(SolarSystemFleetDialog.class);
    private SolarSystemActivity mActivity;
    private Colony mColony;

    public SolarSystemFleetDialog(SolarSystemActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_fleet_dlg);
    }

    public void setColony(Colony colony) {
        mColony = colony;
    }
}
