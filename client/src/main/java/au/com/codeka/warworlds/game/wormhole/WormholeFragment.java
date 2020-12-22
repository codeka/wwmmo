package au.com.codeka.warworlds.game.wormhole;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.joda.time.DateTime;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.ctrl.FleetListWormhole;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveFragmentArgs;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * This fragment is used in place of SolarSystemFragment in the SolarSystemActivity for
 * wormholes.
 */
public class WormholeFragment extends BaseFragment {
  private static final Log log = new Log("WormholeFragment");
  private Object wormhole;
  private Star star;
  private Star destStar;
  private DateTime tuneCompleteTime;
  private Handler handler;
  private View contentView;

  private boolean refreshing = false;
  private boolean needRefresh = false;

  private final HashMap<Integer, DisruptorStatus> disruptorStatusMap = new HashMap<>();

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    handler = new Handler();

    EmpireShieldManager.i.clearTextureCache();

    Bundle extras = getArguments();
    int starID = (int) extras.getLong("au.com.codeka.warworlds.StarID");
    wormhole = null;//new WormholeSceneManager(WormholeFragment.this, starID);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    contentView = super.onCreateView(inflater, container, savedInstanceState);

    Button renameBtn = contentView.findViewById(R.id.rename_btn);
    renameBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        RenameDialog dialog = new RenameDialog();
        dialog.setWormhole(star);
        dialog.show(getFragmentManager(), "");
      }
    });

    Button destinationBtn = contentView.findViewById(R.id.destination_btn);
    destinationBtn.setOnClickListener(
        v -> {
          Bundle args = new Bundle();
          Messages.Star.Builder sb = Messages.Star.newBuilder();
          star.toProtocolBuffer(sb);
          args.putByteArray("srcWormhole", sb.build().toByteArray());
          NavHostFragment.findNavController(this).navigate(R.id.destinationFragment, args);
        });

    Button viewDestinationBtn = contentView.findViewById(R.id.view_destination_btn);
    viewDestinationBtn.setOnClickListener(v -> {
      if (destStar == null) {
        return;
      }

      // This cast isn't great...
//TODO        ((SolarSystemActivity) getActivity()).showStar(destStar.getID());
    });
    viewDestinationBtn.setEnabled(false);

    Button destroyBtn = contentView.findViewById(R.id.destroy_btn);
    destroyBtn.setOnClickListener(v -> new StyledDialog.Builder(getActivity())
        .setMessage("Are you sure you want to destroy this wormhole? Any ships at this wormhole, " +
            "as well as ships that are inbound/outbound, will be destroyed!")
        .setPositiveButton("Destroy", (dialog, which) -> {
          destroyWormhole();
          dialog.dismiss();
        }).setNegativeButton("Cancel", null).create().show());

    Button takeOverBtn = (Button) contentView.findViewById(R.id.takeover_btn);
    takeOverBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new StyledDialog.Builder(getActivity())
            .setMessage("Are you sure you want to take over this wormhole? You will become the " +
                "new owner, and will be able to tune it to your alliance's wormholes.")
            .setPositiveButton("Take over", (dialog, which) -> {
              takeOverWormhole();
              dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .create().show();
      }
    });

    FleetListWormhole fleetList = contentView.findViewById(R.id.fleet_list);
    fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
      @Override
      public void onFleetView(Star star, Fleet fleet) {
        // won't be called here...
      }

      @Override
      public void onFleetSplit(Star star, Fleet fleet) {
        FleetSplitDialog dialog = new FleetSplitDialog();
        dialog.setFleet(fleet);
        dialog.show(getChildFragmentManager(), "");
      }

      @Override
      public void onFleetBoost(Star star, Fleet fleet) {
        // won't be called here
      }

      @Override
      public void onFleetMove(Star star, Fleet fleet) {
        NavHostFragment.findNavController(WormholeFragment.this).navigate(
            R.id.fleetMoveFragment,
            new FleetMoveFragmentArgs.Builder(star.getID(), fleet.getID())
                .build().toBundle());
      }

      @Override
      public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
        FleetMergeDialog dialog = new FleetMergeDialog();
        dialog.setup(fleet, potentialFleets);
        dialog.show(getChildFragmentManager(), "");
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

    ServerGreeter.waitForHello(requireMainActivity(),
        (success, greeting) -> {
          Bundle extras = requireArguments();
          int starID = (int) extras.getLong("au.com.codeka.warworlds.StarID");

          star = StarManager.i.getStar(starID);
          if (star != null) {
            refreshStar();
          }
        });

    StarManager.eventBus.register(eventHandler);
  }

  @Override
  public void onPause() {
    super.onPause();

    StarManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      Bundle extras = requireArguments();
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

  private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  private void postDestroyOrTakeOverRequest(final String url, final boolean destroy) {
    final Activity activity = getActivity();

    App.i.getTaskRunner().runTask(() -> {
      try {
        ApiClient.postProtoBuf(url, null);
      } catch (ApiException e) {
        sneakyThrow(e);
      }
    }, Threads.BACKGROUND).then(() -> {
      SectorManager.i.refreshSector(star.getSectorX(), star.getSectorY());

      if (destroy) {
        // if we've destroyed the star, exit back to the starfield.
        NavHostFragment.findNavController(this).popBackStack();
      } else {
        // otherwise, just refresh the star
        StarManager.i.refreshStar(star.getID());
      }
    }, Threads.UI).error((ApiException e) -> {
      String errorMessage = e.getServerErrorMessage();
      if (errorMessage != null) {
        new StyledDialog.Builder(activity)
            .setMessage(errorMessage)
            .setPositiveButton("OK", null)
            .create().show();
      }
    }, Threads.UI);
  }

  private void refreshStar() {
    if (refreshing) {
      needRefresh = true;
      return;
    }
    refreshing = true;

    if (star.getWormholeExtra().getDestWormholeID() != 0 && (
            destStar == null || destStar.getID() != star.getWormholeExtra().getDestWormholeID())) {
      log.info("Fetching the destStar's details");
      destStar = StarManager.i.getStar(star.getWormholeExtra().getDestWormholeID());
    }

    TextView starName = contentView.findViewById(R.id.star_name);
    TextView destinationName = contentView.findViewById(R.id.destination_name);
    FleetListWormhole fleetList = contentView.findViewById(R.id.fleet_list);

    starName.setText(star.getName());

    TreeMap<String, Star> stars = new TreeMap<>();
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
      TextView empireName = contentView.findViewById(R.id.empire_name);
      empireName.setVisibility(View.VISIBLE);
      empireName.setText(empire.getDisplayName());

      Bitmap bmp = EmpireShieldManager.i.getShield(getActivity(), empire);
      ImageView empireIcon = contentView.findViewById(R.id.empire_icon);
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
    checkDisruptorStatus(star.getID(), () -> {
      contentView.findViewById(R.id.destroy_btn).setEnabled(true);
      contentView.findViewById(R.id.takeover_btn).setEnabled(true);
    });

    refreshing = false;
    if (needRefresh) {
      // If we had a call to refresh while we were refreshing, then let's do it now, just
      // to be on the safe side.
      needRefresh = false;
      handler.post(this::refreshStar);
    }
  }

  private void updateTuningProgress() {
    TextView tuningProgress = contentView.findViewById(R.id.tuning_progress);
    if (tuneCompleteTime == null && destStar != null) {
      tuningProgress.setText("");

      String str = String.format(Locale.ENGLISH, "→ %s", destStar.getName());
      TextView destinationName = contentView.findViewById(R.id.destination_name);
      destinationName.setText(Html.fromHtml(str));
    } else if (tuneCompleteTime != null) {
      tuningProgress.setText(String.format(Locale.ENGLISH, "%s left",
          TimeFormatter.create().format(DateTime.now(), tuneCompleteTime)));

      handler.postDelayed(this::updateTuningProgress, 10000);
    } else {
      tuningProgress.setText("");
    }
  }

  @Override
  public void onStart() {
    super.onStart();
  //  wormhole.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
  ///  wormhole.onStop();
  }

  /**
   * Checks whether a disruptor is nearby, and calls the given callback if there is one. We'll cache
   * the status for a little while to ensure that we don't hammer the server.
   */
  private void checkDisruptorStatus(int wormholeId, Runnable disruptorNearbyCallback) {
    DisruptorStatus status;
    synchronized (disruptorStatusMap) {
      status = disruptorStatusMap.get(wormholeId);
    }
    if (status == null || status.lastCheckTime.isBefore(DateTime.now().minusMinutes(5))) {
      App.i.getTaskRunner().runTask(() -> {
        String url =
            String.format(Locale.ENGLISH, "stars/%d/wormhole/disruptor-nearby", wormholeId);
        try {
          ApiClient.getString(url);
          return true;
        } catch (ApiException e) {
          return false;
        }
      }, Threads.BACKGROUND).then((found) -> {
        synchronized (disruptorStatusMap) {
          disruptorStatusMap.put(wormholeId, new DisruptorStatus(found));
        }
        if (found) {
          disruptorNearbyCallback.run();
        }
      }, Threads.UI);
    } else if (status.nearby) {
      disruptorNearbyCallback.run();
    }
  }

  private static class DisruptorStatus {
    public boolean nearby;
    public DateTime lastCheckTime;

    public DisruptorStatus(boolean nearby) {
      this.nearby = nearby;
      this.lastCheckTime = DateTime.now();
    }
  }
}
