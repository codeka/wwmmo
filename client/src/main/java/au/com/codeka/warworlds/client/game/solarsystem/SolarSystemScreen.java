package au.com.codeka.warworlds.client.game.solarsystem;

import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A screen which shows a view of the solar system (star, planets, etc) and is the launching point
 * for managing builds, planet focus, launching fleets and so on.
 */
public class SolarSystemScreen extends Screen {
  private SolarSystemLayout layout;
  private Star star;
  private int planetIndex;

  public SolarSystemScreen(Star star, int planetIndex) {
    this.star = star;
    this.planetIndex = planetIndex;
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);

    layout = new SolarSystemLayout(context.getActivity(), layoutCallbacks, star, planetIndex);

    App.i.getEventBus().register(eventHandler);
  }

  @Override
  public View onShow() {
    return layout;
  }

  @Override
  public void onDestroy() {
    App.i.getEventBus().unregister(eventHandler);
  }

  private void refreshStar(Star star) {
    layout.refreshStar(star);
    this.star = star;
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star.id.equals(s.id)) {
        refreshStar(s);
      }
    }
  };

  private final SolarSystemLayout.Callbacks layoutCallbacks = new SolarSystemLayout.Callbacks() {
    @Override
    public void onBuildClick(int planetIndex) {

    }

    @Override
    public void onFocusClick(int planetIndex) {

    }

    @Override
    public void onSitrepClick() {

    }

    @Override
    public void onViewColonyClick(int planetIndex) {
//      getFragmentTransitionManager().replaceFragment(
//          PlanetDetailsFragment.class,
//          PlanetDetailsFragment.createArguments(star.id, star.planets.indexOf(planet)),
//          SharedViewHolder.builder()
//              .addSharedView(R.id.bottom_pane, "bottom_pane")
//              .addSharedView(sunAndPlanetsView.getPlanetView(planet), "planet_icon")
//              .build());
    }

    @Override
    public void onFleetClick(long fleetId) {

    }
  };
}
