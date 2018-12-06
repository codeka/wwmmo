package au.com.codeka.warworlds.client.game.fleets;

import android.view.ViewGroup;

import androidx.annotation.Nullable;

import au.com.codeka.warworlds.client.game.world.ArrayListStarCollection;
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection;
import au.com.codeka.warworlds.client.game.world.StarCollection;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This screen contains a list of fleets, and lets you do all the interesting stuff on them (like
 * merge, split, move, etc).
 */
public class FleetsScreen extends Screen {
  private static final Log log = new Log("FleetsScreen");

  private final StarCollection starCollection;
  private FleetsLayout layout;
  private Long initialFleetId;

  /**
   * Construct a new {@link FleetsScreen}.
   *
   * @param star The {@link Star} to display fleets of. If null, the fleets of all stars will
   *             be displayed.
   * @param fleetId If non-null, the ID of the fleet to have initially selected. Only possible when
   *                star is also non-null.
   */
  public FleetsScreen(@Nullable Star star, @Nullable Long fleetId) {
    if (star == null) {
      starCollection = new MyEmpireStarCollection();
    } else {
      starCollection = ArrayListStarCollection.of(star);
      initialFleetId = fleetId;
    }
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup parent) {
    super.onCreate(context, parent);

    layout = new FleetsLayout(context.getActivity(), starCollection);
    if (initialFleetId != null) {
      layout.selectFleet(initialFleetId);
    }
  }

  @Override
  public ShowInfo onShow() {
    return ShowInfo.builder().view(layout).build();
  }
}
