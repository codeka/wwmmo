package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.TreeMap;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class FleetFragment extends BaseFragment {
  private Star star;
  private FleetList fleetList;
  private boolean firstRefresh = true;

  private FleetFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    fleetList = new FleetList(inflater.getContext());
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT);
    fleetList.setLayoutParams(lp);
    return fleetList;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    args = FleetFragmentArgs.fromBundle(requireArguments());

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
        FleetManager.i.boostFleet(fleet, null);
      }

      @Override
      public void onFleetMove(Star star, Fleet fleet) {
        FleetMoveActivity.show(requireActivity(), fleet);
      }

      @Override
      public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
        FleetMergeDialog dialog = new FleetMergeDialog();
        dialog.setup(fleet, potentialFleets);
        dialog.show(getChildFragmentManager(), "");
      }

      @Override
      public void onFleetStanceModified(Star star, Fleet fleet,
          Fleet.Stance newStance) {
        EmpireManager.i.getEmpire().updateFleetStance(star, fleet, newStance);
      }
    });

    // no "View" button, because it doesn't make sense here...
    fleetList.findViewById(R.id.view_btn).setVisibility(View.GONE);
  }

  @Override
  public void onResume() {
    super.onResume();

    ServerGreeter.waitForHello(requireActivity(), (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(requireContext(), WelcomeFragment.class));
      } else {
        StarManager.eventBus.register(eventHandler);
        star = StarManager.i.getStar(args.getStarID());
        if (star != null) {
          refreshStarDetails();
        }
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star != null && !star.getKey().equals(s.getKey())) {
        return;
      }
      star = s;
      refreshStarDetails();
    }
  };

  private void refreshStarDetails() {
    requireMainActivity().requireSupportActionBar().setTitle(star.getName());
    requireMainActivity().requireSupportActionBar().setSubtitle("Fleets");

    TreeMap<String, Star> stars = new TreeMap<>();
    stars.put(star.getKey(), star);
    fleetList.refresh(star.getFleets(), stars);

    int fleetID = args.getFleetID();
    if (firstRefresh && fleetID > 0) {
      fleetList.selectFleet(fleetID, true);
      firstRefresh = false;
    }
  }
}
