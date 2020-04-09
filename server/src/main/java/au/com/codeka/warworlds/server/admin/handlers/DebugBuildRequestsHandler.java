package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

public class DebugBuildRequestsHandler extends AdminHandler {
  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();

    // Similar to moving fleets, the simulation queue is what we'll use as a proxy for build
    // requests. A build request will, by necessity, cause a re-simulation, so these stars are where
    // the requests will be. We can just do a little bit of extra filtering.
    HashMap<Long, Star> buildRequestStars = new HashMap<>();
    ArrayList<BuildRequest> buildRequests = new ArrayList<>();
    for (Star star : DataStore.i.stars().fetchSimulationQueue(100)) {
      for (Planet planet : star.planets) {
        if (planet.colony == null) {
          continue;
        }

        for (BuildRequest buildRequest : planet.colony.build_requests) {
          buildRequestStars.put(buildRequest.id, star);
          buildRequests.add(buildRequest);
        }
      }
    }

    /*
    </div>Designs.get(planets[i].colony.buildings[j].design_type);
     */


    data.put("build_requests", buildRequests);
    data.put("request_stars", buildRequestStars);
    render("debug/build-requests.html", data);
  }
}
