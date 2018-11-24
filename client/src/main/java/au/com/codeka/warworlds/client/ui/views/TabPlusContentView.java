package au.com.codeka.warworlds.client.ui.views;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;

import au.com.codeka.warworlds.client.R;

/**
 * A simple layout that just shows tabs at the top and content underneath.
 */
public abstract class TabPlusContentView extends FrameLayout {
  private final TabLayout tabLayout;
  private final FrameLayout tabContent;

  public TabPlusContentView(@NonNull Context context) {
    super(context);

    tabLayout = new TabLayout(context, null, R.style.TabLayout);
    tabLayout.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
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

  protected void addTab(int titleResId) {
    tabLayout.addTab(tabLayout.newTab().setText(titleResId));
  }

  protected abstract void onTabSelected(TabLayout.Tab tab, int index);

  protected FrameLayout getTabContent() {
    return tabContent;
  }

  private final TabLayout.OnTabSelectedListener tabSelectedListener =
      new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
          TabPlusContentView.this.onTabSelected(tab, tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
      };
}
