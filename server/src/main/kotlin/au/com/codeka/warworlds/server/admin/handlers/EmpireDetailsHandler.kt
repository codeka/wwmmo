package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.DesignDefinitions
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.proto.PatreonInfo
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.NotificationManager
import au.com.codeka.warworlds.server.world.StarManager
import au.com.codeka.warworlds.server.world.WatchableObject
import java.util.*

/**
 * Handler for /admin/empires/xxx which shows details about the empire with id xxx.
 */
class EmpireDetailsHandler : AdminHandler() {
  public override fun get() {
    val id = getUrlParameter("id")!!.toLong()
    val empire: Empire = DataStore.i.empires()[id] ?: throw RequestException(404)

    val data = HashMap<String, Any>()
    data["empire"] = empire

    when (request.getParameter("tab")) {
      "stars" -> {
        completeStarsTab(empire, data)
      }
      "devices" -> {
        completeDevicesTab(empire, data)
      }
      "sit-reports" -> {
        completeSitReportsTab(empire, data)
      }
      else -> {
        completeOverview(empire, data)
      }
    }
  }

  public override fun post() {
    val id = getUrlParameter("id")!!.toLong()
    val empire: Empire = DataStore.i.empires()[id] ?: throw RequestException(404)
    val msg = request.getParameter("msg")
    if (msg.isEmpty()) {
      val data = HashMap<String, Any>()
      data["empire"] = empire
      data["error"] = "You need to specify a message."
      completeOverview(empire, data)
      return
    }

    // TODO: send it
    NotificationManager.i.sendNotification(empire, Notification(debug_message = msg))
    redirect("/admin/empires/$id")
  }

  private fun completeOverview(empire: Empire, data: HashMap<String, Any>) {
    data["account"] = DataStore.i.accounts().getByEmpireId(empire.id)!!.two

    val patreonInfo: PatreonInfo? = DataStore.i.empires().getPatreonInfo(empire.id)
    if (patreonInfo != null) {
      data["patreon"] = patreonInfo
    }

    render("empires/details.html", data)
  }

  private fun completeStarsTab(empire: Empire, data: HashMap<String, Any>) {
    val stars = ArrayList<Star?>()
    for (starId in DataStore.i.stars().getStarsForEmpire(empire.id)) {
      val star: WatchableObject<Star>? = StarManager.i.getStar(starId)
      if (star != null) {
        stars.add(star.get())
      }
    }
    data["stars"] = stars

    render("empires/details-stars.html", data)
  }

  private fun completeDevicesTab(empire: Empire, data: HashMap<String, Any>) {
    data["devices"] = DataStore.i.empires().getDevicesForEmpire(empire.id)
    render("empires/details-devices.html", data)
  }

  private fun completeSitReportsTab(empire: Empire, data: HashMap<String, Any>) {
    val sitReports = DataStore.i.sitReports().getByEmpireId(empire.id, 50)
    data["sitReports"] = sitReports

    val designs = HashMap<String, Design>()
    for (design in DesignDefinitions.designs.designs) {
      designs[design.type.name] = design
    }
    data["designs"] = designs

    val stars = HashMap<Long, Star>()
    for (sitReport in sitReports) {
      if (stars.containsKey(sitReport.star_id)) {
        continue
      }

      val star = StarManager.i.getStar(sitReport.star_id)
      if (star != null) {
        stars[sitReport.star_id] = star.get()
      }
    }
    data["stars"] = stars

    render("empires/details-sitreports.html", data)
  }
}
