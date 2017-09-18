package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
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
  private final Star star;
  private final Colony colony;

  private final TabLayout tabLayout;

  public ColonyView(@NonNull Context context, Star star, Colony colony) {
    super(context);
    this.star = star;
    this.colony = colony;

    tabLayout = new TabLayout(context, null, R.style.TabLayout);
    tabLayout.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    tabLayout.setTabMode(TabLayout.MODE_FIXED);
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.buildings)
        .setCustomView(new BuildingsView(context, star, colony)));
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.ships));
    tabLayout.addTab(tabLayout.newTab()
        .setText(R.string.queue));
    addView(tabLayout);

    tabLayout.addOnTabSelectedListener(tabSelectedListener);
  }

  private final TabLayout.OnTabSelectedListener tabSelectedListener =
      new TabLayout.OnTabSelectedListener() {
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
      
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }
  };
}
