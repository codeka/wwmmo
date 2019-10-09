package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

public class DebugMovingFleetsHandler extends AdminHandler {
  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();

    // The simulation queue is what we'll use as a proxy for moving fleets. A moving fleet will, by
    // necessity, cause a re-simulation, so these stars are where the fleets will be. We can just do
    // a little bit of extra filtering.
    HashMap<Long, Star> stars = new HashMap<>();
    HashMap<Long, Star> fleetStars = new HashMap<>();
    ArrayList<Fleet> fleets = new ArrayList<>();
    for (Star star : DataStore.i.stars().fetchSimulationQueue(100)) {
      stars.put(star.id, star);

      for (Fleet fleet : star.fleets) {
        if (fleet.state == Fleet.FLEET_STATE.MOVING) {
          fleets.add(fleet);
          fleetStars.put(fleet.id, star);
        }
      }
    }

    // Make sure the destination stars are in the list as well (they won't necessarily be there)
    for (Fleet fleet : fleets) {
      if (!stars.containsKey(fleet.destination_star_id)) {
        Star dest = DataStore.i.stars().get(fleet.destination_star_id);
        if (dest != null) {
          stars.put(dest.id, dest);
        }
      }
    }


    data.put("fleets", fleets);
    data.put("stars", stars);
    data.put("fleet_stars", fleetStars);
    render("debug/moving-fleets.html", data);
  }
}
