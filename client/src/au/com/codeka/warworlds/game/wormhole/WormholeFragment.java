package au.com.codeka.warworlds.game.wormhole;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;
import org.joda.time.DateTime;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseGlFragment;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.ctrl.FleetListWormhole;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/** This fragment is used in place of SolarSystemFragment in the SolarSystemActivity for
    wormholes. */
public class WormholeFragment extends BaseGlFragment {
    private WormholeSceneManager mWormhole;
    private Star star;
    private Star destStar;
    private DateTime tuneCompleteTime;
    private Handler handler;
    private View contentView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        EmpireShieldManager.i.clearTextureCache();
        StarManager.eventBus.register(eventHandler);

        Bundle extras = getArguments();
        int starID = (int) extras.getLong("au.com.codeka.warworlds.StarID");
        mWormhole = new WormholeSceneManager(WormholeFragment.this, starID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = super.onCreateView(inflater, container, savedInstanceState);

        Button renameBtn = (Button) contentView.findViewById(R.id.rename_btn);
        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameDialog dialog = new RenameDialog();
                dialog.setWormhole(star);
                dialog.show(getFragmentManager(), "");
            }
        });

        Button destinationBtn = (Button) contentView.findViewById(R.id.destination_btn);
        destinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DestinationDialog dialog = new DestinationDialog();
                dialog.loadWormholes(star);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }
        });

        Button viewDestinationBtn = (Button) contentView.findViewById(R.id.view_destination_btn);
        viewDestinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (destStar == null) {
                    return;
                }

                // This cast isn't great...
                ((SolarSystemActivity) getActivity()).showStar(destStar.getID());
            }
        });
        viewDestinationBtn.setEnabled(false);

        FleetListWormhole fleetList = (FleetListWormhole) contentView.findViewById(R.id.fleet_list);
        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                // won't be called here...
            }

            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
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
                FleetMoveActivity.show(getActivity(), fleet);;
            }

            @Override
            public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FleetMergeDialog dialog = new FleetMergeDialog();
                dialog.setup(fleet, potentialFleets);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance) {
                EmpireManager.i.getEmpire().updateFleetStance(star, fleet, newStance);
            }
        });

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(getActivity(), new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getArguments();
                int starID = (int) extras.getLong("au.com.codeka.warworlds.StarID");

                star = StarManager.i.getStar(starID);
                if (star != null) {
                    refreshStar();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StarManager.eventBus.unregister(eventHandler);
    }

    private Object eventHandler = new Object() {
        @EventHandler
        public void onStarFetched(Star s) {
            Bundle extras = getArguments();
            int starID = (int) extras.getLong("au.com.codeka.warworlds.StarID");

            if (s.getID() == starID) {
                star = s;
                refreshStar();
            } else if (star != null && star.getWormholeExtra().getDestWormholeID() == s.getID()) {
                destStar = s;
                refreshStar();
            }
        }
    };

    private void refreshStar() {
        if (star.getWormholeExtra().getDestWormholeID() != 0 && (
                destStar == null || !destStar.getKey().equals(Integer.toString(star.getWormholeExtra().getDestWormholeID())))) {
            destStar = StarManager.i.getStar(star.getWormholeExtra().getDestWormholeID());
        }

        TextView starName  = (TextView) contentView.findViewById(R.id.star_name);
        TextView destinationName = (TextView) contentView.findViewById(R.id.destination_name);
        FleetListWormhole fleetList = (FleetListWormhole) contentView.findViewById(R.id.fleet_list);

        if (destinationName.getText().toString().equals("")) {
            destinationName.setText(Html.fromHtml("→ <i>None</i>"));
        }
        starName.setText(star.getName());

        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        stars.put(star.getKey(), star);
        fleetList.refresh(star.getFleets(), stars);

        if (destStar != null) {
            BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
            if (wormholeExtra.getTuneCompleteTime() != null &&
                    wormholeExtra.getTuneCompleteTime().isAfter(DateTime.now())) {
                tuneCompleteTime = wormholeExtra.getTuneCompleteTime();
            }

            String str = String.format(Locale.ENGLISH, "→ %s", destStar.getName());
            if (tuneCompleteTime != null) {
                str = "<font color=\"red\">" + str + "</font>";
            } else {
                contentView.findViewById(R.id.view_destination_btn).setEnabled(true);
            }
            destinationName.setText(Html.fromHtml(str));

            updateTuningProgress();
        }
    }

    private void updateTuningProgress() {
        TextView tuningProgress = (TextView) contentView.findViewById(R.id.tuning_progress);
        if (tuneCompleteTime == null && destStar != null) {
            tuningProgress.setText("");

            String str = String.format(Locale.ENGLISH, "→ %s", destStar.getName());
            TextView destinationName = (TextView) contentView.findViewById(R.id.destination_name);
            destinationName.setText(Html.fromHtml(str));
        } else if (tuneCompleteTime != null) {
            tuningProgress.setText(String.format(Locale.ENGLISH, "%s left",
                    TimeFormatter.create().format(DateTime.now(), tuneCompleteTime)));

            handler.postDelayed(new Runnable() {
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
    }

    @Override
    public void onStop() {
        super.onStop();
        mWormhole.onStop();
    }
}
