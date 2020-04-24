package au.com.codeka.warworlds.client.store

import android.database.Cursor
import au.com.codeka.warworlds.common.proto.Star
import java.io.IOException

/**
 * A "cursor" that's used to scroll through the results of querying stars.
 */
class StarCursor(private val cursor: Cursor) : Iterable<Star?>, AutoCloseable {
  val size: Int
    get() = cursor.count

  /**
   * Gets the value at the given position, can return null if the position is invalid or the star
   * could not be deserialized.
   */
  fun getValue(position: Int): Star {
    cursor.moveToPosition(position)
    return Star.ADAPTER.decode(cursor.getBlob(0))
  }

  override fun close() {
    cursor.close()
  }

  override fun iterator(): Iterator<Star> {
    return StarIterator(cursor)
  }

  private class StarIterator(private val cursor: Cursor) : Iterator<Star> {
    private var hasValue: Boolean
    override fun hasNext(): Boolean {
      return hasValue
    }

    override fun next(): Star {
      return try {
        val star = Star.ADAPTER.decode(cursor.getBlob(0))
        hasValue = cursor.moveToNext()
        star
      } catch (e: IOException) {
        throw NoSuchElementException()
      }
    }

    init {
      hasValue = cursor.moveToFirst()
    }
  }

}