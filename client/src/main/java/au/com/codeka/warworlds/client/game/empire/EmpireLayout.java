package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.transition.TransitionManager;

import com.google.android.material.tabs.TabLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.fleets.FleetsLayout;
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection;
import au.com.codeka.warworlds.client.ui.views.TabPlusContentView;

/**
 * Layout for the {@link EmpireScreen}.
 */
public class EmpireLayout extends TabPlusContentView {
  @Nonnull private final SettingsView.Callback settingsCallbacks;

  public EmpireLayout(Context context, @Nonnull SettingsView.Callback settingsCallbacks) {
    super(context);
    this.settingsCallbacks = settingsCallbacks;
    setBackgroundColor(context.getResources().getColor(R.color.default_background));

    addTab(R.string.overview);
    addTab(R.string.colonies);
    addTab(R.string.build);
    addTab(R.string.fleets);
    addTab(R.string.settings);
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
    } else if (index == 4) {
      contentView = new SettingsView(getContext(), settingsCallbacks);
    }
    tabContent.addView(contentView);
  }
}
