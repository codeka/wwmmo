package au.com.codeka.warworlds.server.admin;

import org.joda.time.DateTime;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.AdminUser;

/**
 * Represents details about the current session, such as your username, access level and so on.
 */
public class Session {
  private final String cookie;
  private final DateTime loginTime;
  @Nonnull private final AdminUser adminUser;

  public Session(String cookie, @Nonnull AdminUser adminUser, DateTime loginTime) {
    this.cookie = cookie;
    this.adminUser = adminUser;
    this.loginTime = loginTime;
  }

  public String getCookie() {
    return cookie;
  }

  public String getEmail() {
    return adminUser.email_addr;
  }

  public boolean isInRole(AdminRole role) {
    for (AdminRole ar : adminUser.roles) {
      if (ar.equals(role)) {
        return true;
      }
    }
    return false;
  }

  public DateTime getLoginTime() {
    return loginTime;
  }
}
