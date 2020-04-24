package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.EmpireManager
import com.google.common.collect.ImmutableMap
import java.util.*

/** Handles chat messages and stuff. */
class ChatHandler : AdminHandler() {
  public override fun get() {
    val empireIds = DataStore.i.empires().search(null)
    val empires = ArrayList<Empire>()
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
