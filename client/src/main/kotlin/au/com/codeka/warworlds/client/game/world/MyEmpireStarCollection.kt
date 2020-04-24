package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.client.store.StarCursor
import au.com.codeka.warworlds.common.proto.Star

/**
 * Implementation of [StarCollection] that represents all of the stars my empire "owns".
 */
class MyEmpireStarCollection : StarCollection {
  private val starCursor: StarCursor
  override fun size(): Int {
    return starCursor.size
  }

  override fun get(index: Int): Star {
    return starCursor.getValue(index)
  }

  override fun indexOf(starId: Long): Int {
    // TODO
    return -1
  }

  override fun notifyStarModified(star: Star): Boolean {
    // TODO
    return false
  }

  init {
    starCursor = StarManager.myStars
  }
}