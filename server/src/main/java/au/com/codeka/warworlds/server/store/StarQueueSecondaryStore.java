package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.Transaction;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * Represents a "secondary" store for the main stars store, which indexes the stars by their
 * <code>next_simulation</code> field.
 */
public class StarQueueSecondaryStore {
  private final SecondaryDatabase sdb;
  private final ProtobufStore<Star> stars;

  public StarQueueSecondaryStore(Environment env, Database db, ProtobufStore<Star> stars) {
    SecondaryConfig secondaryConfig = new SecondaryConfig();
    secondaryConfig.setAllowCreate(true);
    secondaryConfig.setTransactional(true);
    secondaryConfig.setSortedDuplicates(true);
    secondaryConfig.setKeyCreator(new KeyCreator());
    SecondaryDatabase sdb = env.openSecondaryDatabase(null, "stars_queue", db, secondaryConfig);

    this.sdb = Preconditions.checkNotNull(sdb);
    this.stars = Preconditions.checkNotNull(stars);
  }

  /** Gets a {@link Cursor} that returns all stars belonging to a . */
  @Nullable
  public Star next(@Nullable Transaction trans) {
    CursorConfig cursorConfig = new CursorConfig();
    Cursor cursor = sdb.openCursor(trans, cursorConfig);
    try {
      DatabaseEntry value = new DatabaseEntry();
      if (cursor.getFirst(new DatabaseEntry(), value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        return stars.decodeValue(value);
      }
      return null;
    } finally {
      cursor.close();
    }
  }

  /** Key creator which extracts the "next simulation" time from stars and uses that as the key. */
  public class KeyCreator implements SecondaryKeyCreator {
    @Override
    public boolean createSecondaryKey(
        SecondaryDatabase secondaryDatabase,
        DatabaseEntry key,
        DatabaseEntry value,
        DatabaseEntry result) {
      if (key.getSize() != 8) {
        // it's probably not a star (probably the sequence)
        return false;
      }
      Star star = stars.decodeValue(value);
      if (star.next_simulation == null) {
        return false;
      }

      result.setData(ByteBuffer.allocate(Long.BYTES).putLong(star.next_simulation).array());
      return true;
    }
  }
}
