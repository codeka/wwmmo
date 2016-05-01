package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import java.io.IOException;
import java.nio.ByteBuffer;

import au.com.codeka.warworlds.common.proto.Account;

/**
 * A special data store which we use to store accounts. This isn't just a single mapping from ID
 * to protocol buffer, so has a special class.
 */
public class AccountsStore extends BaseStore<String, Account> {
  /* package */ AccountsStore(Database db) {
    super(db);
  }

  @Override
  protected DatabaseEntry encodeKey(String key) {
    return new DatabaseEntry(key.getBytes());
  }

  @Override
  protected DatabaseEntry encodeValue(Account value) {
    return new DatabaseEntry(value.encode());
  }

  @Override
  protected Account decodeValue(DatabaseEntry databaseEntry) {
    try {
      return Account.ADAPTER.decode(databaseEntry.getData());
    } catch (IOException e) {
      return null;
    }
  }
}
