package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.server.store.StoreException
import au.com.codeka.warworlds.server.store.base.BaseStore
import au.com.codeka.warworlds.server.util.Pair
import java.io.IOException
import java.util.*

/**
 * Stores information about [Account]s, indexed by cookie.
 */
class AccountsStore internal constructor(fileName: String) : BaseStore(fileName) {
  operator fun get(cookie: String?): Account? {
    try {
      newReader()
          .stmt("SELECT account FROM accounts WHERE cookie = ?")
          .param(0, cookie)
          .query().use { res ->
            if (res.next()) {
              return Account.ADAPTER.decode(res.getBytes(0))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return null
  }

  fun getByVerifiedEmailAddr(emailAddr: String?): Account? {
    try {
      newReader()
          .stmt("SELECT account FROM accounts WHERE email = ?")
          .param(0, emailAddr)
          .query().use { res ->
            while (res.next()) {
              val acct = Account.ADAPTER.decode(res.getBytes(0))
              if (acct.email_status == Account.EmailStatus.VERIFIED) {
                return acct
              }
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return null
  }

  fun getByEmpireId(empireId: Long): Pair<String?, Account>? {
    try {
      newReader()
          .stmt("SELECT cookie, account FROM accounts WHERE empire_id = ?")
          .param(0, empireId)
          .query().use { res ->
            if (res.next()) {
              return Pair(res.getString(0), Account.ADAPTER.decode(res.getBytes(1)))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return null
  }

  fun getByVerificationCode(emailVerificationCode: String?): Pair<String?, Account>? {
    try {
      newReader()
          .stmt("SELECT cookie, account FROM accounts WHERE email_verification_code = ?")
          .param(0, emailVerificationCode)
          .query().use { res ->
            if (res.next()) {
              return Pair(res.getString(0), Account.ADAPTER.decode(res.getBytes(1)))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return null
  }

  fun search( /* TODO: search string, pagination etc */): ArrayList<Account> {
    val accounts = ArrayList<Account>()
    try {
      newReader()
          .stmt("SELECT account FROM accounts")
          .query().use { res ->
            while (res.next()) {
              accounts.add(Account.ADAPTER.decode(res.getBytes(0)))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return accounts
  }

  fun put(cookie: String?, account: Account?) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO accounts ("
              + " email, cookie, empire_id, email_verification_code, account"
              + ") VALUES (?, ?, ?, ?, ?)")
          .param(0,
              if (account!!.email_status == Account.EmailStatus.VERIFIED) account.email_canonical else null)
          .param(1, cookie)
          .param(2, account.empire_id)
          .param(3, account.email_verification_code)
          .param(4, account.encode())
          .execute()
    } catch (e: StoreException) {
      log.error("Unexpected.", e)
    }
  }

  @Throws(StoreException::class)
  override fun onOpen(diskVersion: Int): Int {
    var diskVersion = diskVersion
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE accounts (email STRING, cookie STRING, account BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_accounts_cookie ON accounts (cookie)")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX UIX_accounts_email ON accounts (email)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 1) {
      newWriter()
          .stmt("DROP INDEX IX_accounts_cookie")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_cookie ON accounts (cookie)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 2) {
      newWriter()
          .stmt("ALTER TABLE accounts ADD COLUMN empire_id INTEGER")
          .execute()
      updateAllAccounts()
      diskVersion++
    }
    if (diskVersion == 3) {
      newWriter()
          .stmt("ALTER TABLE accounts ADD COLUMN email_verification_code STRING")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_empire_id ON accounts (empire_id)")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_email_verification_code ON accounts (email_verification_code)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 4) {
      // Email account needs to be non-unique (we could have unverified emails associated with
      // multiple accounts). Email + email_status=VERIFIED needs to be unique, but we can't really
      // do that with a simple index.
      newWriter()
          .stmt("DROP INDEX UIX_accounts_email")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_accounts_email ON accounts (email)")
          .execute()
      diskVersion++
    }
    return diskVersion
  }

  /** Called by [.onOpen] when we need to re-save the accounts (after adding a column)  */
  @Throws(StoreException::class)
  private fun updateAllAccounts() {
    val res = newReader()
        .stmt("SELECT cookie, account FROM accounts")
        .query()
    while (res!!.next()) {
      try {
        val cookie = res.getString(0)
        val account = Account.ADAPTER.decode(res.getBytes(1))
        put(cookie, account)
      } catch (e: IOException) {
        throw StoreException(e)
      }
    }
  }

  companion object {
    private val log = Log("AccountsStore")
  }
}