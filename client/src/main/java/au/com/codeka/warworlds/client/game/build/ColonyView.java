package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * View which contains the tabs for a single colony. Each tab in turn contains a list of buildings,
 * ships and the queue.
 */
public class ColonyView extends FrameLayout {
  private final Context context;
  private final BuildLayout buildLayout;

  private final TabLayout tabLayout;
  private final FrameLayout tabContent;

  private Star star;
  private Colony colony;
  private TabContentView contentView;

  public ColonyView(@NonNull Context context, Star star, Colony colony, BuildLayout buildLayout) {
    super(context);
    this.context = context;
    this.buildLayout = buildLayout;
    this.star = star;
    this.colony = colony;

    tabLayout = new TabLayout(context, null, R.style.TabLayout);
    tabLayout.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    tabLayout.setTabMode(TabLayout.MODE_FIXED);
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.buildings));
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.ships));
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.queue));
    addView(tabLayout);

    tabContent = new FrameLayout(context);
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
    tabLayout.post(() -> {
      // Once the TabLayout has laid itself out, we can work out it's height.
      lp.topMargin = tabLayout.getHeight();
      tabContent.setLayoutParams(lp);

      tabSelectedListener.onTabSelected(tabLayout.getTabAt(0));
    });
    addView(tabContent);

    tabLayout.addOnTabSelectedListener(tabSelectedListener);
  }

  public void refresh(Star star, Colony colony) {
    this.star = star;
    this.colony = colony;
    if (contentView != null) {
      contentView.refresh(star, colony);
    }
  }

  private final TabLayout.OnTabSelectedListener tabSelectedListener =
      new TabLayout.OnTabSelectedListener() {
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
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

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }
  };
}
