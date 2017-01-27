package au.com.codeka.warworlds.server.store;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.AdminUser;

/**
 * A store for storing admin users in the backend.
 */
public class AdminUsersStore extends BaseStore {
  AdminUsersStore(String fileName) {
    super(fileName);
  }

  /** Returns the total number of users in the admin users store. */
  public int count() {
    return 0;
  }

  /** Delete the admin user with the given email address. */
  public void delete(String email) {

  }

  /** Saves the given {@link AdminUser}, indexed by email address, to the store. */
  public void put(String email, AdminUser adminUser) {
  }

  /** Gets the {@link AdminUser} with the given identifier, or null if the user doesn't exist. */
  @Nullable
  public AdminUser get(String email) {
    return null;
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    // TODO:
    return 1;
  }
}
