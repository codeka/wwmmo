package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Handles chat messages and stuff.
 */
public class ChatHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    List<Long> empireIds = DataStore.i.empires().search(null);
    List<Empire> empires = new ArrayList<>();

    for (Long id : empireIds) {
      WatchableObject<Empire> empire = EmpireManager.i.getEmpire(id);
      if (empire != null) {
        empires.add(empire.get());
      }
    }

    render("chat/index.html", ImmutableMap.<String, Object>builder()
        .put("empires", empires)
        .build());
  }
}
