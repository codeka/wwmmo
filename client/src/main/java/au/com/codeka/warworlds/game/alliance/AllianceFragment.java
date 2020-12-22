package au.com.codeka.warworlds.game.alliance;

import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceFragment extends TabFragmentFragment {

  @Override
  protected void createTabs() {
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (myEmpire.getAlliance() != null) {
      getTabManager().addTab(
          requireContext(), new TabInfo(this, "Overview", AllianceDetailsFragment.class, null));
    }

    getTabManager().addTab(
        requireContext(), new TabInfo(this, "Alliances", AllianceListFragment.class, null));

    if (myEmpire.getAlliance() != null) {
      Integer numPendingRequests = myEmpire.getAlliance().getNumPendingRequests();
      String pending = "";
      if (numPendingRequests != null && numPendingRequests > 0) {
        pending = " (<font color=\"red\">" + numPendingRequests + "</font>)";
      }
      getTabManager().addTab(
          requireContext(), new TabInfo(this, "Requests" + pending, RequestsFragment.class, null));
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    EmpireManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      MyEmpire myEmpire = EmpireManager.i.getEmpire();
      if (myEmpire.getID() == empire.getID()) {
        getTabManager().reloadTab();
      }
    }
  };
}
