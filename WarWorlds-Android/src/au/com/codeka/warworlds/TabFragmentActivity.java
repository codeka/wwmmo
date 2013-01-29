package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private Context mContext = this;
    TabManager mTabManager;
    TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tab_fragment_activity);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabManager = new TabManager(mTabHost);
    }

    protected TabHost getTabHost() {
        return mTabHost;
    }

    protected TabManager getTabManager() {
        return mTabManager;
    }

    protected class TabInfo extends TabManager.TabInfo {
        Class<?> fragmentClass;
        Bundle args;
        Fragment fragment;

        public TabInfo(String title, Class<?> fragmentClass, Bundle args) {
            super(title);
            this.fragmentClass = fragmentClass;
            this.args = args;
        }

        @Override
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumHeight(0);
            v.setMinimumWidth(0);
            return v;
        }

        @Override
        public void switchTo(TabManager.TabInfo lastTabBase) {
            TabInfo lastTab = (TabInfo) lastTabBase;

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (lastTab != null) {
                if (lastTab.fragment != null) {
                    ft.detach(lastTab.fragment);
                }
            }

            if (fragment == null) {
                fragment = Fragment.instantiate(mContext, fragmentClass.getName(), args);
                ft.add(R.id.real_tabcontent, fragment, title);
            } else {
                ft.attach(fragment);
            }

            ft.commit();
            getSupportFragmentManager().executePendingTransactions();
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
