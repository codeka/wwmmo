package au.com.codeka.warworlds.server.store;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * A store which allows us to store unique names (e.g. empire names) and map them to an ID.
 */
public class UniqueNameStore extends BaseStore<String, Long> {
  private static final Charset charset = Charset.forName("utf-8");

  /* package */ UniqueNameStore(Database db) {
    super(db);
  }

  public boolean putIfNotExist(String key, long value) {
    super.put(key, value);
    // TODO: actually do it.
    return true;
  }

  @Override
  protected DatabaseEntry encodeKey(String key) {
    return new DatabaseEntry(key.getBytes(charset));
  }

  @Override
  protected DatabaseEntry encodeValue(Long value) {
    return new DatabaseEntry(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  @Override
  protected String decodeKey(DatabaseEntry databaseEntry) {
    return new String(databaseEntry.getData(), charset);
  }

  @Override
  protected Long decodeValue(DatabaseEntry databaseEntry) {
    return ByteBuffer.wrap(databaseEntry.getData()).getLong();
  }
}
