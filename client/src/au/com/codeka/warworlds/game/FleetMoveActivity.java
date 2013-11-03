package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.starfield.BaseStarfieldActivity;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/** This activity is used to select a location for moves. It's a bit annoying that we have to do it like this... */
public class FleetMoveActivity extends BaseStarfieldActivity {
    private static final Logger log = LoggerFactory.getLogger(FleetMoveActivity.class);
    private Star mStar;
    private Fleet mFleet;

    public static void show(Activity activity, Fleet fleet) {
        Intent intent = new Intent(activity, FleetMoveActivity.class);
        Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder();
        fleet.toProtocolBuffer(fleet_pb);
        intent.putExtra("au.com.codeka.warworlds.Fleet", fleet_pb.build().toByteArray());
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        byte[] fleetBytes = extras.getByteArray("au.com.codeka.warworlds.Fleet");
        try {
            Messages.Fleet fleet_pb = Messages.Fleet.parseFrom(fleetBytes);
            mFleet = new Fleet();
            mFleet.fromProtocolBuffer(fleet_pb);
        } catch (InvalidProtocolBufferException e) {
        }

        // we can get an instance of the star from the sector manager
        mStar = SectorManager.getInstance().findStar(mFleet.getStarKey());
        mStarfield.scrollTo(mStar.getSectorX(), mStar.getSectorY(), mStar.getOffsetX(), mStar.getOffsetY());
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fleet_move;
    }
}
