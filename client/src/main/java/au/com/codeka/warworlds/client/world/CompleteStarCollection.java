package au.com.codeka.warworlds.client.world;

import au.com.codeka.warworlds.client.game.world.StarCollection;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Implementation of {@link StarCollection} that represents all of your empire's stars (though we
 * do support filtering stars).
 */
public class CompleteStarCollection implements StarCollection {
  @Override
  public int size() {
    return 0;
  }

  @Override
  public Star get(int index) {
    return null;
  }

  @Override
  public int indexOf(long starId) {
    return 0;
  }

  @Override
  public boolean notifyStarModified(Star star) {
    return false;
  }
}
