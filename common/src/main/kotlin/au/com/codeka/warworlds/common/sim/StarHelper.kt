package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.EmpireStorage
import au.com.codeka.warworlds.common.proto.Star
import com.squareup.wire.get
import java.util.*
import kotlin.math.roundToInt

/**
 * A helper for working with [Star]s.
 */
object StarHelper {
  /**
   * Gets the [EmpireStorage] for the empire with the given ID.
   */
  fun getStorage(star: Star, empireId: Long): EmpireStorage? {
    for (empireStorage in star.empire_stores) {
      if (empireStorage.empire_id == empireId) {
        return empireStorage
      }
    }
    return null
  }

  /** Gets the delta minerals per hour *at this time* for the given empire.  */
  fun getDeltaMineralsPerHour(star: Star, empireId: Long, now: Long): Float {
    var delta = 0.0f
    val storage = getStorage(star, empireId)
    if (storage != null) {
      delta += get(storage.minerals_delta_per_hour, 0.0f)
    }
    for (planet in star.planets) {
      if (planet.colony?.empire_id != null && planet.colony.empire_id == empireId) {
        for (br in planet.colony.build_requests) {
          if (br.start_time!! < now && br.end_time!! > now) {
            delta += br.delta_minerals_per_hour!!
          }
        }
      }
    }
    return delta
  }

  /**
   * Gets the index of the [EmpireStorage] for the empire with the given ID, or -1 if there's
   * no [EmpireStorage] for that empire.
   */
  fun getStorageIndex(star: Star, empireId: Long?): Int {
    for (i in star.empire_stores.indices) {
      if (star.empire_stores[i].empire_id == empireId) {
        return i
      } else if (empireId == null) {
        return i
      }
    }
    return -1
  }

  /**
   * Gets the index of the [EmpireStorage] for the empire with the given ID, or -1 if there's
   * no [EmpireStorage] for that empire.
   */
  fun getStorageIndex(star: MutableStar, empireId: Long?): Int {
    for (i in star.empireStores.indices) {
      if (star.empireStores[i].empireId == empireId) {
        return i
      }
    }
    return -1
  }

  /**
   * Gets the coordinates string for the given star (something along the lines of "[1.23, 3.45]")
   */
  fun getCoordinateString(star: Star): String {
    return String.format(Locale.US,
        "[%d.%02d, %d.%02d]",
        star.sector_x, (100 * star.offset_x / 1024.0f).roundToInt(),
        star.sector_y, (100 * star.offset_y / 1024.0f).roundToInt())
  }

  /**
   * Calculates a [Vector2] that points from the "from" star to the "to" star.
   *
   *
   * For practical reasons, we assume that the given stars are not *too* far away (that
   * is, that the distance between them can be represented by a 32-bit floating point value).
   */
  fun directionBetween(from: Star, to: Star): Vector2 {
    val sdx = to.sector_x - from.sector_x
    val sdy = to.sector_y - from.sector_y
    return Vector2(
        (to.offset_x - from.offset_x + sdx * 1024.0f).toDouble(),
        (to.offset_y - from.offset_y + sdy * 1024.0f).toDouble())
  }

  fun directionBetween(from: MutableStar, to: Star): Vector2 {
    val sdx = to.sector_x - from.sectorX
    val sdy = to.sector_y - from.sectorY
    return Vector2(
        (to.offset_x - from.offsetX + sdx * 1024.0f).toDouble(),
        (to.offset_y - from.offsetY + sdy * 1024.0f).toDouble())
  }

  fun distanceBetween(from: Star, to: Star): Double {
    return directionBetween(from, to).length()
  }

  fun distanceBetween(from: MutableStar, to: Star): Double {
    return directionBetween(from, to).length()
  }
}