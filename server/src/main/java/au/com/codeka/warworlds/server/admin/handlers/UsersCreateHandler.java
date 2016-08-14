package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/users/create, which is used to create new users.
 */
public class UsersCreateHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("users/create.html", null);
  }

  @Override
  public void post() throws RequestException {
    String emailAddr = getRequest().getParameter("email_addr");
    AdminRole role = AdminRole.valueOf(getRequest().getParameter("role"));

    if (emailAddr == null || emailAddr.isEmpty()) {
      render("users/create.html", ImmutableMap.<String, Object>builder()
          .put("role", role)
          .put("error", "Email address must not be empty.")
          .build());
      return;
    }

    DataStore.i.adminUsers().put(emailAddr, new AdminUser.Builder()
        .email_addr(emailAddr)
        .role(role)
        .build());
    redirect("/admin/users");
  }
}
