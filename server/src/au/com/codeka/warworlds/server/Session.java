package au.com.codeka.warworlds.server;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.SqlResult;

/**
 * Represents details about the current session, such as your username, empire_id and so on.
 *
 * @author dean
 */
public class Session {
  private String cookie;
  private String actualEmail;
  private String email;
  private String clientId;
  private DateTime loginTime;
  private int empireID;
  private Integer allianceID;
  private boolean isAdmin;
  private boolean isAnonymous;

  public Session() {
  }

  public Session(SqlResult res) throws SQLException {
    cookie = res.getString("session_cookie");
    actualEmail = res.getString("user_email");
    email = actualEmail;
    clientId = res.getString("client_id");
    loginTime = res.getDateTime("login_time");
    if (res.getInt("empire_id") == null) {
      empireID = 0;
    } else {
      empireID = res.getInt("empire_id");
    }
    allianceID = res.getInt("alliance_id");
    isAdmin = res.getInt("is_admin") == 1;
    if (res.getInt("is_anonymous") != null) {
      isAnonymous = res.getInt("is_anonymous") == 1;
    }
  }

  public Session(Session copy) {
    cookie = copy.cookie;
    actualEmail = copy.actualEmail;
    email = actualEmail;
    loginTime = copy.loginTime;
    empireID = copy.empireID;
    allianceID = copy.allianceID;
    isAdmin = copy.isAdmin;
    isAnonymous = copy.isAnonymous;
  }

  public Session(String cookie, String email, String clientId, DateTime loginTime, int empireID,
      Integer allianceID, boolean isAdmin, boolean isAnonymous) {
    this.cookie = cookie;
    actualEmail = email;
    this.email = actualEmail;
    this.clientId = clientId;
    this.loginTime = loginTime;
    this.empireID = empireID;
    this.allianceID = allianceID;
    this.isAdmin = isAdmin;
    this.isAnonymous = isAnonymous;
  }

  public String getCookie() {
    return cookie;
  }

  public String getEmail() {
    return email;
  }

  public String getActualEmail() {
    return actualEmail;
  }

  /** Get the Client ID of the app that authenticated as this user. */
  public String getClientId() {
    return clientId;
  }

  public DateTime getLoginTime() {
    return loginTime;
  }

  public int getEmpireID() {
    return empireID;
  }

  public void setEmpireID(int empireID) {
    this.empireID = empireID;
  }

  public int getAllianceID() {
    if (allianceID == null) {
      return 0;
    }
    return allianceID;
  }

  public void setAllianceID(int allianceID) {
    this.allianceID = allianceID;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public boolean isAnonymous() {
    return isAnonymous;
  }

  public Session impersonate(String email, int empireID) {
    Session newSession = new Session(this);
    newSession.email = email;
    newSession.empireID = empireID;
    return newSession;
  }
}
