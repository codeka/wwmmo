package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.starfield.BaseStarfieldActivity;
import au.com.codeka.warworlds.game.starfield.StarfieldSceneManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/** This activity is used to select a location for moves. It's a bit annoying that we have to do it like this... */
public class FleetMoveActivity extends BaseStarfieldActivity {
    private static final Logger log = LoggerFactory.getLogger(FleetMoveActivity.class);
    private Star mSrcStar;
    private Star mDestStar;
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
        mSrcStar = SectorManager.getInstance().findStar(mFleet.getStarKey());
        mStarfield.scrollTo(mSrcStar.getSectorX(), mSrcStar.getSectorY(), mSrcStar.getOffsetX(), mSrcStar.getOffsetY());

        mStarfield.addSelectionChangedListener(new StarfieldSceneManager.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                if (star == null) {
                    mDestStar = null;
                    refreshSelection();
                    return;
                }

                if (mDestStar == null || mDestStar.getKey().equals(star.getKey())) {
                    if (star.getKey().equals(mSrcStar.getKey())) {
                        // if src & dest are the same, just forget about it
                        mDestStar = null;
                        refreshSelection();
                        return;
                    }

                    mDestStar = star;
                    refreshSelection();
                    return;
                }
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
                // you can't select fleets
            }
        });

        Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDestStar != null) {
                    onMoveClick();
                }
                finish();
            }
        });
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fleet_move;
    }

    private void refreshSelection() {
        
    }

    private void onMoveClick() {
        if (mDestStar == null) {
            return;
        }

//        EmpireManager.i.getEmpire().addCash(-mEstimatedCost);

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           mFleet.getStarKey(),
                                           mFleet.getKey());
                Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                               .setOrder(Messages.FleetOrder.FLEET_ORDER.MOVE)
                               .setStarKey(mDestStar.getKey())
                               .build();
                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    StyledDialog dialog = new StyledDialog.Builder(FleetMoveActivity.this)
                                            .setMessage("Could not move the fleet: do you have enough cash?")
                                            .create();
                    dialog.show();
                    dialog.getPositiveButton().setEnabled(true);
                    dialog.getNegativeButton().setEnabled(true);
                } else {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.getInstance().refreshStar(mFleet.getStarKey());

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.i.refreshEmpire(mFleet.getEmpireKey());
                }
            }
        }.execute();
    }
}
