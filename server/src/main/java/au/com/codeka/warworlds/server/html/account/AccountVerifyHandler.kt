package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.html.HtmlRequestHandler
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import au.com.codeka.warworlds.server.world.AccountManager
import com.google.common.base.Strings

/**
 * This servlet handles /accounts/verify, which is used to verify an email address.
 */
class AccountVerifyHandler : HtmlRequestHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    val emailVerificationCode = request.getParameter("code")
    if (Strings.isNullOrEmpty(emailVerificationCode)) {
      log.warning("No email verification code specified.")
      render("account/error-no-code.html", null)
      return
    }
    val pair: Pair<String?, Account>? = DataStore.i.accounts().getByVerificationCode(emailVerificationCode)
    if (pair == null) {
      log.warning("No account found with verification code '%s'", emailVerificationCode)
      render("account/error-invalid-code.html", null)
      return
    }

    // If there's already an associated account with this email address, mark it as abandoned now.
    while (true) {
      val existingAccount: Account? = DataStore.i.accounts().getByVerifiedEmailAddr(pair.two!!.email_canonical)
      if (existingAccount != null) {
        AccountManager.Companion.i.getAccount(existingAccount.empire_id)!!.set(
            existingAccount.newBuilder()
                .email_status(Account.EmailStatus.ABANDONED)
                .build())
      } else {
        break
      }
    }
    AccountManager.i.getAccount(pair.two!!.empire_id)!!.set(pair.two!!.newBuilder()
        .email_status(Account.EmailStatus.VERIFIED)
        .email_verification_code(null)
        .build())
    render("account/verified-success.html", null)
  }

  companion object {
    private val log = Log("AccountAssociateHandler")
  }
}