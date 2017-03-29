package au.com.codeka.warworlds.server.html.account;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest;
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse;
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.CookieHelper;
import au.com.codeka.warworlds.server.util.EmailHelper;
import au.com.codeka.warworlds.server.world.AccountManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * This servlet handles /accounts/associate, which is used to associate an account with an email
 * address.
 */
public class AccountAssociateHandler extends ProtobufRequestHandler {
  private static Log log = new Log("AccountAssociateHandler");

  @Override
  public void post() throws RequestException {
    AccountAssociateRequest req = readProtobuf(AccountAssociateRequest.class);

    Account account = DataStore.i.accounts().get(req.cookie);
    if (account == null) {
      log.warning("Could not associate account, no account for cookie: %s", req.cookie);
      getResponse().setStatus(401);
      return;
    }

    String emailAddr = req.email_addr;
    String canonicalEmailAddr = EmailHelper.canonicalizeEmailAddress(emailAddr);
    log.info("Attempting to associate empire #%d with '%s' (canonical: %s)",
        account.empire_id, emailAddr, canonicalEmailAddr);

    AccountAssociateResponse.Builder resp = new AccountAssociateResponse.Builder();

    // See if there's already one associated with this email address.
    Account existingAccount = DataStore.i.accounts().getByEmailAddr(canonicalEmailAddr);
    if (existingAccount != null) {
      if (req.force != null && req.force) {
        log.info("Un-associating existing account (empire: #%d) first...",
            existingAccount.empire_id);
        // TODO: don't do this until they've verified the email address...
      } else {
        log.info("Returning EMAIL_ALREADY_ASSOCIATED");
        resp.status(AccountAssociateResponse.AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED);
        writeProtobuf(resp.build());
        return;
      }
    }

    // If this account already has an email address
    if (account.email_canonical != null && !account.email_canonical.equals(canonicalEmailAddr)) {
      if (req.force != null && req.force) {
        log.info("Removing old email address from this account (%s) to add new one (%s)",
            account.email, emailAddr);
        // TODO: should we do this before they verify?
      } else {
        if (account.email_status == Account.EmailStatus.VERIFIED) {
          log.info("Returning ACCOUNT_ALREADY_ASSOCIATED");
          resp.status(AccountAssociateResponse.AccountAssociateStatus.ACCOUNT_ALREADY_ASSOCIATED);
          writeProtobuf(resp.build());
          return;
        } else {
          // it's not verified, so just overwrite it.
          log.info("Already associated with an unverified email, we'll just ignore that.");
        }
      }
    }

    String verificationCode = CookieHelper.generateCookie();

    log.info("Saving new account.");
    WatchableObject<Account> watchableAccount = AccountManager.i.getAccount(account.empire_id);
    if (watchableAccount == null) {
      log.error("Couldn't get account from store: %d", account.empire_id);
      resp.status(AccountAssociateResponse.AccountAssociateStatus.UNEXPECTED_ERROR);
      writeProtobuf(resp.build());
      return;
    }

    account = account.newBuilder()
        .email(emailAddr)
        .email_canonical(canonicalEmailAddr)
        .email_status(Account.EmailStatus.UNVERIFIED)
        .email_verification_code(verificationCode)
        .build();
    AccountManager.i.sendVerificationEmail(account);

    watchableAccount.set(account);
    resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS);
    writeProtobuf(resp.build());
  }
}
