package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminUser
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.collect.ImmutableMap

/**
 * Handler for /admin/users, which allows you to view the users that have access to the backend.
 */
class UsersHandler : AdminHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    val users: List<AdminUser> = DataStore.i.adminUsers().search()
    render("users/index.html", ImmutableMap.builder<String, Any>()
        .put("users", users)
        .build())
  }
}