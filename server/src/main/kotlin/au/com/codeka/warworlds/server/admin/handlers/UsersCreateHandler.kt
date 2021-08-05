package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.common.proto.AdminUser
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.collect.ImmutableMap
import java.util.*

/**
 * Handler for /admin/users/create, which is used to create new users.
 */
class UsersCreateHandler : AdminHandler() {
  public override fun get() {
    render("users/create.html", ImmutableMap.builder<String, Any>()
        .put("all_roles", AdminRole.values())
        .build())
  }

  public override fun post() {
    val emailAddr = request.getParameter("email_addr")
    val roles = ArrayList<AdminRole>()
    for (role in AdminRole.values()) {
      if (request.getParameter(role.toString()) != null) {
        roles.add(role)
      }
    }
    if (emailAddr == null || emailAddr.isEmpty()) {
      render("users/create.html", ImmutableMap.builder<String, Any>()
          .put("error", "Email address must not be empty.")
          .build())
      return
    }
    DataStore.i.adminUsers().put(emailAddr, AdminUser(
        email_addr = emailAddr,
        roles = roles))
    redirect("/admin/users")
  }
}