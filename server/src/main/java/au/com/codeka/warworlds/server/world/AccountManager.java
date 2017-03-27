package au.com.codeka.warworlds.server.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.simplejavamail.email.Email;

import java.io.File;
import java.util.Map;

import javax.mail.Message;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.util.SmtpHelper;

/**
 * Manages {@link Account}s.
 */
public class AccountManager {
  public static final AccountManager i = new AccountManager();
  private static final Log log = new Log("AccountManager");

  private CarrotEngine carrotEngine = new CarrotEngine();

  private AccountManager() {
    carrotEngine.getConfig().setResourceLocater(
        new FileResourceLocater(
            carrotEngine.getConfig(),
            new File("data/email").getAbsolutePath()));
    carrotEngine.getConfig().setEncoding("utf-8");
  }

  public void sendVerificationEmail(Account account) {
    WatchableObject<Empire> empire = EmpireManager.i.getEmpire(account.empire_id);
    if (empire == null) {
      throw new IllegalStateException(
          "No empire associated with account: " + account.email_canonical);
    }

    Map<String, Object> data = ImmutableMap.of(
        "account", account,
        "empire", empire.get(),
        "verification_url", "http://localhost:8080/accounts/verify");

    log.info("Sending verification email to '%s' for '%s' code: %s",
        account.email, empire.get().display_name, account.email_verification_code);
    Email email = new Email();
    email.addRecipient(null, account.email, Message.RecipientType.TO);
    email.setSubject("War Worlds email verification");
    try {
      email.setText(carrotEngine.process("verification.txt", data));
      email.setTextHTML(carrotEngine.process("verification.html", data));
    } catch (CarrotException e) {
      log.error("Error sending verification email.", e);
      return;
    }
    SmtpHelper.i.send(email);
  }
}
