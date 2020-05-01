package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.AccountManager

/**
 * This servlet handles /accounts/associate, which is used to associate an account with an email
 * address.
 */
class AccountAssociateHandler : ProtobufRequestHandler() {
  companion object {
    private val log = Log("AccountAssociateHandler")
  }

  /** Get is for checking whether the associate has succeeded.  */
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
    val tokenInfo = TokenVerifier.verify(req.id_token)
    if (tokenInfo.email != req.email_addr) {
      log.warning("Email address from token is not the same as the one in the request " +
          "(token email=${tokenInfo.email}, request email=${req.email_addr})")
      throw RequestException(400, "Invalid email address")
    }

    val emailAddr = req.email_addr
    log.info(
        "Attempting to associate empire #%d with '%s', name=%s audience: %s",
        account.get().empire_id, emailAddr, tokenInfo.displayName, tokenInfo.audience)
    val resp = AccountAssociateResponse.Builder()

    // See if there's already one associated with this email address.
    val existingAccount = DataStore.i.accounts().getByVerifiedEmailAddr(emailAddr)
    if (existingAccount != null) {
      if (req.force != null && req.force) {
        log.info("We're forcing this association (empire: #%d email=%s)...",
            existingAccount.empire_id, existingAccount.email)
      } else {
        log.info("Returning EMAIL_ALREADY_ASSOCIATED")
        resp.status(AccountAssociateResponse.AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED)
        writeProtobuf(resp.build())
        return
      }
    }

    // If this account already has an email address
    if (account.get().email != null && account.get().email != emailAddr) {
      if (req.force != null && req.force) {
        log.info("Removing old email address from this account (%s) to add new one (%s)",
            account.get().email, emailAddr)
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

    log.info("Updating account with email address.")
    account.set(account.get().newBuilder()
        .email(emailAddr)
        .email_status(Account.EmailStatus.VERIFIED)
        .build())
    resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS)
    writeProtobuf(resp.build())
  }
}
