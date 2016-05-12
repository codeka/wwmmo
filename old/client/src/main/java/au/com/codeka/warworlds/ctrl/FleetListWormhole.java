package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import au.com.codeka.warworlds.R;

/** This subclass of the fleet list control adds specific stuff for wormholes. */
public class FleetListWormhole extends FleetList {
    public FleetListWormhole(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.fleet_list_wormhole_ctrl);
    }

    public FleetListWormhole(Context context) {
        super(context, null, R.layout.fleet_list_wormhole_ctrl);
    }
}
