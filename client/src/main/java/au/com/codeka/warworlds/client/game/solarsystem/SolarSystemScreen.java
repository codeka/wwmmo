package au.com.codeka.warworlds.client.game.solarsystem;

import android.view.ViewGroup;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.build.BuildScreen;
import au.com.codeka.warworlds.client.game.fleets.FleetsScreen;
import au.com.codeka.warworlds.client.game.starsearch.StarRecentHistoryManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.SharedViews;
import au.com.codeka.warworlds.client.ui.ShowInfo;
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
  private boolean isCreated;

  public SolarSystemScreen(Star star, int planetIndex) {
    this.star = star;
    this.planetIndex = planetIndex;
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    isCreated = true;
    this.context = context;

    layout = new SolarSystemLayout(context.getActivity(), layoutCallbacks, star, planetIndex);

    App.i.getTaskRunner().runTask(this::doRefresh, Threads.BACKGROUND, 100);
    App.i.getEventBus().register(eventHandler);
  }

  @Override
  public ShowInfo onShow() {
    StarRecentHistoryManager.i.addToLastStars(star);
    return ShowInfo.builder().view(layout).build();
  }

  @Override
  public void onDestroy() {
    isCreated = false;
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

  /**
   * Called on a background thread, we'll simulate the star so that it gets update with correct
   * energy, minerals, etc. We'll schedule it to run every 5 seconds we're on this screen.
   */
  private void doRefresh() {
    StarManager.i.simulateStarSync(star);

    if (isCreated) {
      App.i.getTaskRunner().runTask(this::doRefresh, Threads.BACKGROUND, 5000);
    }
  }

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
      context.pushScreen(
          new FleetsScreen(star, fleetId),
          new SharedViews.Builder()
              .addSharedView(R.id.bottom_pane)
              .build());
    }
  };
}
