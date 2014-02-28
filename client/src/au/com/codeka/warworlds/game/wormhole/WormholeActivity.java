package au.com.codeka.warworlds.game.wormhole;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;
import org.joda.time.DateTime;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.ctrl.FleetListWormhole;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class WormholeActivity extends BaseGlActivity
                              implements StarManager.StarFetchedHandler {
    private WormholeSceneManager mWormhole;
    private Star mStar;
    private Star mDestStar;
    private DateTime mTuneCompleteTime;
    private Handler mHandler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        EmpireShieldManager.i.clearTextureCache();

        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
        mWormhole = new WormholeSceneManager(WormholeActivity.this, starKey);

        Button renameBtn = (Button) findViewById(R.id.rename_btn);
        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameDialog dialog = new RenameDialog();
                dialog.setWormhole(mStar);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button destinationBtn = (Button) findViewById(R.id.destination_btn);
        destinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DestinationDialog dialog = new DestinationDialog();
                dialog.loadWormholes(mStar);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button viewDestinationBtn = (Button) findViewById(R.id.view_destination_btn);
        viewDestinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDestStar == null) {
                    return;
                }

                Intent intent = new Intent(WormholeActivity.this, WormholeActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mDestStar.getKey());
                startActivity(intent);
            }
        });
        viewDestinationBtn.setEnabled(false);

        FleetListWormhole fleetList = (FleetListWormhole) findViewById(R.id.fleet_list);
        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                // won't be called here...
            }

            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                FragmentManager fm = getSupportFragmentManager();
                FleetSplitDialog dialog = new FleetSplitDialog();
                dialog.setFleet(fleet);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetBoost(Star star, Fleet fleet) {
                // won't be called here
            }

            @Override
            public void onFleetMove(Star star, Fleet fleet) {
                FleetMoveActivity.show(WormholeActivity.this, fleet);;
            }

            @Override
            public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
                FragmentManager fm = getSupportFragmentManager();
                FleetMergeDialog dialog = new FleetMergeDialog();
                dialog.setup(fleet, potentialFleets);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance) {
                EmpireManager.i.getEmpire().updateFleetStance(star, fleet, newStance);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

                StarManager.getInstance().requestStar(starKey, false, WormholeActivity.this);

                FleetListWormhole fleetList = (FleetListWormhole) findViewById(R.id.fleet_list);
                TreeMap<String, Star> stars = new TreeMap<String, Star>();
                stars.put(mStar.getKey(), mStar);
                fleetList.refresh(mStar.getFleets(), stars);
            }
        });
    }

    @Override
    public void onStarFetched(Star s) {
        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

        TextView starName  = (TextView) findViewById(R.id.star_name);
        TextView destinationName = (TextView) findViewById(R.id.destination_name);
        FleetListWormhole fleetList = (FleetListWormhole) findViewById(R.id.fleet_list);

        if (!s.getKey().equals(starKey)) {
            int starID = Integer.parseInt(s.getKey());
            if (mStar != null && mStar.getWormholeExtra().getDestWormholeID() == starID) {
                mDestStar = s;

                BaseStar.WormholeExtra wormholeExtra = mStar.getWormholeExtra();
                if (wormholeExtra.getTuneCompleteTime() != null &&
                        wormholeExtra.getTuneCompleteTime().isAfter(DateTime.now())) {
                    mTuneCompleteTime = wormholeExtra.getTuneCompleteTime();
                }

                String str = String.format(Locale.ENGLISH, "→ %s", s.getName());
                if (mTuneCompleteTime != null) {
                    str = "<font color=\"red\">" + str + "</font>";
                } else {
                    findViewById(R.id.view_destination_btn).setEnabled(true);
                }
                destinationName.setText(Html.fromHtml(str));
            }

            updateTuningProgress();
            return;
        }

        mStar = s;
        fleetList.setWormhole(mStar);

        if (mStar.getWormholeExtra().getDestWormholeID() != 0 && (
                mDestStar == null || !mDestStar.getKey().equals(Integer.toString(mStar.getWormholeExtra().getDestWormholeID())))) {
            StarManager.getInstance().requestStar(Integer.toString(
                    mStar.getWormholeExtra().getDestWormholeID()), false, this);
        }

        if (destinationName.getText().toString().equals("")) {
            destinationName.setText(Html.fromHtml("→ <i>None</i>"));
        }
        starName.setText(mStar.getName());
    }

    private void updateTuningProgress() {
        TextView tuningProgress = (TextView) findViewById(R.id.tuning_progress);
        if (mTuneCompleteTime == null) {
            tuningProgress.setText("");

            String str = String.format(Locale.ENGLISH, "→ %s", mDestStar.getName());
            TextView destinationName = (TextView) findViewById(R.id.destination_name);
            destinationName.setText(Html.fromHtml(str));
        } else {
            tuningProgress.setText(String.format(Locale.ENGLISH, "%s left",
                    TimeInHours.format(DateTime.now(), mTuneCompleteTime)));

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateTuningProgress();
                }
            }, 10000);
        }
    }

    /** Create the camera, we don't have a zoom factor. */
    @Override
    protected Camera createCamera() {
        ZoomCamera camera = new ZoomCamera(0, 0, mCameraWidth, mCameraHeight);

        return camera;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.wormhole;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.wormhole;
    }

    @Override
    protected void onCreateResources() throws IOException {
        mWormhole.onLoadResources();
    }

    @Override
    protected Scene onCreateScene() throws IOException {
        return mWormhole.createScene();
    }

    @Override
    public void onStart() {
        super.onStart();
        mWormhole.onStart();

        StarManager.getInstance().addStarUpdatedListener(null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mWormhole.onStop();

        StarManager.getInstance().removeStarUpdatedListener(this);
    }
}
