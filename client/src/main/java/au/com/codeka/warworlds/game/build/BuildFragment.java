package au.com.codeka.warworlds.game.build;

import android.os.Bundle;

import com.google.common.base.Preconditions;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;

public class BuildFragment extends TabFragmentFragment {
  @Override
  protected void createTabs() {
    BuildActivity activity = Preconditions.checkNotNull((BuildActivity) getActivity());
    Bundle args = requireArguments();

    getTabManager().addTab(activity, new TabInfo(this, "Buildings", BuildingsFragment.class, args));
    getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, args));
    getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, args));

    // If this colony has a shipyard, switch to the ships tab by default.
    Star star = activity.getStar();
    String colonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
    for (BaseColony baseColony : star.getColonies()) {
      if (baseColony.getKey().equals(colonyKey)) {
        Colony colony = (Colony) baseColony;
        for (BaseBuilding building : colony.getBuildings()) {
          if (building.getDesignID().equals("shipyard")) {
            getTabHost().setCurrentTab(1);
          }
        }
      }
    }
  }
}
