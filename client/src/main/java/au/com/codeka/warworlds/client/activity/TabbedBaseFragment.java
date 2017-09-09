package au.com.codeka.warworlds.client.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ui.FragmentScreen;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenStack;

/**
 * A {@link BaseFragment} that is just a container for tabs.
 */
public abstract class TabbedBaseFragment extends BaseFragment {
  private TabManager tabManager;
  private TabHost tabHost;
  private View rootView;

  // TODO: I don't think screens are the right abstraction for tabs.
  private ScreenStack screenStack;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(getLayoutID(), container, false);

    screenStack =
        new ScreenStack(getFragmentActivity(), rootView.findViewById(R.id.real_tabcontent));

    tabHost = rootView.findViewById(android.R.id.tabhost);
    tabManager = new TabManager(tabHost);

    createTabs();

    return rootView;
  }

  protected abstract void createTabs();

  /**
   * This can be overridden by a sub class to use a different layout.
   */
  protected int getLayoutID() {
    return R.layout.frag_tabbed;
  }

  public TabHost getTabHost() {
    return tabHost;
  }

  protected TabManager getTabManager() {
    return tabManager;
  }

  public static class TabInfo extends TabManager.TabInfo {
    private TabbedBaseFragment containerFragment;

    Class<?> fragmentClass;
    Bundle args;
    BaseFragment fragment;

    public TabInfo(
        TabbedBaseFragment containerFragment,
        String title,
        Class<?> fragmentClass,
        Bundle args) {
      super(title);
      this.containerFragment = containerFragment;
      this.fragmentClass = fragmentClass;
      this.args = args;
    }

    @Override
    public View createTabContent(String tag) {
      View v = new View(containerFragment.getContext());
      v.setMinimumHeight(0);
      v.setMinimumWidth(0);
      return v;
    }

    private void ensureFragment() {
      if (fragment == null) {
        try {
          fragment = (BaseFragment) fragmentClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public void switchTo(TabManager.TabInfo lastTabBase) {
      ensureFragment();
      Screen screen = new FragmentScreen(fragment);
      containerFragment.screenStack.home();
      containerFragment.screenStack.push(screen);
    }

    @Override
    public void recreate() {
      ensureFragment();
      Screen screen = new FragmentScreen(fragment);
      containerFragment.screenStack.home();
      containerFragment.screenStack.push(screen);
    }

    @Override
    public boolean reload() {
      if (fragment != null && (fragment instanceof TabManager.Reloadable)) {
        ((TabManager.Reloadable) fragment).reloadTab();
        return true;
      }
      return false;
    }
  }
}
