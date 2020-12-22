package au.com.codeka.warworlds.game.alliance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceActivity extends TabFragmentActivity {
  private Context context = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
/*TODO
    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(this, WelcomeFragment.class));
      } else {
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire.getAlliance() != null) {
          getTabManager().addTab(context, new TabInfo(AllianceActivity.this, "Overview",
              AllianceDetailsFragment.class, null));
        }

        getTabManager().addTab(context, new TabInfo(AllianceActivity.this, "Alliances",
            AllianceListFragment.class, null));

        if (myEmpire.getAlliance() != null) {
          Integer numPendingRequests = myEmpire.getAlliance().getNumPendingRequests();
          String pending = "";
          if (numPendingRequests != null && numPendingRequests > 0) {
            pending = " (<font color=\"red\">" + numPendingRequests + "</font>)";
          }
          getTabManager().addTab(context, new TabInfo(AllianceActivity.this,
              "Requests" + pending,
              RequestsFragment.class, null));
        }
      }
    });
 */
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

  private Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      MyEmpire myEmpire = EmpireManager.i.getEmpire();
      if (myEmpire.getID() == empire.getID()) {
        getTabManager().reloadTab();
      }
    }
  };
}
