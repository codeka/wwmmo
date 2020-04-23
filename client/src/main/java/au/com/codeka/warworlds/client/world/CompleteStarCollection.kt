package au.com.codeka.warworlds.client.world

import au.com.codeka.warworlds.client.game.world.StarCollection
import au.com.codeka.warworlds.common.proto.Star

/**
 * Implementation of [StarCollection] that represents all of your empire's stars (though we
 * do support filtering stars).
 */
class CompleteStarCollection : StarCollection {
  override fun size(): Int {
    return 0
  }

  override fun get(index: Int): Star {
    throw NotImplementedError()
  }

  override fun indexOf(starId: Long): Int {
    return 0
  }

  override fun notifyStarModified(star: Star): Boolean {
    return false
  }
}