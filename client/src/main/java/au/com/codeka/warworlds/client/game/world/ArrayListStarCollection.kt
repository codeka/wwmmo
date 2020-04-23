package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.common.proto.Star
import java.util.*

/**
 * A [StarCollection] that's backed by a simple [ArrayList] of stars.
 */
class ArrayListStarCollection : StarCollection {
  val stars = ArrayList<Star>()

  override fun size(): Int {
    return stars.size
  }

  override fun get(index: Int): Star {
    return stars[index]
  }

  override fun indexOf(starId: Long): Int {
    for (i in stars.indices) {
      if (stars[i].id == starId) {
        return i
      }
    }
    return -1
  }

  override fun notifyStarModified(star: Star): Boolean {
    val index = indexOf(star.id)
    if (index >= 0) {
      stars[index] = star
      return true
    }
    return false
  }

  companion object {
    fun of(vararg stars: Star): ArrayListStarCollection {
      val starCollection = ArrayListStarCollection()
      for (star in stars) {
        starCollection.stars.add(star)
      }
      return starCollection
    }
  }
}