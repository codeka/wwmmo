package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/users/create, which is used to create new users.
 */
public class UsersCreateHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("users/create.html", ImmutableMap.<String, Object>builder()
        .put("all_roles", AdminRole.values())
        .build());
  }

  @Override
  public void post() throws RequestException {
    String emailAddr = getRequest().getParameter("email_addr");
    ArrayList<AdminRole> roles = new ArrayList<>();
    for (AdminRole role : AdminRole.values()) {
      if (getRequest().getParameter(role.toString()) != null) {
        roles.add(role);
      }
    }

    if (emailAddr == null || emailAddr.isEmpty()) {
      render("users/create.html", ImmutableMap.<String, Object>builder()
          .put("error", "Email address must not be empty.")
          .build());
      return;
    }

    DataStore.i.adminUsers().put(emailAddr, new AdminUser.Builder()
        .email_addr(emailAddr)
        .roles(roles)
        .build());
    redirect("/admin/users");
  }
}
