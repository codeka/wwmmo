package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.BuildRequest
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import java.util.*

class DebugBuildRequestsHandler : AdminHandler() {
  @Throws(RequestException::class)
  override fun get() {
    val data = TreeMap<String, Any>()

    // Similar to moving fleets, the simulation queue is what we'll use as a proxy for build
    // requests. A build request will, by necessity, cause a re-simulation, so these stars are where
    // the requests will be. We can just do a little bit of extra filtering.
    val buildRequestStars = HashMap<Long, Star>()
    val buildRequests = ArrayList<BuildRequest>()
    for (star in DataStore.i.stars().fetchSimulationQueue(100)) {
      for (planet in star.planets) {
        if (planet.colony == null) {
          continue
        }
        for (buildRequest in planet.colony.build_requests) {
          buildRequestStars[buildRequest.id] = star
          buildRequests.add(buildRequest)
        }
      }
    }

    /*
    </div>Designs.get(planets[i].colony.buildings[j].design_type);
     */data["build_requests"] = buildRequests
    data["request_stars"] = buildRequestStars
    render("debug/build-requests.html", data)
  }
}