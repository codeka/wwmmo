package au.com.codeka.warworlds.client.world;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * Represents a "virtual" collection of stars, usually ones owned by a particular empire.
 */
public interface StarCollection {
  int size();
  Star get(int index);
}
