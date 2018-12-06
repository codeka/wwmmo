package au.com.codeka.warworlds.client.game.build;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildScreen extends Screen {
  private static final Log log = new Log("BuildScreen");

  /** We'll let the layout know to refresh progress and so on at this frequency. */
  private static final long REFRESH_DELAY_MS = 1000L;

  private Star star;
  private List<Colony> colonies;
  private Colony currColony;

  private BuildLayout layout;

  public BuildScreen(Star star, int planetIndex) {
    this.star = star;
    extractColonies(star, planetIndex);
    if (currColony == null) {
      // Shouldn't happen, but maybe we were given a bad planetIndex?
      currColony = colonies.get(0);
    }
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup parent) {
    super.onCreate(context, parent);

    layout = new BuildLayout(context.getActivity(), star, colonies, colonies.indexOf(currColony));
    layout.refreshColonyDetails(currColony);
    App.i.getEventBus().register(eventHandler);
  }

  @Override
  public ShowInfo onShow() {
    // Refresh immediately on show
    layout.post(refreshRunnable);
    return ShowInfo.builder().view(layout).build();
  }

  @Override
  public void onHide() {
    layout.removeCallbacks(refreshRunnable);
  }

  @Override
  public void onDestroy() {
    App.i.getEventBus().unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star != null && star.id.equals(s.id)) {
        updateStar(s);
      }
    }
  };

  private void updateStar(Star s) {
    log.info("Updating star %d [%s]...", s.id, s.name);

    Colony oldColony = currColony;
    star = s;
    extractColonies(star, -1);
    if (oldColony != null) {
      for (Colony colony : colonies) {
        if (colony.id.equals(oldColony.id)) {
          currColony = colony;
        }
      }
    }

    layout.refresh(star, colonies);
  }

  private void extractColonies(Star star, int planetIndex) {
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    colonies = new ArrayList<>();
    currColony = null;
    for (Planet planet : star.planets) {
      if (planet.colony != null
          && planet.colony.empire_id != null
          && planet.colony.empire_id.equals(myEmpire.id)) {
        colonies.add(planet.colony);
        if (planet.index == planetIndex) {
          currColony = planet.colony;
        }
      }
    }
  }

  private final Runnable refreshRunnable = new Runnable() {
    private int refreshCount = 0;

    @Override
    public void run() {
      refreshCount++;
      if (refreshCount % 10 == 0) {
        // Every tenth refresh, we'll re-simulate the star
        StarManager.i.queueSimulateStar(star);
      } else {
        layout.refresh(star, colonies);
      }

      layout.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
    }
  };
}
