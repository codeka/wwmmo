package au.com.codeka.warworlds.server.ctrl;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.BackendUser;

/** Controls access and roles for the backend. */
public class AdminController {
  private DataBase db;

  public AdminController() {
      db = new DataBase(null);
  }
  public AdminController(Transaction trans) {
      db = new DataBase(trans);
  }

  /**
   * Gets the {@link BackendUser} instance for the user with the given email address, or null if
   * no user with that address exists.
   */
  public BackendUser getBackendUser(String email) throws RequestException {
    try {
      BackendUser user = db.getBackendUser(email);
      if (user == null) {
        // If we couldn't authenticate that user, but there's actually no backend users, we allow
        // them anyway (otherwise, you can't login to set up the database!)
        if (db.getNumBackendUsers() == 0) {
          user = new BackendUser(email, Lists.newArrayList(BackendUser.Role.SuperAdmin));
        }
      }
      return user;
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  /**
   * Gets a list of all the  {@link BackendUser} objects in the database (TODO: paging?)
   */
  public List<BackendUser> getBackendUsers() throws RequestException {
    try {
      return db.getBackendUsers();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  public int getNumBackendUsers() throws RequestException {
    try {
      return db.getNumBackendUsers();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private static class DataBase extends BaseDataBase {
    public DataBase(Transaction trans) {
        super(trans);
    }

    public BackendUser getBackendUser(String email) throws Exception {
      String sql = "SELECT * FROM backend_users WHERE email = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setString(1, email);
        SqlResult result = stmt.select();
        if (result.next()) {
          return new BackendUser(result);
        }
      }
      return null;
    }

    public List<BackendUser> getBackendUsers() throws Exception {
      String sql = "SELECT * FROM backend_users ORDER BY id DESC";
      try (SqlStmt stmt = prepare(sql)) {
        SqlResult result = stmt.select();
        ArrayList<BackendUser> users = Lists.newArrayList();
        while (result.next()) {
          users.add(new BackendUser(result));
        }
        return users;
      }
    }

    public int getNumBackendUsers() throws Exception {
      String sql = "SELECT COUNT(*) FROM backend_users";
      try (SqlStmt stmt = prepare(sql)) {
        return (int) (long) (Long) stmt.selectFirstValue(Long.class);
      }
    }
  }
}
