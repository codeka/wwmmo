package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.server.html.HtmlRequestHandler
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import au.com.codeka.warworlds.server.world.AccountManager
import com.google.common.base.Strings

/**
 * This servlet handles /accounts/verify, which is used to verify an email address.
 */
class AccountVerifyHandler : HtmlRequestHandler() {
  companion object {
    private val log = Log("AccountAssociateHandler")
  }

  public override fun get() {
    val emailVerificationCode = request.getParameter("code")
    if (Strings.isNullOrEmpty(emailVerificationCode)) {
      log.warning("No email verification code specified.")
      render("account/error-no-code.html", null)
      return
    }
    val pair = DataStore.i.accounts().getByVerificationCode(emailVerificationCode)
    if (pair == null) {
      log.warning("No account found with verification code '%s'", emailVerificationCode)
      render("account/error-invalid-code.html", null)
      return
    }

    AccountManager.i.verifyAccount(pair.two)
    render("account/verified-success.html", null)
  }
}
