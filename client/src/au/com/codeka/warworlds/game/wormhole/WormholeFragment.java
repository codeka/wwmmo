package au.com.codeka.warworlds.game.wormhole;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;
import org.joda.time.DateTime;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseGlFragment;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.ctrl.FleetListWormhole;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.designeffects.WormholeDisruptorBuildingEffect;

/**
 * This fragment is used in place of SolarSystemFragment in the SolarSystemActivity for
 * wormholes.
 */
public class WormholeFragment extends BaseGlFragment {
  private static final Log log = new Log("WormholeFragment");
  private WormholeSceneManager wormhole;
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
    wormhole = new WormholeSceneManager(WormholeFragment.this, starID);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
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
        DestinationDialog dialog = DestinationDialog.newInstance(star);
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

    Button destroyBtn = (Button) contentView.findViewById(R.id.destroy_btn);
    destroyBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new StyledDialog.Builder(getActivity())
            .setMessage("Are you sure you want to destroy this wormhole? Any ships at this wormhole, as well as ships that are inbound/outbound, will be destroyed!")
            .setPositiveButton("Destroy", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                destroyWormhole();
                dialog.dismiss();
              }
            }).setNegativeButton("Cancel", null).create().show();
      }
    });

    Button takeOverBtn = (Button) contentView.findViewById(R.id.takeover_btn);
    takeOverBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new StyledDialog.Builder(getActivity())
            .setMessage("Are you sure you want to take over this wormhole? You will become the new owner, and will be able to tune it to your alliance's wormholes.")
            .setPositiveButton("Take over", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                takeOverWormhole();
                dialog.dismiss();
              }
            })
            .setNegativeButton("Cancel", null)
            .create().show();
      }
    });

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
        FleetMoveActivity.show(getActivity(), fleet);
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

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      refreshStar();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refreshStar();
    }
  };

  /** Sends a request to the server to destroy this wormhole. */
  private void destroyWormhole() {
    postDestroyOrTakeOverRequest("stars/" + star.getKey() + "/wormhole/destroy", true);
  }

  /** Sends a request to the server to take over this wormhole. */
  private void takeOverWormhole() {
    postDestroyOrTakeOverRequest("stars/" + star.getKey() + "/wormhole/take-over", false);
  }

  private void postDestroyOrTakeOverRequest(final String url, final boolean destroy) {
    final Activity activity = getActivity();

    new BackgroundRunner<Boolean>() {
      private String errorMessage;

      @Override
      protected Boolean doInBackground() {
        try {
          ApiClient.postProtoBuf(url, null);
        } catch (ApiException e) {
          errorMessage = e.getServerErrorMessage();
          return false;
        } catch (Exception e) {
          log.error("Exception caught sending error reports.", e);
          return false;
        }

        return true;
      }

      @Override
      protected void onComplete(Boolean result) {
        if (result) {
          SectorManager.i.refreshSector(star.getSectorX(), star.getSectorY());

          if (destroy) {
            // if we've destroyed the star, exit back to the starfield.
            activity.finish();
          } else {
            // otherwise, just refresh the star
            StarManager.i.refreshStar(star.getID());
          }
        } else {
          if (errorMessage != null) {
            new StyledDialog.Builder(activity)
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .create().show();
          }
        }
      }
    }.execute();
  }

  private void refreshStar() {
    if (star.getWormholeExtra().getDestWormholeID() != 0 && (destStar == null || !destStar.getKey()
        .equals(Integer.toString(star.getWormholeExtra().getDestWormholeID())))) {
      destStar = StarManager.i.getStar(star.getWormholeExtra().getDestWormholeID());
    }

    TextView starName = (TextView) contentView.findViewById(R.id.star_name);
    TextView destinationName = (TextView) contentView.findViewById(R.id.destination_name);
    FleetListWormhole fleetList = (FleetListWormhole) contentView.findViewById(R.id.fleet_list);

    starName.setText(star.getName());

    TreeMap<String, Star> stars = new TreeMap<String, Star>();
    stars.put(star.getKey(), star);
    fleetList.refresh(star.getFleets(), stars);

    if (destStar != null) {
      BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
      if (wormholeExtra.getTuneCompleteTime() != null && wormholeExtra.getTuneCompleteTime()
          .isAfter(DateTime.now())) {
        tuneCompleteTime = wormholeExtra.getTuneCompleteTime();
      }

      String str = String.format(Locale.ENGLISH, "→ %s", destStar.getName());
      if (tuneCompleteTime != null) {
        str = "<font color=\"red\">" + str + "</font>";
      }

      contentView.findViewById(R.id.view_destination_btn).setEnabled(true);
      destinationName.setText(Html.fromHtml(str));
    } else {
      contentView.findViewById(R.id.view_destination_btn).setEnabled(false);
      destinationName.setText(Html.fromHtml("→ <i>None</i>"));
    }
    updateTuningProgress();

    Empire empire = EmpireManager.i.getEmpire(star.getWormholeExtra().getEmpireID());
    if (empire != null) {
      TextView empireName = (TextView) contentView.findViewById(R.id.empire_name);
      empireName.setVisibility(View.VISIBLE);
      empireName.setText(empire.getDisplayName());

      Bitmap bmp = EmpireShieldManager.i.getShield(getActivity(), empire);
      ImageView empireIcon = (ImageView) contentView.findViewById(R.id.empire_icon);
      empireIcon.setVisibility(View.VISIBLE);
      empireIcon.setImageBitmap(bmp);
    } else {
      contentView.findViewById(R.id.empire_name).setVisibility(View.GONE);
      contentView.findViewById(R.id.empire_icon).setVisibility(View.GONE);
    }

    // if there's no star with a wormhole disruptor in range, then we disable the buttons that will
    // require a nearby wormhole disruptor.
    contentView.findViewById(R.id.destroy_btn).setEnabled(false);
    contentView.findViewById(R.id.takeover_btn).setEnabled(false);
    new BackgroundRunner<Boolean>() {
      @Override
      protected Boolean doInBackground() {
        String url = "stars/" + star.getKey() + "/wormhole/disruptor-nearby";
        try {
          ApiClient.getString(url);
          return true;
        } catch (Exception e) {
          return false;
        }
      }

      @Override
      protected void onComplete(Boolean found) {
        contentView.findViewById(R.id.destroy_btn).setEnabled(found);
        contentView.findViewById(R.id.takeover_btn).setEnabled(found);
      }
    }.execute();
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
    } else {
      tuningProgress.setText("");
    }
  }

  /**
   * Create the camera, we don't have a zoom factor.
   */
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
    wormhole.onLoadResources();
  }

  @Override
  protected Scene onCreateScene() throws IOException {
    return wormhole.createScene();
  }

  @Override
  public void onStart() {
    super.onStart();
    wormhole.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    wormhole.onStop();
  }
}
