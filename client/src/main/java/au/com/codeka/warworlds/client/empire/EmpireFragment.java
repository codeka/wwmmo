package au.com.codeka.warworlds.client.empire;

import android.os.Bundle;

import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.TabbedBaseFragment;
import au.com.codeka.warworlds.client.game.fleets.FleetsFragment;

/**
 * The empire fragment contains a tab view which lets you view all your fleets, colonies, etc.
 */
public class EmpireFragment extends TabbedBaseFragment {

  public static Bundle createArguments() {
    return new Bundle();
  }

  @Override
  protected void createTabs() {
    Bundle args = new Bundle();
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Colonies", EmptyFragment.class, args));

    args = new Bundle();
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Ships", FleetsFragment.class, args));

    args = new Bundle();
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Search", EmptyFragment.class, args));

    args = new Bundle();
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Settings", EmptyFragment.class, args));
  }

  public static class EmptyFragment extends BaseFragment {
  }
}
