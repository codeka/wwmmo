package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import au.com.codeka.warworlds.server.world.AccountManager

/**
 * Handler for /admin/ajax/accounts, allowing various actions on accounts.
 */
class AjaxAccountsHandler : AjaxHandler() {
  @Throws(RequestException::class)
  public override fun post() {
    if (request.getParameter("action") == "resend") {
      val empireId = request.getParameter("id").toLong()
      val pair = DataStore.i.accounts().getByEmpireId(empireId)
      if (pair == null) {
        setResponseText("No account for empire #$empireId")
        return
      }
      AccountManager.i.sendVerificationEmail(pair.two)
      setResponseText("success")
    }
  }
}