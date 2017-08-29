package au.com.codeka.warworlds.server.store;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A store for storing admin users in the backend.
 */
public class AdminUsersStore extends BaseStore {
  private static final Log log = new Log("AdminUsersStore");

  private int count = -1;

  AdminUsersStore(String fileName) {
    super(fileName);
  }

  /** Returns the total number of users in the admin users store. */
  public int count() {
    if (count == -1) {
      count = 0;
      try (QueryResult res = newReader().stmt("SELECT COUNT(*) FROM users").query()) {
        if (res.next()) {
          count = res.getInt(0);
        }
      } catch (Exception e) {
        log.error("Unexpected.", e);
      }
    }

    return count;
  }

  /** Gets the {@link AdminUser} with the given identifier, or null if the user doesn't exist. */
  @Nullable
  public AdminUser get(String email) {
    try(QueryResult res = newReader()
        .stmt("SELECT user FROM users WHERE email = ?")
        .param(0, email)
        .query()) {
      if (res.next()) {
        return AdminUser.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public List<AdminUser> search() {
    try (QueryResult res = newReader().stmt("SELECT user FROM users").query()) {
      ArrayList<AdminUser> users = new ArrayList<>();
      while (res.next()) {
        users.add(AdminUser.ADAPTER.decode(res.getBytes(0)));
      }
      return users;
    } catch(Exception e) {
      log.error("Unexpected.", e);
      return new ArrayList<>();
    }
  }

  /** Saves the given {@link AdminUser}, indexed by email address, to the store. */
  public void put(String email, AdminUser adminUser) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO users (email, user) VALUES (?, ?)")
          .param(0, email)
          .param(1, adminUser.encode())
          .execute();
      count = -1; // Reset it so it gets recaculated.
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  /** Delete the admin user with the given email address. */
  public void delete(String email) {
    try {
      newWriter()
          .stmt("DELETE FROM users WHERE email = ?")
          .param(0, email)
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE users (email STRING PRIMARY KEY, user BLOB)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
