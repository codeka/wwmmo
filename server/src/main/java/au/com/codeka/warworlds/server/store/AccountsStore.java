package au.com.codeka.warworlds.server.store;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Account;

/**
 * Stores information about {@link Account}s, indexed by cookie.
 */
public class AccountsStore extends BaseStore {
  AccountsStore(String fileName) {
    super(fileName);
  }

  @Nullable
  public Account get(String cookie) {
    return null;
  }

  public void put(String cookie, Account account) {
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    // TODO
    return 1;
  }
}
