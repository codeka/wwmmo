package au.com.codeka.warworlds.client.activity;

import java.util.TreeMap;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import au.com.codeka.warworlds.client.R;

/**
 * An instance of this class can be used to manage tabs (e.g. in {@link TabbedBaseFragment} but
 * also in your own layouts.)
 */
public class TabManager {
  private TabHost tabHost;
  private TreeMap<String, TabInfo> tabInfos = new TreeMap<>();
  private TabInfo lastTab;

  public TabManager(TabHost tabHost) {
    this.tabHost = tabHost;
    this.tabHost.setup();
  }

  public void addTab(Context context, TabInfo tabInfo) {
    tabInfos.put(tabInfo.title, tabInfo);

    View tabView = createTabButton(context, tabInfo.title);
    TabHost.TabSpec setContent = tabHost.newTabSpec(tabInfo.title)
        .setIndicator(tabView)
        .setContent(tabInfo);

    tabHost.addTab(setContent);
    tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
      @Override
      public void onTabChanged(String tabId) {
        TabInfo newTab = tabInfos.get(tabId);
        changeTab(newTab, false);
      }
    });

    if (tabInfos.size() == 1) {
      // if this is the first, select it
      changeTab(tabInfos.get(tabInfo.title), true);
    }
  }

  public void reloadTab() {
    if (lastTab != null && lastTab.reload()) {
      return;
    }

    if (lastTab != null) {
      lastTab.recreate();
    }
  }

  private void changeTab(TabInfo newTab, boolean force) {
    if (lastTab != newTab || force) {
      if (newTab != null) {
        newTab.switchTo(lastTab);
        lastTab = newTab;
      }
    }
  }

  /**
   * Creates the {@link View} that represents the actual tab button.
   */
  private View createTabButton(Context context, final String text) {
    View view = LayoutInflater.from(context).inflate(R.layout.ctrl_tab_button, null);
    TextView tv = (TextView) view.findViewById(R.id.text);
    tv.setText(Html.fromHtml(text));
    return view;
  }

  /**
   * You'll need to subclass this to include whatever extra info about your tabs needed.
   */
  public static abstract class TabInfo implements TabHost.TabContentFactory {
    public String title;

    public TabInfo(String title) {
      this.title = title;
    }

    public abstract View createTabContent(String tag);

    public abstract void switchTo(TabInfo lastTab);
    public abstract void recreate();

    /** If possible, try to reload this tab, to avoid destroying it and recreating it. */
    public abstract boolean reload();
  }

  /**
   * Your tab's {@link Fragment} can implement this interface if you want to support reloading
   * without literally unloading and re-loading the whole tab.
   */
  public interface Reloadable {
    void reloadTab();
  }
}
