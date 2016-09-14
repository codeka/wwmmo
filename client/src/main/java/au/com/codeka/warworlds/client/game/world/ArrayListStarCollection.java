package au.com.codeka.warworlds.client.game.world;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * A {@link StarCollection} that's backed by a simple {@link ArrayList} of stars.
 */
public class ArrayListStarCollection implements StarCollection {
  private ArrayList<Star> stars;

  public ArrayListStarCollection() {
    stars = new ArrayList<>();
  }

  @Override
  public int size() {
    return stars.size();
  }

  @Override
  public Star get(int index) {
    return stars.get(index);
  }

  public ArrayList<Star> getStars() {
    return stars;
  }
}
