package au.com.codeka.warworlds.server.world

import au.com.codeka.carrot.CarrotEngine
import au.com.codeka.carrot.CarrotException
import au.com.codeka.carrot.Configuration
import au.com.codeka.carrot.bindings.MapBindings
import au.com.codeka.carrot.resource.FileResourceLocator
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import au.com.codeka.warworlds.server.util.SmtpHelper
import com.google.common.collect.ImmutableMap
import org.simplejavamail.email.Email
import java.io.File
import java.util.*
import javax.mail.Message

/**
 * Manages [Account]s.
 */
class AccountManager private constructor() {
  private val watchedAccounts = HashMap<Long, Pair<String?, WatchableObject<Account>>>()

  private val carrotEngine = CarrotEngine(Configuration.Builder()
      .setResourceLocator(FileResourceLocator.Builder(File("data/email").absolutePath))
      .build())

  fun getAccount(empireId: Long): WatchableObject<Account>? {
    synchronized(watchedAccounts) {
      var pair = watchedAccounts[empireId]
      if (pair == null) {
        val cookieAndAccount: Pair<String?, Account> = DataStore.Companion.i.accounts().getByEmpireId(empireId)
            ?: return null
        pair = watchAccount(cookieAndAccount.one, cookieAndAccount.two)
      }
      return pair!!.two
    }
  }

  fun sendVerificationEmail(account: Account?) {
    val empire: WatchableObject<Empire> = EmpireManager.i.getEmpire(account!!.empire_id)
        ?: throw IllegalStateException(
            "No empire associated with account: " + account.email_canonical)
    val verifyUrl: String = au.com.codeka.warworlds.server.Configuration.i.baseUrl + "/accounts/verify"
    val data: Map<String, Any?> = ImmutableMap.of<String, Any?>(
        "account", account,
        "empire", empire.get(),
        "verification_url", verifyUrl)
    log.info("Sending verification email to '%s' for '%s' code: %s",
        account.email, empire.get().display_name, account.email_verification_code)
    val email = Email()
    email.addRecipient(null, account.email, Message.RecipientType.TO)
    email.setSubject("War Worlds email verification")
    try {
      email.setText(carrotEngine.process("verification.txt", MapBindings(data)))
      email.setTextHTML(carrotEngine.process("verification.html", MapBindings(data)))
    } catch (e: CarrotException) {
      log.error("Error sending verification email.", e)
      return
    }
    SmtpHelper.Companion.i.send(email)
  }

  private fun watchAccount(cookie: String?, account: Account?): Pair<String?, WatchableObject<Account>>? {
    var pair: Pair<String?, WatchableObject<Account>>?
    synchronized(watchedAccounts) {
      pair = watchedAccounts[account!!.empire_id]
      if (pair != null) {
        pair!!.two!!.set(account)
      } else {
        pair = Pair(cookie, WatchableObject(account))
        pair!!.two!!.addWatcher(accountWatcher)
        watchedAccounts.put(pair!!.two!!.get().empire_id, pair!!)
      }
    }
    return pair
  }

  private val accountWatcher: WatchableObject.Watcher<Account> = object : WatchableObject.Watcher<Account> {
    override fun onUpdate(obj: WatchableObject<Account>) {
      val account = obj.get()
      log.debug("Saving account %d %s", account.empire_id, account.email_canonical)
      val pair = watchedAccounts[account.empire_id]!!
      DataStore.Companion.i.accounts().put(pair.one, pair.two!!.get())
    }
  }

  companion object {
    val i = AccountManager()
    private val log = Log("AccountManager")
  }
}