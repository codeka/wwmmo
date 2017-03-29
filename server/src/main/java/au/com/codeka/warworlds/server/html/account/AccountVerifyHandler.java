package au.com.codeka.warworlds.server.html.account;

import com.google.common.base.Strings;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.html.HtmlRequestHandler;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.Pair;
import au.com.codeka.warworlds.server.world.AccountManager;

/**
 * This servlet handles /accounts/verify, which is used to verify an email address.
 */
public class AccountVerifyHandler extends HtmlRequestHandler {
  private static Log log = new Log("AccountAssociateHandler");

  @Override
  public void get() throws RequestException {
    String emailVerificationCode = getRequest().getParameter("code");
    if (Strings.isNullOrEmpty(emailVerificationCode)) {
      log.warning("No email verification code specified.");
      render("account/error-no-code.html", null);
      return;
    }

    Pair<String, Account> pair =
        DataStore.i.accounts().getByVerificationCode(emailVerificationCode);
    if (pair == null) {
      log.warning("No account found with verification code '%s'", emailVerificationCode);
      render("account/error-invalid-code.html", null);
      return;
    }

    AccountManager.i.getAccount(pair.two.empire_id).set(pair.two.newBuilder()
        .email_status(Account.EmailStatus.VERIFIED)
        .email_verification_code(null)
        .build());

    render("account/verified-success.html", null);
  }
}
