package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.EmpireManager
import au.com.codeka.warworlds.server.world.StarManager
import au.com.codeka.warworlds.server.world.SuspiciousEventManager
import java.util.*

/**
 * Handler for /debug/suspicious-events.
 */
class DebugSuspiciousEventsHandler : AdminHandler() {
  /** Any role can visit this page.  */
  override val requiredRoles: Collection<AdminRole>?
    get() = listOf(*AdminRole.values())

  @Throws(RequestException::class)
  override fun get() {
    val data = TreeMap<String, Any>()
    val events = SuspiciousEventManager.i.query()
    data["events"] = events
    val empires = HashMap<Long, Empire?>()
    for (event in events) {
      if (!empires.containsKey(event.modification.empire_id)) {
        val empire = EmpireManager.i.getEmpire(event.modification.empire_id)
        if (empire != null) {
          empires[event.modification.empire_id] = empire.get()
        }
      }
    }
    data["empires"] = empires
    val stars = HashMap<Long, Star?>()
    for (event in events) {
      if (!empires.containsKey(event.star_id)) {
        val star = StarManager.i.getStar(event.star_id)
        if (star != null) {
          stars[event.star_id] = star.get()
        }
      }
    }
    data["stars"] = stars
    render("debug/suspicious-events.html", data)
  }
}