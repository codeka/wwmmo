package au.com.codeka.warworlds;

import java.util.TreeMap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * This is our base \c FragmentActivity class for a tabbed activity. We'll define the code
 * generating our tab buttons and so on.
 */
public class TabFragmentActivity extends FragmentActivity {
    private Context mContext = this;
    private TabHost mTabHost;
    private TreeMap<String, TabInfo> mTabInfos = new TreeMap<String, TabInfo>();
    private TabInfo mLastTab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tab_fragment_activity);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
    }

    /**
     * Adds a tab to this activity with the given \c Fragment as the content and given title.
     */
    protected void addTab(final String title, Class<?> fragmentClass, Bundle args) {
        mTabInfos.put(title, new TabInfo(title, fragmentClass, args));

        View tabView = createTabButton(title);
        TabHost.TabSpec setContent = mTabHost.newTabSpec(title)
                                     .setIndicator(tabView)
                                     .setContent(new TabContentFactory());

        mTabHost.addTab(setContent);

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                TabInfo newTab = mTabInfos.get(tabId);
                changeTab(newTab, false);
            }
        });

        if (mTabInfos.size() == 1) {
            // if this is the first, select it
            changeTab(mTabInfos.get(title), true);
        }
    }

    public void reloadTab() {
        changeTab(mLastTab, true);
    }

    private void changeTab(TabInfo newTab, boolean force) {
        if (mLastTab != newTab || force) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (mLastTab != null) {
                if (mLastTab.fragment != null) {
                    ft.detach(mLastTab.fragment);
                }
            }
            if (newTab != null) {
                if (newTab.fragment == null) {
                    newTab.fragment = Fragment.instantiate(mContext,
                            newTab.fragmentClass.getName(), newTab.args);
                    ft.add(R.id.real_tabcontent, newTab.fragment, newTab.title);
                } else {
                    ft.attach(newTab.fragment);
                }
            }

            mLastTab = newTab;
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    /**
     * Creates the \c View that represents the actual tab button.
     */
    private View createTabButton(final String text) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.tab_button, null);
        TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(text);
        return view;
    }

    private class TabContentFactory implements TabHost.TabContentFactory {
        @Override
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumHeight(0);
            v.setMinimumWidth(0);
            return v;
        }
    }

    private static class TabInfo {
        public String title;
        public Class<?> fragmentClass;
        public Fragment fragment;
        public Bundle args;

        public TabInfo(String title, Class<?> fragmentClass, Bundle args) {
            this.title = title;
            this.fragmentClass = fragmentClass;
            this.args = args;
        }
    }
}
