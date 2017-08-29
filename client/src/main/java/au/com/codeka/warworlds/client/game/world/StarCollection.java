package au.com.codeka.warworlds.client.game.world;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * Represents a "virtual" collection of stars, usually ones owned by a particular empire.
 */
public interface StarCollection {
  /** Gets the total size of this collection. */
  int size();

  /** Gets the star at the given index. */
  Star get(int index);

  /** Finds the index of the star with the given ID, or -1 if the star isn't in this collection. */
  int indexOf(long starId);

  /**
   * Notify the collection that the given star has been modified.
   *
   * @return true if we have the given star in our collection (and therefore it's been updated with
   *     the new value), or false if we don't have the given star in our collection.
   */
  boolean notifyStarModified(Star star);
}
