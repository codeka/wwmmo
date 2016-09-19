package au.com.codeka.warworlds.client.game.world;

import au.com.codeka.warworlds.client.store.StarCursor;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Implementation of {@link StarCollection} that represents all of the stars my empire "owns".
 */
public class MyEmpireStarCollection implements StarCollection {
  private StarCursor starCursor;

  public MyEmpireStarCollection() {
    starCursor = StarManager.i.getMyStars();
  }

  @Override
  public int size() {
    return starCursor.getSize();
  }

  @Override
  public Star get(int index) {
    return starCursor.getValue(index);
  }
}
