package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.fleets.FleetsLayout;
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection;
import au.com.codeka.warworlds.client.ui.views.TabPlusContentView;

/**
 * Layout for the {@link EmpireScreen}.
 */
public class EmpireLayout extends TabPlusContentView {
  public EmpireLayout(Context context) {
    super(context);
    setBackgroundColor(context.getResources().getColor(R.color.default_background));

    addTab(R.string.overview);
    addTab(R.string.colonies);
    addTab(R.string.build);
    addTab(R.string.fleets);
  }

  @Override
  protected void onTabSelected(TabLayout.Tab tab, int index) {
    ViewGroup tabContent = getTabContent();
    TransitionManager.beginDelayedTransition(tabContent);

    tabContent.removeAllViews();
    View contentView = null;
    if (index == 0) {
      contentView = new OverviewView(getContext());
    } else if (index == 1) {
      contentView = new ColoniesView(getContext());
    } else if (index == 2) {
      contentView = new BuildQueueView(getContext());
    } else if (index == 3) {
      contentView = new FleetsLayout(getContext(), new MyEmpireStarCollection());
    }
    tabContent.addView(contentView);
  }
}
