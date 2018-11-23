package au.com.codeka.warworlds.server.world;

import com.google.common.collect.ImmutableMap;

import org.simplejavamail.email.Email;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.mail.Message;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.Configuration;
import au.com.codeka.carrot.bindings.MapBindings;
import au.com.codeka.carrot.resource.FileResourceLocator;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.Pair;
import au.com.codeka.warworlds.server.util.SmtpHelper;

/**
 * Manages {@link Account}s.
 */
public class AccountManager {
  public static final AccountManager i = new AccountManager();
  private static final Log log = new Log("AccountManager");

  private final Map<Long, Pair<String, WatchableObject<Account>>> watchedAccounts;
  private final CarrotEngine carrotEngine = new CarrotEngine(new Configuration.Builder()
      .setResourceLocator(new FileResourceLocator.Builder(new File("data/email").getAbsolutePath()))
      .build());

  private AccountManager() {
    watchedAccounts = new HashMap<>();
  }

  @Nullable
  public WatchableObject<Account> getAccount(long empireId) {
    synchronized (watchedAccounts) {
      Pair<String, WatchableObject<Account>> pair = watchedAccounts.get(empireId);
      if (pair == null) {
        Pair<String, Account> cookieAndAccount = DataStore.i.accounts().getByEmpireId(empireId);
        if (cookieAndAccount == null) {
          return null;
        }
        pair = watchAccount(cookieAndAccount.one, cookieAndAccount.two);
      }
      return pair.two;
    }
  }

  public void sendVerificationEmail(Account account) {
    WatchableObject<Empire> empire = EmpireManager.i.getEmpire(account.empire_id);
    if (empire == null) {
      throw new IllegalStateException(
          "No empire associated with account: " + account.email_canonical);
    }

    String verifyUrl =
        au.com.codeka.warworlds.server.Configuration.i.getBaseUrl() + "/accounts/verify";
    Map<String, Object> data = ImmutableMap.of(
        "account", account,
        "empire", empire.get(),
        "verification_url", verifyUrl);

    log.info("Sending verification email to '%s' for '%s' code: %s",
        account.email, empire.get().display_name, account.email_verification_code);
    Email email = new Email();
    email.addRecipient(null, account.email, Message.RecipientType.TO);
    email.setSubject("War Worlds email verification");
    try {
      email.setText(carrotEngine.process("verification.txt", new MapBindings(data)));
      email.setTextHTML(carrotEngine.process("verification.html", new MapBindings(data)));
    } catch (CarrotException e) {
      log.error("Error sending verification email.", e);
      return;
    }
    SmtpHelper.i.send(email);
  }

  private Pair<String, WatchableObject<Account>> watchAccount(String cookie, Account account) {
    Pair<String, WatchableObject<Account>> pair;
    synchronized (watchedAccounts) {
      pair = watchedAccounts.get(account.empire_id);
      if (pair != null) {
        pair.two.set(account);
      } else {
        pair = new Pair<>(cookie, new WatchableObject<>(account));
        pair.two.addWatcher(accountWatcher);
        watchedAccounts.put(pair.two.get().empire_id, pair);
      }
    }
    return pair;
  }

  private final WatchableObject.Watcher<Account> accountWatcher =
      new WatchableObject.Watcher<Account>() {
    @Override
    public void onUpdate(WatchableObject<Account> account) {
      log.debug("Saving account %d %s", account.get().empire_id, account.get().email_canonical);
      Pair<String, WatchableObject<Account>> pair = watchedAccounts.get(account.get().empire_id);
      DataStore.i.accounts().put(pair.one, pair.two.get());
    }
  };
}
