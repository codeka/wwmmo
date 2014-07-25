package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;

/** Customized {@link FleetSelectionPanel} for wormholes. */
public class FleetWormholeSelectionPanel extends FleetSelectionPanel {

    public FleetWormholeSelectionPanel(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.fleet_wormhole_selection_panel_ctrl);
        initialize(context);
    }

    public FleetWormholeSelectionPanel(Context context) {
        super(context, null, R.layout.fleet_wormhole_selection_panel_ctrl);
        initialize(context);
    }

    private void initialize(Context context) {
        final Button enterBtn = (Button) findViewById(R.id.enter_btn);
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                FleetManager.i.enterWormhole(mStar, mFleet,
                        new FleetManager.FleetEnteredWormholeHandler() {
                    @Override
                    public void onFleetEnteredWormhole(Fleet fleet) {
                        //??
                    }
                });
            }
        });
    }

}
