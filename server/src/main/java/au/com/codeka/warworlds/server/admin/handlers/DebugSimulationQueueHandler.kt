package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import java.util.*

class DebugSimulationQueueHandler : AdminHandler() {
  /** Any role can visit this page.  */
  override val requiredRoles: Collection<AdminRole>?
    get() = listOf(*AdminRole.values())

  @Throws(RequestException::class)
  override fun get() {
    val data = TreeMap<String, Any>()
    data["stars"] = DataStore.i.stars().fetchSimulationQueue(100)
    render("debug/simulation-queue.html", data)
  }
}