package au.com.codeka.warworlds.game.empire;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.ImagePickerHelper;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.model.EmpireManager;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireFragment extends TabFragmentFragment {
  private static final Log log = new Log("EmpireActivity");
  boolean firstRefresh = true;
  boolean firstStarsRefresh = true;

  @Nullable private EmpireFragmentArgs args;

  public enum EmpireActivityResult {
    NavigateToPlanet(1),
    NavigateToFleet(2);

    private int mValue;

    public static EmpireActivityResult fromValue(int value) {
      for (EmpireActivityResult res : values()) {
        if (res.mValue == value) {
          return res;
        }
      }

      throw new IllegalArgumentException("value is not a valid EmpireActivityResult");
    }

    public int getValue() {
      return mValue;
    }

    EmpireActivityResult(int value) {
      mValue = value;
    }
  }

  @Override
  protected void createTabs() {
    if (getArguments() != null) {
      args = EmpireFragmentArgs.fromBundle(getArguments());
    }

    TabManager tabManager = getTabManager();
    tabManager.addTab(
        requireContext(), new TabInfo(this, "Overview", OverviewFragment.class, null));
    tabManager.addTab(
        requireContext(), new TabInfo(this, "Colonies", ColoniesFragment.class, null));
    tabManager.addTab(requireContext(), new TabInfo(this, "Build", BuildQueueFragment.class, null));

    Integer fleetID = null;
    if (args != null) {
      fleetID = args.getFleetID();
      if (fleetID <= 0) {
        fleetID = null;
      }
    }

    tabManager.addTab(
        requireContext(), new TabInfo(this, "Fleets", FleetsFragment.class, getArguments()));
    tabManager.addTab(
        requireContext(), new TabInfo(this, "Settings", SettingsFragment.class, null));

    if (fleetID != null) {
      getTabHost().setCurrentTabByTag("Fleets");
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    ServerGreeter.waitForHello(requireActivity(), (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(requireContext(), WelcomeFragment.class));
        return;
      }

      // make sure we have a current empire
      EmpireManager.i.refreshEmpire();
    });
  }
}
