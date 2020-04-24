package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.EmpireStorage
import au.com.codeka.warworlds.common.proto.Star

/**
 * Helper for working with empires and empire IDs.
 */
object EmpireHelper {
  fun isSameEmpire(lhs: Long?, rhs: Long?): Boolean {
    if (lhs == null && rhs == null) {
      return true
    }
    return if (lhs == null || rhs == null) {
      false
    } else lhs == rhs
  }

  /** Gets the [EmpireStorage.Builder] for the given empire.  */
  fun getStore(star: Star.Builder, empireId: Long?): EmpireStorage.Builder? {
    val index = getStoreIndex(star, empireId)
    return if (index < 0) {
      null
    } else star.empire_stores.get(index).newBuilder()
  }

  fun getStoreIndex(star: Star.Builder, empireId: Long?): Int {
    for (i in 0 until star.empire_stores.size) {
      val store: EmpireStorage = star.empire_stores[i]
      if (store.empire_id == null && empireId == null) {
        return i
      }
      if (store.empire_id != null && store.empire_id == empireId) {
        return i
      }
    }
    return -1
  }
}