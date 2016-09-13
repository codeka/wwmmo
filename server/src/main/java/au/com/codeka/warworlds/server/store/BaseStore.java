package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.utilint.Pair;

import java.nio.ByteBuffer;

/**
 * Base storage object for {@link ProtobufStore} and some of the other stores.
 *
 * K is the type of the keys we'll store in this store, V is the type of the value.
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

  public boolean delete(K key) {
    return (db.delete(null, encodeKey(key)) == OperationStatus.SUCCESS);
  }

  /** Gets the number of entries in this store. */
  public long count() {
    return db.count();
  }

  /** Search the store and return all values. */
  public StoreCursor search() {
    return new StoreCursor(db.openCursor(null, null));
  }

  public long nextIdentifier() {
    synchronized (db) {
      if (seq == null) {
        SequenceConfig seqConfig = new SequenceConfig();
        seqConfig.setAllowCreate(true);
        seqConfig.setInitialValue(100);
        ByteBuffer bb =
            ByteBuffer.allocate(4).put(StoreHelper.SEQUENCE_MARKER).put("ids".getBytes());
        seq = db.openSequence(null, new DatabaseEntry(bb.array()), seqConfig);
      }
    }
    return seq.get(null, 1);
  }

  public void close() {
    db.close();
  }

  protected abstract DatabaseEntry encodeKey(K key);
  protected abstract DatabaseEntry encodeValue(V value);
  protected abstract K decodeKey(DatabaseEntry databaseEntry);
  protected abstract V decodeValue(DatabaseEntry databaseEntry);

  /** A cursor for looping through all the values in a store. */
  public class StoreCursor implements AutoCloseable {
    private final Cursor cursor;
    private final DatabaseEntry key = new DatabaseEntry();
    private final DatabaseEntry value = new DatabaseEntry();

    public StoreCursor(Cursor cursor) {
      this.cursor = cursor;
    }

    public Pair<K, V> first() {
      if (cursor.getFirst(key, value, LockMode.DEFAULT) != OperationStatus.SUCCESS) {
        return null;
      }

      return new Pair<>(decodeKey(key), decodeValue(value));
    }

    public Pair<K, V> next() {
      K k = null;
      while (k == null) {
        if (cursor.getNext(key, value, LockMode.DEFAULT) != OperationStatus.SUCCESS) {
          return null;
        }

        k = decodeKey(key);
      }

      return new Pair<>(k, decodeValue(value));
    }

    @Override
    public void close() {
      cursor.close();
    }
  }
}
