package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import java.util.*

/** Manages [Account]s. */
class AccountManager private constructor() {
  companion object {
    val i = AccountManager()
    private val log = Log("AccountManager")
  }

  private val watchedAccounts = HashMap<Long, Pair<String, WatchableObject<Account>>>()

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
      log.debug("Saving account %d %s", account.empire_id, account.email)
      val pair = watchedAccounts[account.empire_id]!!
      DataStore.i.accounts().put(pair.one, pair.two.get())
    }
  }
}