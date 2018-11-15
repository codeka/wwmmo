package au.com.codeka.warworlds.server.admin.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.proto.SuspiciousEvent;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.SuspiciousEventManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Handler for /debug/suspicious-events.
 */
public class DebugSuspiciousEventsHandler extends AdminHandler  {
  /** Any role can visit this page. */
  @Override
  protected Collection<AdminRole> getRequiredRoles() {
    return Arrays.asList(AdminRole.values());
  }

  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();

    Collection<SuspiciousEvent> events = SuspiciousEventManager.i.query();
    data.put("events", events);

    HashMap<Long, Empire> empires = new HashMap<>();
    for (SuspiciousEvent event : events) {
      if (!empires.containsKey(event.modification.empire_id)) {
        WatchableObject<Empire> empire = EmpireManager.i.getEmpire(event.modification.empire_id);
        if (empire != null) {
          empires.put(
              event.modification.empire_id,
              empire.get());
        }
      }
    }
    data.put("empires", empires);

    HashMap<Long, Star> stars = new HashMap<>();
    for (SuspiciousEvent event : events) {
      if (!empires.containsKey(event.star_id)) {
        WatchableObject<Star> star = StarManager.i.getStar(event.star_id);
        if (star != null) {
          stars.put(
              event.star_id,
              star.get());
        }
      }
    }
    data.put("stars", stars);

    render("debug/suspicious-events.html", data);
  }
}
