package au.com.codeka.warworlds.client.game.starfield;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.chat.ChatFragment;
import au.com.codeka.warworlds.client.game.empire.EmpireFragment;
import au.com.codeka.warworlds.client.game.fleets.FleetsFragment;
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemContainerFragment;
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemFragment;
import au.com.codeka.warworlds.client.ui.FragmentScreen;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.SharedViews;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This is the main fragment that shows the starfield, lets you navigate around, select stars
 * and fleets and so on.
 */
public class StarfieldScreen extends Screen {
  private final Log log = new Log("StarfieldScreen");

  private StarfieldManager starfieldManager;

  private ScreenContext context;
  private StarfieldLayout layout;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    this.context = context;
    layout = new StarfieldLayout(context.getActivity(), layoutCallbacks);

    starfieldManager = ((MainActivity) context.getActivity()).getStarfieldManager();
    if (starfieldManager.getSelectedStar() != null) {
      showStarSelectedBottomPane(starfieldManager.getSelectedStar());
    } else {
      showEmptyBottomPane(true);
    }
  }

  @Override
  public View onShow() {
    starfieldManager.addTapListener(tapListener);
    return layout;
  }

  @Override
  public void onHide() {
    starfieldManager.removeTapListener(tapListener);
  }

  private void showEmptyBottomPane(boolean instant) {
    EmptyBottomPane emptyBottomPane = new EmptyBottomPane(context.getActivity(),
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
    layout.showBottomPane(emptyBottomPane, instant);
  }

  private void showStarSelectedBottomPane(Star star) {
    StarSelectedBottomPane starSelectedBottomPane = new StarSelectedBottomPane(
        context.getActivity(), star, new StarSelectedBottomPane.Callback() {
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
        SolarSystemContainerFragment fragment = new SolarSystemContainerFragment();
        fragment.setArguments(SolarSystemFragment.createArguments(star.id));
        context.pushScreen(new FragmentScreen(fragment), createSharedViews());
      }

      @Override
      public void onFleetClicked(Star star, Fleet fleet) {
        FleetsFragment fragment = new FleetsFragment();
        fragment.setArguments(FleetsFragment.createArguments(star.id, fleet.id));
        context.pushScreen(new FragmentScreen(fragment), createSharedViews());
      }
    });

    layout.showBottomPane(starSelectedBottomPane, false /* instant */);
  }

  private void showFleetSelectedBottomPane(Star star, Fleet fleet) {
    FleetSelectedBottomPane fleetSelectedBottomPane = new FleetSelectedBottomPane(
        context.getActivity(), star, fleet, new FleetSelectedBottomPane.Callback() {
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

    layout.showBottomPane(fleetSelectedBottomPane, false /* instant */);
  }

  private void onEmpireClick() {
    EmpireFragment fragment = new EmpireFragment();
    fragment.setArguments(EmpireFragment.createArguments());
    context.pushScreen(new FragmentScreen(fragment), createSharedViews());
  }

  private void onSitrepClick() {
  }

  private void onAllianceClick() {
  }

  private SharedViews createSharedViews() {
    return SharedViews.builder()
        .addSharedView(R.id.bottom_pane)
        .addSharedView(R.id.top_pane)
        .build();
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

  private final StarfieldLayout.Callbacks layoutCallbacks = new StarfieldLayout.Callbacks() {
    @Override
    public void onChatClick(@Nullable Long roomId) {
      context.pushScreen(
          new FragmentScreen(new ChatFragment()),
          SharedViews.builder()
              .addSharedView(R.id.bottom_pane)
              .build());
    }
  };
}
