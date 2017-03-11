package au.com.codeka.warworlds.client.game.empire;

import android.os.Bundle;

import au.com.codeka.warworlds.client.activity.TabbedBaseFragment;
import au.com.codeka.warworlds.client.game.fleets.FleetsFragment;
import au.com.codeka.warworlds.common.Log;

/**
 * This fragment shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireFragment extends TabbedBaseFragment {
  private static final Log log = new Log("EmpireFragment");

  public static Bundle createArguments() {
    Bundle bundle = new Bundle();
    return bundle;
  }

  @Override
  protected void createTabs() {
    Bundle args = getArguments();
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Overview", OverviewFragment.class, new Bundle()));
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Colonies", ColoniesFragment.class, new Bundle()));
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Build", BuildQueueFragment.class, new Bundle()));
    getTabManager().addTab(getContext(),
        new TabInfo(this, "Fleets", FleetsFragment.class, new Bundle()));
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
}