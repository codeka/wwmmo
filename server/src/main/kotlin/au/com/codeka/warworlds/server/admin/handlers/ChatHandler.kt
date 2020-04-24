package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.EmpireManager
import au.com.codeka.warworlds.server.world.WatchableObject
import com.google.common.collect.ImmutableMap
import java.util.*

/** Handles chat messages and stuff. */
class ChatHandler : AdminHandler() {
  public override fun get() {
    val empireIds: List<Long> = DataStore.i.empires().search(null)
    val empires: MutableList<Empire?> = ArrayList()
    for (id in empireIds) {
      val empire = EmpireManager.i.getEmpire(id)
      if (empire != null) {
        empires.add(empire.get())
      }
    }
    render("chat/index.html", ImmutableMap.builder<String, Any>()
        .put("empires", empires)
        .build())
  }
}