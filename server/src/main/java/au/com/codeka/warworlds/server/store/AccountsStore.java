package au.com.codeka.warworlds.server.store;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;

/**
 * Stores information about {@link Account}s, indexed by cookie.
 */
public class AccountsStore extends BaseStore {
  private final static Log log = new Log("AccountsStore");

  AccountsStore(String fileName) {
    super(fileName);
  }

  @Nullable
  public Account get(String cookie) {
    try (QueryResult res = newReader()
        .stmt("SELECT account FROM accounts WHERE cookie = ?")
        .param(0, cookie)
        .query()) {
      if (res.next()) {
        return Account.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public void put(String cookie, Account account) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO accounts (email, cookie, account) VALUES (?, ?, ?)")
          .param(0, (String) null)
          .param(1, cookie)
          .param(2, account.encode())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE accounts (email STRING, cookie STRING, account BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_accounts_cookie ON accounts (cookie)")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX UIX_accounts_email ON accounts (email)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
