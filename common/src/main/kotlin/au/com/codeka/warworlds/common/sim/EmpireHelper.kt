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

  /** Gets the [EmpireStorage] for the given empire.  */
  fun getStore(star: Star, empireId: Long?): EmpireStorage? {
    val index = getStoreIndex(star, empireId)
    return if (index < 0) {
      null
    } else star.empire_stores[index]
  }

  /** Gets the [EmpireStorage] for the given empire.  */
  fun getStore(star: MutableStar, empireId: Long): MutableEmpireStorage? {
    val index = getStoreIndex(star, empireId)
    return if (index < 0) {
      null
    } else star.empireStores[index]
  }

  fun getStoreIndex(star: MutableStar, empireId: Long): Int {
    star.empireStores.indices.forEach { i ->
      val store = star.empireStores[i]
      if (store.empireId == empireId) {
        return i
      }
    }
    return -1
  }

  fun getStoreIndex(star: Star, empireId: Long?): Int {
    star.empire_stores.indices.forEach { i ->
      val store = star.empire_stores[i]
      if (empireId == null) {
        return i
      }
      if (store.empire_id == empireId) {
        return i
      }
    }
    return -1
  }
}
