package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.widget.TabHost;

/**
 * This is our base \c FragmentActivity class for a tabbed activity. We'll define the code
 * generating our tab buttons and so on.
 */
@SuppressLint("Registered") // it's a base class
public class TabFragmentActivity extends BaseActivity {
    private TabManager mTabManager;
    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getLayoutID());

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabManager = new TabManager(mTabHost);
    }

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
        private BaseActivity mActivity;

        Class<?> fragmentClass;
        Bundle args;
        Fragment fragment;

        public TabInfo(BaseActivity activity, String title, Class<?> fragmentClass, Bundle args) {
            super(title);
            mActivity = activity;
            this.fragmentClass = fragmentClass;
            this.args = args;
        }

        @Override
        public View createTabContent(String tag) {
            View v = new View(mActivity);
            v.setMinimumHeight(0);
            v.setMinimumWidth(0);
            return v;
        }

        @Override
        public void switchTo(TabManager.TabInfo lastTabBase) {
            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
            if (fragment == null) {
                fragment = Fragment.instantiate(mActivity, fragmentClass.getName(), args);
            }
            ft.replace(R.id.real_tabcontent, fragment, title);

            try {
                ft.commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            } catch (IllegalStateException e) {
                // we can ignore this since it probably just means the activity is gone.
            }
        }

        @Override
        public void recreate() {
            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
            if (fragment != null) {
                ft.detach(fragment);
                ft.attach(fragment);
            } else {
                fragment = Fragment.instantiate(mActivity, fragmentClass.getName(), args);
                ft.add(R.id.real_tabcontent, fragment, title);
            }

            try {
                ft.commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
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
