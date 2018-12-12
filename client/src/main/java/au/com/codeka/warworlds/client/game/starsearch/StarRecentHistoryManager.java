package au.com.codeka.warworlds.client.game.starsearch;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * We keep the last 5 stars you've visited in an LRU cache so we can display them at the top of
 * the search list (note we actually keep 6 but ignore the most recent one, which is always "this
 * star").
 */
public class StarRecentHistoryManager {
  public static StarRecentHistoryManager i = new StarRecentHistoryManager();

  private final ArrayList<Star> lastStars = new ArrayList<>();
  private final int LAST_STARS_MAX_SIZE = 6;

  public void addToLastStars(Star star) {
    synchronized (lastStars) {
      for (int i = 0; i < lastStars.size(); i++) {
        if (lastStars.get(i).id.equals(star.id)) {
          lastStars.remove(i);
          break;
        }
      }
      lastStars.add(0, star);
      while (lastStars.size() > LAST_STARS_MAX_SIZE) {
        lastStars.remove(lastStars.size() - 1);
      }
    }
  }

  public List<Star> getRecentStars() {
    return lastStars;
  }
}
