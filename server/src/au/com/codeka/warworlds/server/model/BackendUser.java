package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.SqlResult;

/** A {@link BackendUser} has access to the website backend. */
public class BackendUser {
  /**
   * Each role that is available to a backend user. SuperAdmin is a special role that has
   * all roles automatically granted.
   */
  public enum Role {
    /**
     * Special role with all access granted. Additionally has ability to edit roles of other
     * users (no other role can do that).
     */
    SuperAdmin,

    /** Ability to read chat messages. */
    ChatRead,

    /** Ability to post chat messages as [SERVER]. */
    ChatPost,
  }

  private int id;
  private String email;
  private ArrayList<Role> roles;
  private DateTime lastLogin;

  public BackendUser(String email, ArrayList<Role> roles) {
    this.email = email;
    this.roles = roles;
  }

  public BackendUser(SqlResult result) throws SQLException {
    id = result.getInt("id");
    email = result.getString("email");
    roles = new ArrayList<Role>();
    for (String roleName : result.getString("roles").split(",")) {
      roles.add(Role.valueOf(roleName.trim()));
    }
    lastLogin = result.getDateTime("last_login");
  }

  public int getID() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public boolean isInRole(Role role) {
    return roles.contains(Role.SuperAdmin) || roles.contains(role);
  }

  public DateTime getLastLogin() {
    return lastLogin;
  }
}
