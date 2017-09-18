package au.com.codeka.warworlds.client.game.build;

import static com.google.common.base.Preconditions.checkNotNull;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.TabbedBaseFragment;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildScreen extends Screen {
  private static final Log log = new Log("BuildScreen");

  private Star star;
  private List<Colony> colonies;
  private Colony currColony;

  private BuildLayout layout;

  public BuildScreen(Star star, int planetIndex) {
    this.star = star;
    colonies = new ArrayList<>();
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    for (Planet planet : star.planets) {
      if (planet.colony != null
          && planet.colony.empire_id != null
          && planet.colony.empire_id.equals(myEmpire.id)) {
        colonies.add(planet.colony);
        if (planet.index.equals(planetIndex)) {
          currColony = planet.colony;
        }
      }
    }
    if (currColony == null) {
      // Shouldn't happen, but maybe we were given a bad planetIndex?
      currColony = colonies.get(0);
    }
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup parent) {
    super.onCreate(context, parent);

    layout = new BuildLayout(context.getActivity(), star, colonies);
    layout.refreshColonyDetails(currColony);
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

    boolean dataSetChanged = (star == null);

    star = s;
    colonies = new ArrayList<>();
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null
          && planet.colony.empire_id.equals(myEmpire.id)) {
        colonies.add(planet.colony);
      }
    }
/*
    if (dataSetChanged) {
      colonyPagerAdapter.notifyDataSetChanged();
    }
*/
  }

}
