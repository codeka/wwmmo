package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.common.proto.Star

/**
 * Represents a "virtual" collection of stars, usually ones owned by a particular empire.
 */
interface StarCollection {
  /** Gets the total size of this collection.  */
  fun size(): Int

  /** Gets the star at the given index.  */
  operator fun get(index: Int): Star

  /** Finds the index of the star with the given ID, or -1 if the star isn't in this collection.  */
  fun indexOf(starId: Long): Int

  /**
   * Notify the collection that the given star has been modified.
   *
   * @return true if we have the given star in our collection (and therefore it's been updated with
   * the new value), or false if we don't have the given star in our collection.
   */
  fun notifyStarModified(star: Star): Boolean
}