package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.TransitionManager;

import com.google.android.material.tabs.TabLayout;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ui.views.TabPlusContentView;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * View which contains the tabs for a single colony. Each tab in turn contains a list of buildings,
 * ships and the queue.
 */
public class ColonyView extends TabPlusContentView {
  private final Context context;
  private final BuildLayout buildLayout;

  private Star star;
  private Colony colony;
  private TabContentView contentView;

  public ColonyView(@NonNull Context context, Star star, Colony colony, BuildLayout buildLayout) {
    super(context);
    this.context = context;
    this.buildLayout = buildLayout;
    this.star = star;
    this.colony = colony;

    addTab(R.string.buildings);
    addTab(R.string.ships);
    addTab(R.string.queue);
  }

  public void refresh(Star star, Colony colony) {
    this.star = star;
    this.colony = colony;
    if (contentView != null) {
      contentView.refresh(star, colony);
    }
  }

  @Override
  protected void onTabSelected(TabLayout.Tab tab, int index) {
    ViewGroup tabContent = getTabContent();
    TransitionManager.beginDelayedTransition(tabContent);

    buildLayout.hideBottomSheet();
    tabContent.removeAllViews();
    if (tab.getPosition() == 0) {
      contentView = new BuildingsView(context, star, colony, buildLayout);
    } else if (tab.getPosition() == 1) {
      contentView = new ShipsView(context, star, colony, buildLayout);
    } else if (tab.getPosition() == 2) {
      contentView = new QueueView(context, star, colony);
    }
    tabContent.addView((View) contentView);
  }
}
