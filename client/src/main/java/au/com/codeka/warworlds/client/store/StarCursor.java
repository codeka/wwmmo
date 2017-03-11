package au.com.codeka.warworlds.client.store;

import android.database.Cursor;

import java.io.IOException;
import java.util.Iterator;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * A "cursor" that's used to scroll through the results of querying stars.
 */
public class StarCursor implements Iterable<Star>, AutoCloseable {
  private final Cursor cursor;

  public StarCursor(Cursor cursor) {
    this.cursor = cursor;
  }

  public int getSize() {
    return cursor.getCount();
  }

  /**
   * Gets the value at the given position, can return null if the position is invalid or the star
   * could not be deserialized.
   */
  @Nullable
  public Star getValue(int position) {
    cursor.moveToPosition(position);
    try {
      return Star.ADAPTER.decode(cursor.getBlob(0));
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void close() {
    cursor.close();
  }

  @Override
  public Iterator<Star> iterator() {
    return new StarIterator(cursor);
  }

  private static class StarIterator implements Iterator<Star> {
    private final Cursor cursor;
    private boolean hasValue;

    public StarIterator(Cursor cursor) {
      this.cursor = cursor;
      hasValue = cursor.moveToFirst();
    }

    @Override
    public boolean hasNext() {
      return hasValue;
    }

    @Override
    public Star next() {
      try {
        Star star = Star.ADAPTER.decode(cursor.getBlob(0));
        hasValue = cursor.moveToNext();
        return star;
      } catch (IOException e) {
        return null;
      }
    }
  }
}
