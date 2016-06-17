package au.com.codeka.warworlds.client.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import au.com.codeka.warworlds.client.R;

/**
 * A {@link BaseFragment} that is just a container for tabs.
 */
public abstract class TabbedBaseFragment extends BaseFragment {
  private TabManager tabManager;
  private TabHost tabHost;
  private View rootView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(getLayoutID(), container, false);

    tabHost = (TabHost) rootView.findViewById(android.R.id.tabhost);
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
    private Fragment mContainerFragment;

    Class<?> fragmentClass;
    Bundle args;
    Fragment fragment;

    public TabInfo(Fragment containerFragment, String title, Class<?> fragmentClass, Bundle args) {
      super(title);
      mContainerFragment = containerFragment;
      this.fragmentClass = fragmentClass;
      this.args = args;
    }

    @Override
    public View createTabContent(String tag) {
      View v = new View(mContainerFragment.getActivity());
      v.setMinimumHeight(0);
      v.setMinimumWidth(0);
      return v;
    }

    @Override
    public void switchTo(TabManager.TabInfo lastTabBase) {
      FragmentTransaction ft = mContainerFragment.getChildFragmentManager().beginTransaction();
      if (fragment == null) {
        fragment = Fragment.instantiate(mContainerFragment.getActivity(), fragmentClass.getName(), args);
      }
      ft.replace(R.id.real_tabcontent, fragment, title);

      try {
        ft.commit();
        mContainerFragment.getChildFragmentManager().executePendingTransactions();
      } catch (IllegalStateException e) {
        // we can ignore this since it probably just means the activity is gone.
      }
    }

    @Override
    public void recreate() {
      FragmentTransaction ft = mContainerFragment.getChildFragmentManager().beginTransaction();
      if (fragment != null) {
        ft.detach(fragment);
        ft.attach(fragment);
      } else {
        fragment = Fragment.instantiate(mContainerFragment.getActivity(), fragmentClass.getName(), args);
        ft.add(R.id.real_tabcontent, fragment, title);
      }

      try {
        ft.commit();
        mContainerFragment.getChildFragmentManager().executePendingTransactions();
      } catch (IllegalStateException e) {
        // we can ignore this since it probably just means the activity is gone.
      }
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
