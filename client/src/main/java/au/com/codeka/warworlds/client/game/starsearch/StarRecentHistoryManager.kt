package au.com.codeka.warworlds.client.game.starsearch

import au.com.codeka.warworlds.common.proto.Star
import java.util.*

/**
 * We keep the last 5 stars you've visited in an LRU cache so we can display them at the top of
 * the search list (note we actually keep 6 but ignore the most recent one, which is always "this
 * star").
 */
object StarRecentHistoryManager {
  private val lastStars = ArrayList<Star>()
  private val LAST_STARS_MAX_SIZE = 6

  fun addToLastStars(star: Star) {
    synchronized(lastStars) {
      for (i in lastStars.indices) {
        if (lastStars[i].id == star.id) {
          lastStars.removeAt(i)
          break
        }
      }
      lastStars.add(0, star)
      while (lastStars.size > LAST_STARS_MAX_SIZE) {
        lastStars.removeAt(lastStars.size - 1)
      }
    }
  }

  val recentStars: List<Star>
    get() = lastStars
}