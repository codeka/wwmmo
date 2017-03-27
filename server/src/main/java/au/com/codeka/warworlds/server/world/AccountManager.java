package au.com.codeka.warworlds.server.world;

import org.simplejavamail.email.Email;

import javax.mail.Message;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.util.SmtpHelper;

/**
 * Manages {@link Account}s.
 */
public class AccountManager {
  public static final AccountManager i = new AccountManager();
  private static final Log log = new Log("AccountManager");

  private AccountManager() {
  }

  public void sendVerificationEmail(Account account) {
    log.info("Sending verification email to '%s' for code: %s",
        account.email, account.email_verification_code);
    Email email = new Email();
    email.addRecipient(null, account.email, Message.RecipientType.TO);
    email.setSubject("War Worlds email verification");
    email.setText("blah blah: verification code: " + account.email_verification_code);
    SmtpHelper.i.send(email);
  }
}
