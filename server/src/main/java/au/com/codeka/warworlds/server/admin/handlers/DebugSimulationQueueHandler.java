package au.com.codeka.warworlds.server.admin.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

public class DebugSimulationQueueHandler extends AdminHandler {
  /** Any role can visit this page. */
  @Override
  protected Collection<AdminRole> getRequiredRoles() {
    return Arrays.asList(AdminRole.values());
  }

  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();
    data.put("stars", DataStore.i.stars().fetchSimulationQueue(100));
    render("debug/simulation-queue.html", data);
  }
}
