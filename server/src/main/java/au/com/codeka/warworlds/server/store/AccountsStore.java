package au.com.codeka.warworlds.server.store;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.util.Pair;

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
  public Account getByEmailAddr(String emailAddr) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT account FROM accounts WHERE email = ?")
            .param(0, emailAddr)
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

  @Nullable
  public Account getByEmpireId(long empireId) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT account FROM accounts WHERE empire_id = ?")
            .param(0, empireId)
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

  @Nullable
  public Pair<String, Account> getByVerificationCode(String emailVerificationCode) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT cookie, account FROM accounts WHERE email_verification_code = ?")
            .param(0, emailVerificationCode)
            .query()
    ) {
      if (res.next()) {
        return new Pair<>(res.getString(0), Account.ADAPTER.decode(res.getBytes(1)));
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
          .stmt("INSERT OR REPLACE INTO accounts ("
              + " email, cookie, empire_id, email_verification_code, account"
              + ") VALUES (?, ?, ?, ?, ?)")
          .param(0,
              account.email_status == Account.EmailStatus.VERIFIED
                  ? account.email_canonical
                  : null)
          .param(1, cookie)
          .param(2, account.empire_id)
          .param(3, account.email_verification_code)
          .param(4, account.encode())
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
    if (diskVersion == 1) {
      newWriter()
          .stmt("DROP INDEX IX_accounts_cookie")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_cookie ON accounts (cookie)")
          .execute();
      diskVersion++;
    }
    if (diskVersion == 2) {
      newWriter()
          .stmt("ALTER TABLE accounts ADD COLUMN empire_id INTEGER")
          .execute();

      updateAllAccounts();
      diskVersion++;
    }
    if (diskVersion == 3) {
      newWriter()
          .stmt("ALTER TABLE accounts ADD COLUMN email_verification_code STRING")
          .execute();;
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_empire_id ON accounts (empire_id)")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_accounts_email_verification_code ON accounts (email_verification_code)")
          .execute();

      diskVersion ++;
    }

    return diskVersion;
  }

  /** Called by {@link #onOpen} when we need to re-save the accounts (after adding a column) */
  private void updateAllAccounts() throws StoreException {
    QueryResult res = newReader()
        .stmt("SELECT cookie, account FROM accounts")
        .query();
    while (res.next()) {
      try {
        String cookie = res.getString(0);
        Account account = Account.ADAPTER.decode(res.getBytes(1));
        put(cookie, account);
      } catch (IOException e) {
        throw new StoreException(e);
      }
    }
  }
}
