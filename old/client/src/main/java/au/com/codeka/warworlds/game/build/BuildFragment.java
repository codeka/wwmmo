package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import au.com.codeka.warworlds.TabFragmentFragment;

public class BuildFragment extends TabFragmentFragment {
  @Override
  protected void createTabs() {
    BuildActivity activity = (BuildActivity) getActivity();
    Bundle args = getArguments();

    getTabManager().addTab(activity, new TabInfo(this, "Buildings", BuildingsFragment.class, args));
    getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, args));
    getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, args));
  }
}
