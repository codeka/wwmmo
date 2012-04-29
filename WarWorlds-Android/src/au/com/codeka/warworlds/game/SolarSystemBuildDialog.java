package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony.
 * @author dean@codeka.com.au
 *
 */
public class SolarSystemBuildDialog extends Dialog {
    private SolarSystemActivity mActivity;
    private TabManager mTabManager;

    public SolarSystemBuildDialog(SolarSystemActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_build_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        mTabManager = new TabManager(tabHost);
        mTabManager.addTab(new SolarSystemBuildBuildingTab(this, mActivity));
        mTabManager.addTab(new SolarSystemBuildQueueTab(this, mActivity));
    }

    public void setColony(Colony colony) {
        for (Tab tab : mTabManager.getTabs()) {
            tab.setColony(colony);
        }
    }

    /**
     * This needs to be implemented by the classes that implement the tabs on this dialog, so
     * we can communicate with them.
     * @author dean@codeka.com.au
     *
     */
    public interface Tab {
        public View getView();
        public String getTitle();
        public void setColony(Colony colony);
    }

    private class TabManager implements TabHost.TabContentFactory {
        private TabHost mTabHost;
        private ArrayList<Tab> mTabs;

        public TabManager(TabHost tabHost) {
            mTabHost = tabHost;
            mTabs = new ArrayList<Tab>();
        }

        public void addTab(Tab tab) {
            mTabs.add(tab);
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(Integer.toString(mTabs.size() - 1))
                                              .setIndicator(tab.getTitle())
                                              .setContent(this);
            mTabHost.addTab(tabSpec);

            // This is really ugly, but I can't find a better way to reduce the height of the tabs
            // which really are too high in landscape mode...
            // http://stackoverflow.com/questions/8271385/can-i-use-default-tab-styling-in-my-custom-tab-view

            TabWidget tw = (TabWidget) findViewById(android.R.id.tabs);
            RelativeLayout indicator = (RelativeLayout) tw.getChildTabViewAt(mTabs.size() - 1);
            indicator.findViewById(android.R.id.icon).setVisibility(View.GONE);

            ViewGroup.LayoutParams vglp = indicator.getLayoutParams();
            vglp.height = 40;
            indicator.setLayoutParams(vglp);
        }

        public List<Tab> getTabs() {
            return mTabs;
        }

        @Override
        public View createTabContent(String tag) {
            int tabIndex = Integer.parseInt(tag);
            Tab tab = mTabs.get(tabIndex);
            return tab.getView();
        }
    }
}
