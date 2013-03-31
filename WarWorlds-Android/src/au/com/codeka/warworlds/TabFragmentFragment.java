package au.com.codeka.warworlds;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

/**
 * Similar to TabFragmentActivity, but this one puts the tabs into a fragment.
 */
public abstract class TabFragmentFragment extends Fragment {
    private TabManager mTabManager;
    private TabHost mTabHost;
    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutID(), container, false);

        mTabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
        mTabManager = new TabManager(mTabHost);

        createTabs();

        return mRootView;
    }

    protected abstract void createTabs();

    /**
     * This can be overridden by a sub class to use a different layout.
     */
    protected int getLayoutID() {
        return R.layout.tab_fragment_activity;
    }

    protected TabHost getTabHost() {
        return mTabHost;
    }

    protected TabManager getTabManager() {
        return mTabManager;
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
            TabInfo lastTab = (TabInfo) lastTabBase;

            FragmentTransaction ft = mContainerFragment.getChildFragmentManager().beginTransaction();
            if (lastTab != null) {
                if (lastTab.fragment != null) {
                    ft.detach(lastTab.fragment);
                }
            }

            if (fragment == null) {
                fragment = Fragment.instantiate(mContainerFragment.getActivity(), fragmentClass.getName(), args);
                ft.add(R.id.real_tabcontent, fragment, title);
            } else {
                ft.attach(fragment);
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
