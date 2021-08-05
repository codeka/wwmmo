package au.com.codeka.warworlds.server.admin

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.common.proto.AdminUser
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.collect.Lists
import org.joda.time.DateTime
import java.security.SecureRandom
import java.util.*

/**
 * Manages sessions in the admin backend. We keep current sessions live in memory, and don't bother
 * saving them to the database (if the server restarts, anybody using the backend will need to re-
 * authenticate).
 */
class SessionManager {
  private val sessions = HashMap<String?, Session>()
  fun authenticate(emailAddr: String?): Session? {
    // TODO: expire old sessions
    val user: AdminUser?
    if (DataStore.i.adminUsers().count() == 0) {
      // If you don't have any users, then everybody who authenticates is in all roles.
      user = AdminUser(
          email_addr = emailAddr,
          roles = Lists.newArrayList(*AdminRole.values()))
    } else {
      user = DataStore.i.adminUsers()[emailAddr]
      if (user == null) {
        log.warning("User '%s' is not a valid admin user.", emailAddr)
        return null
      }
    }
    val session = Session(generateCookie(), user, DateTime.now())
    sessions[session.cookie] = session
    return session
  }

  fun getSession(cookie: String?): Session? {
    return sessions[cookie]
  }

  /** Generates a cookie, which is basically just a long-ish string of random bytes. */
  private fun generateCookie(): String {
    // Generate a random string for the session cookie.
    val rand = SecureRandom()
    val cookie = StringBuilder()
    for (i in 0 until COOKIE_LENGTH) {
      cookie.append(COOKIE_CHARS[rand.nextInt(COOKIE_CHARS.size)])
    }
    return cookie.toString()
  }

  companion object {
    private val log = Log("SessionManager")
    val i = SessionManager()
    private val COOKIE_CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()
    private const val COOKIE_LENGTH = 40
  }
}