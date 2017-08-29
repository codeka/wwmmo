package au.com.codeka.warworlds.server.html.account;

import static com.google.common.base.Preconditions.checkNotNull;

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

  /** Get is for checking whether the associate has succeeded. */
  @Override
  public void get() throws RequestException {
    long empireId = Long.parseLong(getRequest().getParameter("id"));
    WatchableObject<Account> account = checkNotNull(AccountManager.i.getAccount(empireId));
    if (account == null) {
      log.warning("Could not associate account, no account for empire: %d", empireId);
      getResponse().setStatus(401);
      return;
    }
    if (account.get().email_status == Account.EmailStatus.VERIFIED) {
      log.info("Account is verified!");
      writeProtobuf(new AccountAssociateResponse.Builder()
          .status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS)
          .build());
    } else {
      log.info("Account not verified: %s", account.get().email_status);
      writeProtobuf(new AccountAssociateResponse.Builder()
          .status(AccountAssociateResponse.AccountAssociateStatus.NOT_VERIFIED)
          .build());
    }
  }

  /** Post is to actually initiate an association. */
  @Override
  public void post() throws RequestException {
    AccountAssociateRequest req = readProtobuf(AccountAssociateRequest.class);

    Account acc = DataStore.i.accounts().get(req.cookie);
    if (acc == null) {
      log.warning("Could not associate account, no account for cookie: %s", req.cookie);
      getResponse().setStatus(401);
      return;
    }
    WatchableObject<Account> account = checkNotNull(AccountManager.i.getAccount(acc.empire_id));

    String emailAddr = req.email_addr;
    String canonicalEmailAddr = EmailHelper.canonicalizeEmailAddress(emailAddr);
    log.info("Attempting to associate empire #%d with '%s' (canonical: %s)",
        account.get().empire_id, emailAddr, canonicalEmailAddr);

    AccountAssociateResponse.Builder resp = new AccountAssociateResponse.Builder();

    // See if there's already one associated with this email address.
    Account existingAccount = DataStore.i.accounts().getByVerifiedEmailAddr(canonicalEmailAddr);
    if (existingAccount != null) {
      if (req.force != null && req.force) {
        log.info("We're forcing this association (empire: #%d email=%s)...",
            existingAccount.empire_id, existingAccount.email_canonical);
      } else {
        log.info("Returning EMAIL_ALREADY_ASSOCIATED");
        resp.status(AccountAssociateResponse.AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED);
        writeProtobuf(resp.build());
        return;
      }
    }

    // If this account already has an email address
    if (account.get().email_canonical != null
        && !account.get().email_canonical.equals(canonicalEmailAddr)) {
      if (req.force != null && req.force) {
        log.info("Removing old email address from this account (%s) to add new one (%s)",
            account.get().email, emailAddr);
        // TODO: should we do this before they verify?
      } else {
        if (account.get().email_status == Account.EmailStatus.VERIFIED) {
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
    account.set(account.get().newBuilder()
        .email(emailAddr)
        .email_canonical(canonicalEmailAddr)
        .email_status(Account.EmailStatus.UNVERIFIED)
        .email_verification_code(verificationCode)
        .build());
    AccountManager.i.sendVerificationEmail(account.get());

    resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS);
    writeProtobuf(resp.build());
  }
}
