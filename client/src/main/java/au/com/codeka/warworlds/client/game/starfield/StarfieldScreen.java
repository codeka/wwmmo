package au.com.codeka.warworlds.client.game.starfield;

import android.view.ViewGroup;

import androidx.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.chat.ChatScreen;
import au.com.codeka.warworlds.client.game.fleets.FleetsScreen;
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.SharedViews;
import au.com.codeka.warworlds.client.ui.ShowInfo;
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

    starfieldManager = context.getActivity().getStarfieldManager();
    if (starfieldManager.getSelectedStar() != null) {
      showStarSelectedBottomPane(starfieldManager.getSelectedStar());
    } else {
      showEmptyBottomPane(true);
    }
  }

  @Override
  public ShowInfo onShow() {
    starfieldManager.addTapListener(tapListener);
    return ShowInfo.builder().view(layout).build();
  }

  @Override
  public void onHide() {
    starfieldManager.removeTapListener(tapListener);
  }

  private void showEmptyBottomPane(boolean instant) {
    EmptyBottomPane emptyBottomPane = new EmptyBottomPane(context.getActivity());
    layout.showBottomPane(emptyBottomPane, instant);
  }

  private void showStarSelectedBottomPane(Star star) {
    StarSelectedBottomPane starSelectedBottomPane = new StarSelectedBottomPane(
        context.getActivity(), star, new StarSelectedBottomPane.Callback() {
      @Override
      public void onStarClicked(Star star, @Nullable Planet planet) {
        context.pushScreen(
            new SolarSystemScreen(star, -1 /* planetIndex */),
            new SharedViews.Builder()
                .addSharedView(R.id.top_pane)
                .addSharedView(R.id.bottom_pane)
                .build());
      }

      @Override
      public void onFleetClicked(Star star, Fleet fleet) {
        context.pushScreen(new FleetsScreen(star, fleet.id), createSharedViews());
      }
    });

    layout.showBottomPane(starSelectedBottomPane, false /* instant */);
  }

  private void showFleetSelectedBottomPane(Star star, Fleet fleet) {
    FleetSelectedBottomPane fleetSelectedBottomPane = new FleetSelectedBottomPane(
        context.getActivity(), star, fleet);

    layout.showBottomPane(fleetSelectedBottomPane, false /* instant */);
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
          new ChatScreen(),
          SharedViews.builder()
              .addSharedView(R.id.bottom_pane)
              .build());
    }
  };
}
