package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.server.store.DataStore

/**
 * Handler for /admin/ajax/accounts, allowing various actions on accounts.
 */
class AjaxAccountsHandler : AjaxHandler() {
  public override fun post() {
    val empireId = request.getParameter("id").toLong()
    val pair = DataStore.i.accounts().getByEmpireId(empireId)
    if (pair == null) {
      setResponseText("No account for empire #$empireId")
      return
    }
    /*
    val account = pair.two

    when (request.getParameter("action")) {
      "resend" -> {
        AccountManager.i.sendVerificationEmail(account)
      }
      "force-verify" -> {
        AccountManager.i.verifyAccount(account)
      }
    }
    */
    setResponseText("success")
  }
}
