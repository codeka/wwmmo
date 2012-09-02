package au.com.codeka.warworlds;

import java.util.TreeMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * An instance of this class can be used to manage tabs (e.g. in \c TabFragmentActivity but
 * also in your own layouts.)
 */
public class TabManager {
    private TabHost mTabHost;
    private TreeMap<String, TabInfo> mTabInfos = new TreeMap<String, TabInfo>();
    private TabInfo mLastTab;
    private boolean mSmallButtons;

    public TabManager(TabHost tabHost, boolean smallButtons) {
        mTabHost = tabHost;
        mSmallButtons = smallButtons;

        mTabHost.setup();
    }

    /**
     * Adds a tab to this activity with the given \c Fragment as the content and given title.
     */
    public void addTab(Context context, TabInfo tabInfo) {
        mTabInfos.put(tabInfo.title, tabInfo);

        View tabView = createTabButton(context, tabInfo.title);
        TabHost.TabSpec setContent = mTabHost.newTabSpec(tabInfo.title)
                                     .setIndicator(tabView)
                                     .setContent(tabInfo);

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
            changeTab(mTabInfos.get(tabInfo.title), true);
        }
    }

    public void reloadTab() {
        changeTab(mLastTab, true);
    }

    private void changeTab(TabInfo newTab, boolean force) {
        if (mLastTab != newTab || force) {
            if (newTab != null) {
                newTab.switchTo(mLastTab);
                mLastTab = newTab;
            }
        }
    }

    /**
     * Creates the \c View that represents the actual tab button.
     */
    private View createTabButton(Context context, final String text) {
        View view;
        if (mSmallButtons) {
            view = LayoutInflater.from(context).inflate(R.layout.tab_button_small, null);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.tab_button, null);
        }

        TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(text);
        return view;
    }

    /**
     * You'll need to subclass this to include whatever extra info about your tabs
     * needed.
     */
    public static abstract class TabInfo implements TabHost.TabContentFactory {
        public String title;

        public TabInfo(String title) {
            this.title = title;
        }

        public abstract View createTabContent(String tag);

        public abstract void switchTo(TabInfo lastTab);
    }
}
