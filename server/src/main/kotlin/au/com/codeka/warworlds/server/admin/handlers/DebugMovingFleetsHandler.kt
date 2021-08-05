package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import java.util.*

class DebugMovingFleetsHandler : AdminHandler() {
  override fun get() {
    val data = TreeMap<String, Any>()

    // The simulation queue is what we'll use as a proxy for moving fleets. A moving fleet will, by
    // necessity, cause a re-simulation, so these stars are where the fleets will be. We can just do
    // a little bit of extra filtering.
    val stars = HashMap<Long, Star>()
    val fleetStars = HashMap<Long, Star>()
    val fleets = ArrayList<Fleet>()
    for (star in DataStore.i.stars().fetchSimulationQueue(100)) {
      stars[star.id] = star
      for (fleet in star.fleets) {
        if (fleet.state == Fleet.FLEET_STATE.MOVING) {
          fleets.add(fleet)
          fleetStars[fleet.id] = star
        }
      }
    }

    // Make sure the destination stars are in the list as well (they won't necessarily be there)
    for (fleet in fleets) {
      if (!stars.containsKey(fleet.destination_star_id)) {
        val dest = DataStore.i.stars()[fleet.destination_star_id!!]
        if (dest != null) {
          stars[dest.id] = dest
        }
      }
    }
    data["fleets"] = fleets
    data["stars"] = stars
    data["fleet_stars"] = fleetStars
    render("debug/moving-fleets.html", data)
  }
}