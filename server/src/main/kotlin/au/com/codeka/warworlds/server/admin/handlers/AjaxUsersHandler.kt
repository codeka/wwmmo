package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore

/**
 * Handler for /admin/ajax/users which lets us modify some backend user stuff.
 */
class AjaxUsersHandler : AjaxHandler() {
  public override fun post() {
    when (request.getParameter("action")) {
      "delete" -> {
        val emailAddr = request.getParameter("email_addr")
        handleDeleteRequest(emailAddr)
      }
      else -> throw RequestException(400, "Unknown action: ${request.getParameter("action")}")
    }
  }

  private fun handleDeleteRequest(emailAddr: String) {
    DataStore.i.adminUsers().delete(emailAddr)
  }
}
