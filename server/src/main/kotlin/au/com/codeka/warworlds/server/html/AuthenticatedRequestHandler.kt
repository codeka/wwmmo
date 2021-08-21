package au.com.codeka.warworlds.server.html

import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.EmpireManager
import au.com.codeka.warworlds.server.world.WatchableObject

open class AuthenticatedRequestHandler : ProtobufRequestHandler() {
  private var empire: WatchableObject<Empire>? = null

  /**
   * Gets the [Empire] that has authenticated this request.
   *
   * @throws RequestException if the request is not actually authenticated.
   */
  @get:Throws(RequestException::class)
  protected val authenticatedEmpire: WatchableObject<Empire>?
    get() {
      if (empire == null) {
        val cookie = request.getHeader("COOKIE")
            ?: throw RequestException(400, "No COOKIE header found.")
        val acct: Account = DataStore.i.accounts().get(cookie)
            ?: throw RequestException(400, "Invalid COOKIE.")
        empire = EmpireManager.i.getEmpire(acct.empire_id)
      }
      return empire
    }
}
