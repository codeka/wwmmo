package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.CookieHelper
import au.com.codeka.warworlds.server.util.EmailHelper
import au.com.codeka.warworlds.server.world.AccountManager
import au.com.codeka.warworlds.server.world.WatchableObject
import com.google.common.base.Preconditions

/**
 * This servlet handles /accounts/associate, which is used to associate an account with an email
 * address.
 */
class AccountAssociateHandler : ProtobufRequestHandler() {
  /** Get is for checking whether the associate has succeeded.  */
  @Throws(RequestException::class)
  public override fun get() {
    val empireId = request.getParameter("id").toLong()
    val account = AccountManager.i.getAccount(empireId)
    if (account == null) {
      log.warning("Could not associate account, no account for empire: %d", empireId)
      response.status = 401
      return
    }
    if (account.get().email_status == Account.EmailStatus.VERIFIED) {
      log.info("Account is verified!")
      writeProtobuf(AccountAssociateResponse.Builder()
          .status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS)
          .build())
    } else {
      log.info("Account not verified: %s", account.get().email_status)
      writeProtobuf(AccountAssociateResponse.Builder()
          .status(AccountAssociateResponse.AccountAssociateStatus.NOT_VERIFIED)
          .build())
    }
  }

  /** Post is to actually initiate an association.  */
  @Throws(RequestException::class)
  public override fun post() {
    val req = readProtobuf(AccountAssociateRequest::class.java)
    val acc = DataStore.i.accounts()[req.cookie]
    if (acc == null) {
      log.warning("Could not associate account, no account for cookie: %s", req.cookie)
      response.status = 401
      return
    }
    val account = AccountManager.i.getAccount(acc.empire_id)
        ?: throw RequestException(404, "Could not load account for empire.")
    val emailAddr = req.email_addr
    val canonicalEmailAddr = EmailHelper.canonicalizeEmailAddress(emailAddr)
    log.info("Attempting to associate empire #%d with '%s' (canonical: %s)",
        account.get().empire_id, emailAddr, canonicalEmailAddr)
    val resp = AccountAssociateResponse.Builder()

    // See if there's already one associated with this email address.
    val existingAccount = DataStore.i.accounts().getByVerifiedEmailAddr(canonicalEmailAddr)
    if (existingAccount != null) {
      if (req.force != null && req.force) {
        log.info("We're forcing this association (empire: #%d email=%s)...",
            existingAccount.empire_id, existingAccount.email_canonical)
      } else {
        log.info("Returning EMAIL_ALREADY_ASSOCIATED")
        resp.status(AccountAssociateResponse.AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED)
        writeProtobuf(resp.build())
        return
      }
    }

    // If this account already has an email address
    if (account.get().email_canonical != null
        && account.get().email_canonical != canonicalEmailAddr) {
      if (req.force != null && req.force) {
        log.info("Removing old email address from this account (%s) to add new one (%s)",
            account.get().email, emailAddr)
        // TODO: should we do this before they verify?
      } else {
        if (account.get().email_status == Account.EmailStatus.VERIFIED) {
          log.info("Returning ACCOUNT_ALREADY_ASSOCIATED")
          resp.status(AccountAssociateResponse.AccountAssociateStatus.ACCOUNT_ALREADY_ASSOCIATED)
          writeProtobuf(resp.build())
          return
        } else {
          // it's not verified, so just overwrite it.
          log.info("Already associated with an unverified email, we'll just ignore that.")
        }
      }
    }
    val verificationCode = CookieHelper.generateCookie()
    log.info("Saving new account.")
    account.set(account.get().newBuilder()
        .email(emailAddr)
        .email_canonical(canonicalEmailAddr)
        .email_status(Account.EmailStatus.UNVERIFIED)
        .email_verification_code(verificationCode)
        .build())
    AccountManager.i.sendVerificationEmail(account.get())
    resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS)
    writeProtobuf(resp.build())
  }

  companion object {
    private val log = Log("AccountAssociateHandler")
  }
}