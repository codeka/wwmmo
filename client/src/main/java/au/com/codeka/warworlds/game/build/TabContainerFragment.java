package au.com.codeka.warworlds.game.build;

import android.app.Activity;
import android.os.Bundle;

import com.google.common.base.Preconditions;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/** The main fragment that just contains the build tabs.*/
public class TabContainerFragment extends TabFragmentFragment {
  @Override
  protected void createTabs() {
    Activity activity = Preconditions.checkNotNull(requireActivity());
    Bundle bundleArgs = requireArguments();

    getTabManager().addTab(
        activity, new TabInfo(this, "Buildings", BuildingsFragment.class, bundleArgs));
    getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, bundleArgs));
    getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, bundleArgs));

    // If this colony has a shipyard, switch to the ships tab by default.
    BuildFragmentArgs args = BuildFragmentArgs.fromBundle(bundleArgs);
    Star star = StarManager.i.getStar(args.getStarID());
    if (star == null) {
      // TODO: wait for it to refresh?
      return;
    }

    for (BaseColony baseColony : star.getColonies()) {
      if (baseColony.getPlanetIndex() == args.getPlanetIndex()) {
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
