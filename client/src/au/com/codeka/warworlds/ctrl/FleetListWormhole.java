package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Star;

/** This subclass of the fleet list control adds specific stuff for wormholes. */
public class FleetListWormhole extends FleetList {
    public FleetListWormhole(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.fleet_list_wormhole_ctrl);
    }

    public FleetListWormhole(Context context) {
        super(context, null, R.layout.fleet_list_wormhole_ctrl);
    }

    public void setWormhole(Star wormhole) {
        //mWormhole = wormhole;
    }

    @Override
    public void selectFleet(String fleetKey, boolean recentre) {
        super.selectFleet(fleetKey, recentre);
/*
        final Button enterBtn = (Button) findViewById(R.id.enter_btn);

        boolean tuned = false;
        if (mWormhole != null && mWormhole.getWormholeExtra() != null) {
            Star.WormholeExtra extra = mWormhole.getWormholeExtra();
            if (extra.getDestWormholeID() > 0 && extra.getTuneCompleteTime() != null &&
                    extra.getTuneCompleteTime().isBefore(DateTime.now())) {
                tuned = true;
            }
        }

        if (mSelectedFleet != null && tuned && mSelectedFleet.getState() == State.IDLE) {
            enterBtn.setEnabled(true);
        } else {
            enterBtn.setEnabled(false);
        }*/
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
/*
        final Button enterBtn = (Button) findViewById(R.id.enter_btn);
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                FleetManager.i.enterWormhole(mWormhole, mSelectedFleet, new FleetManager.FleetEnteredWormholeHandler() {
                    @Override
                    public void onFleetEnteredWormhole(Fleet fleet) {
                        //??
                    }
                });
            }
        });*/
    }
}
