package au.com.codeka.warworlds.server.world

import au.com.codeka.carrot.CarrotEngine
import au.com.codeka.carrot.bindings.MapBindings
import au.com.codeka.carrot.resource.FileResourceLocator
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.Configuration
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import au.com.codeka.warworlds.server.util.SmtpHelper
import com.google.common.collect.ImmutableMap
import org.simplejavamail.email.Email
import java.io.File
import java.util.*
import javax.mail.Message

import au.com.codeka.carrot.Configuration as CarrotConfiguration

/** Manages [Account]s. */
class AccountManager private constructor() {
  companion object {
    val i = AccountManager()
    private val log = Log("AccountManager")
  }

  private val watchedAccounts = HashMap<Long, Pair<String, WatchableObject<Account>>>()

  private val carrotEngine = CarrotEngine(CarrotConfiguration.Builder()
      .setResourceLocator(FileResourceLocator.Builder(File("data/email").absolutePath))
      .build())

  fun getAccount(empireId: Long): WatchableObject<Account>? {
    synchronized(watchedAccounts) {
      var pair = watchedAccounts[empireId]
      if (pair == null) {
        val cookieAndAccount = DataStore.i.accounts().getByEmpireId(empireId)
            ?: return null
        pair = watchAccount(cookieAndAccount.one, cookieAndAccount.two)
      }
      return pair.two
    }
  }

  fun sendVerificationEmail(account: Account) {
    val empire: WatchableObject<Empire> = EmpireManager.i.getEmpire(account.empire_id)
        ?: throw IllegalStateException(
            "No empire associated with account: " + account.email_canonical)
    val verifyUrl: String = Configuration.i.baseUrl + "/accounts/verify"
    val data: Map<String, Any> = ImmutableMap.of<String, Any>(
        "account", account,
        "empire", empire.get(),
        "verification_url", verifyUrl)

    log.info("Sending verification email to '%s' for '%s' code: %s",
        account.email, empire.get().display_name, account.email_verification_code)
    val email = Email()
    email.addRecipient(null, account.email, Message.RecipientType.TO)
    email.subject = "War Worlds email verification"
    email.text = carrotEngine.process("verification.txt", MapBindings(data))
    email.textHTML = carrotEngine.process("verification.html", MapBindings(data))

    SmtpHelper.i.send(email)
  }

  fun verifyAccount(account: Account) {
    // If there's already an associated account with this email address, mark it as abandoned now.
    while (true) {
      val existingAccount = DataStore.i.accounts().getByVerifiedEmailAddr(account.email_canonical)
      if (existingAccount != null) {
        getAccount(existingAccount.empire_id)!!.set(
            existingAccount.newBuilder()
                .email_status(Account.EmailStatus.ABANDONED)
                .build())
      } else {
        break
      }
    }
    getAccount(account.empire_id)!!.set(account.newBuilder()
        .email_status(Account.EmailStatus.VERIFIED)
        .email_verification_code(null)
        .build())
  }

  private fun watchAccount(cookie: String, account: Account)
      : Pair<String, WatchableObject<Account>> {
    synchronized(watchedAccounts) {
      var pair = watchedAccounts[account.empire_id]
      if (pair != null) {
        pair.two.set(account)
      } else {
        pair = Pair(cookie, WatchableObject(account))
        pair.two.addWatcher(accountWatcher)
        watchedAccounts[pair.two.get().empire_id] = pair
      }
      return pair
    }
  }

  private val accountWatcher: WatchableObject.Watcher<Account> =
      object : WatchableObject.Watcher<Account> {
    override fun onUpdate(obj: WatchableObject<Account>) {
      val account = obj.get()
      log.debug("Saving account %d %s", account.empire_id, account.email_canonical)
      val pair = watchedAccounts[account.empire_id]!!
      DataStore.i.accounts().put(pair.one, pair.two.get())
    }
  }
}