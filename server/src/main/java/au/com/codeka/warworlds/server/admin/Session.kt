package au.com.codeka.warworlds.server.admin

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.common.proto.AdminUser
import org.joda.time.DateTime

/**
 * Represents details about the current session, such as your username, access level and so on.
 */
class Session(val cookie: String, private val adminUser: AdminUser?, val loginTime: DateTime) {

  val email: String
    get() = adminUser!!.email_addr

  fun isInRole(role: AdminRole): Boolean {
    for (ar in adminUser!!.roles) {
      if (ar == role) {
        return true
      }
    }
    return false
  }

}