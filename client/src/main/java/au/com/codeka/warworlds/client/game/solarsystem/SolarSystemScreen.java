package au.com.codeka.warworlds.client.game.solarsystem;

import static com.google.common.base.Preconditions.checkNotNull;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildScreen;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.SharedViews;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A screen which shows a view of the solar system (star, planets, etc) and is the launching point
 * for managing builds, planet focus, launching fleets and so on.
 */
public class SolarSystemScreen extends Screen {
  private static final Log log = new Log("SolarSystemScreen");

  private ScreenContext context;
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
    this.context = context;

    layout = new SolarSystemLayout(context.getActivity(), layoutCallbacks, star, planetIndex);

    App.i.getEventBus().register(eventHandler);
  }

  @Override
  public View onShow() {
    ActionBar actionBar = checkNotNull(context.getActivity().getSupportActionBar());
    actionBar.show();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    return layout;
  }

  @Override
  public void onHide() {
    ActionBar actionBar = checkNotNull(context.getActivity().getSupportActionBar());
    actionBar.hide();
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
      context.pushScreen(
          new BuildScreen(star, planetIndex),
          new SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build());
    }

    @Override
    public void onFocusClick(int planetIndex) {
      log.info("focus click: %d", planetIndex);
      context.pushScreen(
          new PlanetDetailsScreen(star, star.planets.get(planetIndex)),
          new SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build());
    }

    @Override
    public void onSitrepClick() {

    }

    @Override
    public void onViewColonyClick(int planetIndex) {
      context.pushScreen(
          new PlanetDetailsScreen(star, star.planets.get(planetIndex)),
          new SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build());
    }

    @Override
    public void onFleetClick(long fleetId) {

    }
  };
}
