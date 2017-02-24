package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;

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

  @Nullable
  public Account getByUid(String uid) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT account FROM accounts WHERE uid = ?")
            .param(0, uid)
            .query()
    ) {
      if (res.next()) {
        return Account.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public ArrayList<Account> search(/* TODO: search string, pagination etc */) {
    ArrayList<Account> accounts = new ArrayList<>();
    try (
        QueryResult res = newReader()
            .stmt("SELECT account FROM accounts")
            .query()
    ) {
      while (res.next()) {
        accounts.add(Account.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return accounts;
  }

  public void put(String cookie, Account account) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO accounts (email, cookie, uid, account) VALUES (?, ?, ?, ?)")
          .param(0, account.email)
          .param(1, cookie)
          .param(2, account.uid)
          .param(3, account.encode())
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
    } else if (diskVersion == 1) {
      newWriter()
          .stmt("ALTER TABLE accounts ADD COLUMN uid STRING")
          .execute();
      newWriter()
          .stmt("DROP INDEX IX_accounts_cookie")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_cookie ON accounts(cookie)")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_uid ON accounts (uid)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
