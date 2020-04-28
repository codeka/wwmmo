package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.EmpireManager

/** Handler for /admin/ajax/empires/&lt;id&gt;  */
class AjaxEmpireHandler : AdminHandler() {
  public override fun get() {
    if (request.getParameter("id") != null) {
      val empireId = request.getParameter("id").toLong()
      val empire = EmpireManager.i.getEmpire(empireId) ?: throw RequestException(404)
      setResponseJson(empire.get())
    }
  }
}
