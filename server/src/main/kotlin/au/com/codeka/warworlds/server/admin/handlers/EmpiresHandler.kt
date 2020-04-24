package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.collect.ImmutableMap
import java.util.*

/**
 * Handler for /admin/empires which lists all empires in the database. Actually, we search for all
 * accounts and then match them to the empire.
 */
class EmpiresHandler : AdminHandler() {
  public override fun get() {
    val accounts: ArrayList<Account> = DataStore.i.accounts().search()
    val empires = HashMap<Long, Empire?>()
    for (account in accounts) {
      empires[account.empire_id] = DataStore.i.empires()[account.empire_id]
    }
    render("empires/index.html", ImmutableMap.builder<String, Any>()
        .put("accounts", accounts)
        .put("empires", empires)
        .build())
  }
}