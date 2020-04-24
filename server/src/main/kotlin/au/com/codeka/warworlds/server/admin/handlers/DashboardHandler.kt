package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.proto.DailyStat
import au.com.codeka.warworlds.server.store.DataStore
import org.joda.time.DateTime
import java.util.*

class DashboardHandler : AdminHandler() {
  /** Any role can visit this page.  */
  override val requiredRoles: Collection<AdminRole>?
    get() = listOf(*AdminRole.values())

  override fun get() {
    val data = TreeMap<String, Any>()
    val loginEvents = DataStore.i.stats().getRecentLogins(10)
    data["loginEvents"] = loginEvents
    val empires = HashMap<Long, Empire?>()
    for (loginEvent in loginEvents) {
      if (!empires.containsKey(loginEvent.empire_id)) {
        empires[loginEvent.empire_id] = DataStore.i.empires()[loginEvent.empire_id]
      }
    }
    data["empires"] = empires
    var dt = DateTime.now().minusDays(60)
    val dailyStats: Map<Int, DailyStat> = DataStore.i.stats().getDailyStats(60)
    val graph: MutableList<DailyStat> = ArrayList<DailyStat>()
    for (i in 0..60) {
      val day = dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get()
      var stat = dailyStats[day]
      if (stat == null) {
        stat = DailyStat.Builder().day(day).oneda(0).sevenda(0).signups(0).build()
      }
      graph.add(stat!!)
      dt = dt.plusDays(1)
    }
    data["graph"] = graph
    render("index.html", data)
  }
}