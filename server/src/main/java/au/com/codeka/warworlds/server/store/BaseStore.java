package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;

import au.com.codeka.warworlds.common.proto.Account;

/**
 * Base storage object for {@link AccountsStore} and {@link ProtobufStore}.
 *
 * K is the type of the keys we'll store in this store.
 */
public abstract class BaseStore<K, V> {
  protected final Database db;
  protected Sequence seq;

  public BaseStore(Database db) {
    this.db = Preconditions.checkNotNull(db);
  }

  public void put(K key, V value) {
    db.put(null, encodeKey(key), encodeValue(value));
  }

  public V get(K key) {
    DatabaseEntry value = new DatabaseEntry();
    if (db.get(null, encodeKey(key), value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      return decodeValue(value);
    } else {
      return null;
    }
  }

  public long nextIdentifier() {
    synchronized (db) {
      if (seq == null) {
        SequenceConfig seqConfig = new SequenceConfig();
        seqConfig.setAllowCreate(true);
        seq = db.openSequence(null, new DatabaseEntry("ids".getBytes()), seqConfig);
      }
    }
    return seq.get(null, 1);
  }

  public void close() {
    db.close();
  }

  protected abstract DatabaseEntry encodeKey(K key);
  protected abstract DatabaseEntry encodeValue(V value);
  protected abstract V decodeValue(DatabaseEntry databaseEntry);
}
