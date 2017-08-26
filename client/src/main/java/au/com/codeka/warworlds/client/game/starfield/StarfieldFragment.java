package au.com.codeka.warworlds.client.game.starfield;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.ctrl.ChatMiniView;
import au.com.codeka.warworlds.client.game.chat.ChatFragment;
import au.com.codeka.warworlds.client.game.empire.EmpireFragment;
import au.com.codeka.warworlds.client.game.fleets.FleetsFragment;
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemContainerFragment;
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemFragment;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This is the main fragment that shows the starfield, lets you navigate around, select stars
 * and fleets and so on.
 */
public class StarfieldFragment extends BaseFragment {
  private final Log log = new Log("StarfieldFragment");

  private StarfieldManager starfieldManager;

  private ViewGroup stuff;
  private ViewGroup bottomPane;
  private ChatMiniView chatMiniView;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_starfield;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  //  selectionDetailsView = (SelectionDetailsView) view.findViewById(R.id.selection_details);
    bottomPane = (ViewGroup) view.findViewById(R.id.bottom_pane);
  //  allianceBtn = (Button) view.findViewById(R.id.alliance_btn);
 //   empireBtn = (Button) view.findViewById(R.id.empire_btn);
    chatMiniView = (ChatMiniView) view.findViewById(R.id.mini_chat);

    chatMiniView.setCallback(roomId -> {
      getFragmentTransitionManager().replaceFragment(
          ChatFragment.class,
          new Bundle() /* TODO: args */,
          SharedViewHolder.builder()
              .addSharedView(R.id.bottom_pane, "bottom_pane")
              .build());
    });

    starfieldManager = ((MainActivity) getActivity()).getStarfieldManager();
    if (starfieldManager.getSelectedStar() != null) {
      showStarSelectedBottomPane(starfieldManager.getSelectedStar());
    } else {
      showEmptyBottomPane(true);
    }

    stuff = (ViewGroup) view.findViewById(R.id.stuff);
  }

  @Override
  public void onStart() {
    super.onStart();
    starfieldManager.addTapListener(tapListener);
  }

  @Override
  public void onStop() {
    super.onStop();
    starfieldManager.removeTapListener(tapListener);
  }

  private void showEmptyBottomPane(boolean instant) {
    EmptyBottomPane emptyBottomPane = new EmptyBottomPane(getContext(),
        new EmptyBottomPane.Callback() {
      @Override
      public void onEmpireClicked(View view) {
        onEmpireClick();
      }

      @Override
      public void onSitrepClicked(View view) {
        onSitrepClick();
      }

      @Override
      public void onAllianceClicked(View view) {
        onAllianceClick();
      }
    });

    if (!instant) {
      TransitionManager.beginDelayedTransition(stuff);
    }
    bottomPane.removeAllViews();
    bottomPane.addView(emptyBottomPane);
  }

  private void showStarSelectedBottomPane(Star star) {
    StarSelectedBottomPane starSelectedBottomPane = new StarSelectedBottomPane(
        getContext(), star, new StarSelectedBottomPane.Callback() {
      @Override
      public void onEmpireClicked(View view) {
        onEmpireClick();
      }

      @Override
      public void onSitrepClicked(View view) {
      }

      @Override
      public void onAllianceClicked(View view) {
      }

      @Override
      public void onStarClicked(Star star, @Nullable Planet planet) {
        getFragmentTransitionManager().replaceFragment(
            SolarSystemContainerFragment.class,
            SolarSystemFragment.createArguments(star.id),
            SharedViewHolder.builder()
                .addSharedView(R.id.bottom_pane, "bottom_pane")
                //.addSharedView(R.id.top_pane, "top_pane")
                .build());
      }

      @Override
      public void onFleetClicked(Star star, Fleet fleet) {
        getFragmentTransitionManager().replaceFragment(
            FleetsFragment.class,
            FleetsFragment.createArguments(star.id, fleet.id),
            SharedViewHolder.builder()
                .addSharedView(R.id.bottom_pane, "bottom_pane")
                .build());
      }
    });

    TransitionManager.beginDelayedTransition(stuff);
    bottomPane.removeAllViews();
    bottomPane.addView(starSelectedBottomPane);
  }

  private void showFleetSelectedBottomPane(Star star, Fleet fleet) {
    FleetSelectedBottomPane fleetSelectedBottomPane = new FleetSelectedBottomPane(
        getContext(), star, fleet, new FleetSelectedBottomPane.Callback() {
      @Override
      public void onEmpireClicked(View view) {
        onEmpireClick();
      }

      @Override
      public void onSitrepClicked(View view) {
      }

      @Override
      public void onAllianceClicked(View view) {
      }
    });

    TransitionManager.beginDelayedTransition(stuff);
    bottomPane.removeAllViews();
    bottomPane.addView(fleetSelectedBottomPane);
  }

  private void onEmpireClick() {
    getFragmentTransitionManager().replaceFragment(
        EmpireFragment.class,
        EmpireFragment.createArguments(),
        SharedViewHolder.builder()
            .addSharedView(R.id.bottom_pane, "bottom_pane")
            .build());
  }

  private void onSitrepClick() {
  }

  private void onAllianceClick() {
  }

  private final StarfieldManager.TapListener tapListener = new StarfieldManager.TapListener() {
    @Override
    public void onStarTapped(@Nullable Star star) {
      if (star == null) {
        showEmptyBottomPane(false);
      } else {
        showStarSelectedBottomPane(star);
//        selectionDetailsView.showStar(star);
      }
    }

    @Override
    public void onFleetTapped(@Nullable Star star, @Nullable Fleet fleet) {
      if (fleet == null) {
        showEmptyBottomPane(false);
      } else {
        showFleetSelectedBottomPane(star, fleet);
      }
    }
  };
}
