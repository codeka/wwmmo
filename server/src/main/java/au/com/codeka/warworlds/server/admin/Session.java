package au.com.codeka.warworlds.server.admin;

import org.joda.time.DateTime;

/**
 * Represents details about the current session, such as your username, access level and so on.
 */
public class Session {
  private String cookie;
  private String email;
  private DateTime loginTime;

  public Session() {
  }

  public Session(Session copy) {
    cookie = copy.cookie;
    email = copy.email;
    loginTime = copy.loginTime;
  }

  public Session(String cookie, String email, DateTime loginTime) {
    this.cookie = cookie;
    this.email = email;
    this.loginTime = loginTime;
  }

  public String getCookie() {
    return cookie;
  }

  public String getEmail() {
    return email;
  }

  public DateTime getLoginTime() {
    return loginTime;
  }
}
