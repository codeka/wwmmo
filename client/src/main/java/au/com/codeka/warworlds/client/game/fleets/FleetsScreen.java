package au.com.codeka.warworlds.client.game.fleets;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.world.ArrayListStarCollection;
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection;
import au.com.codeka.warworlds.client.game.world.StarCollection;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
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

  /**
   * Construct a new {@link FleetsScreen}.
   *
   * @param star The {@link Star} to display fleets of. If null, the fleets of all stars will
   *             be displayed.
   */
  public FleetsScreen(@Nullable Star star) {
    if (star == null) {
      starCollection = new MyEmpireStarCollection();
    } else {
      starCollection = ArrayListStarCollection.of(star);
    }
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup parent) {
    super.onCreate(context, parent);

    layout = new FleetsLayout(context.getActivity(), starCollection);
  }

  @Override
  public View onShow() {
    return layout;
  }
}
