package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.store.base.BaseStore
import java.util.*

/**
 * A store for storing stars, including some extra indices for special queries that we can do.
 */
class StarsStore internal constructor(fileName: String) : BaseStore(fileName) {
  operator fun get(id: Long): Star? {
    newReader().stmt("SELECT star FROM stars WHERE id = ?").param(0, id).query().use { res ->
      if (res.next()) {
        return processStar(Star.ADAPTER.decode(res.getBytes(0)))
      }
    }

    // TODO: return null or throw exception?
    return null
  }

  fun put(id: Long, star: Star?) {
    val empireIds: MutableSet<Long> = HashSet()
    for (fleet in star!!.fleets) {
      if (fleet.empire_id != null) {
        empireIds.add(fleet.empire_id)
      }
    }
    for (planet in star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null) {
        empireIds.add(planet.colony.empire_id)
      }
    }
    newTransaction().use { trans ->
      newWriter(trans)
          .stmt("INSERT OR REPLACE INTO stars (id, sector_x, sector_y, next_simulation, star) VALUES (?, ?, ?, ?, ?)")
          .param(0, id)
          .param(1, star.sector_x)
          .param(2, star.sector_y)
          .param(3, star.next_simulation)
          .param(4, star.encode())
          .execute()
      newWriter(trans)
          .stmt("DELETE FROM star_empires WHERE star_id = ?")
          .param(0, id)
          .execute()
      val writer = newWriter(trans)
          .stmt("INSERT INTO star_empires (empire_id, star_id) VALUES (?, ?)")
          .param(1, id)
      for (empireId in empireIds) {
        writer.param(0, empireId).execute()
      }
      trans.commit()
    }
  }

  fun delete(id: Long) {
    newTransaction().use { trans ->
      newWriter(trans)
          .stmt("DELETE FROM star_empires WHERE star_id = ?")
          .param(0, id)
          .execute()
      newWriter(trans)
          .stmt("DELETE FROM stars WHERE id = ?")
          .param(0, id)
          .execute()
      trans.commit()
    }
  }

  fun nextStarForSimulate(): Star? {
    val queue = fetchSimulationQueue(1)
    return if (queue.isEmpty()) {
      null
    } else queue[0]
  }

  /**
   * Fetches all stars that are queued for simulation, in order. [.nextStarForSimulate] will
   * just return the first one.
   */
  fun fetchSimulationQueue(count: Int): ArrayList<Star> {
    newReader()
        .stmt("SELECT star FROM stars WHERE next_simulation IS NOT NULL ORDER BY next_simulation ASC")
        .query().use { res ->
          val stars = ArrayList<Star>()
          while (res.next() && stars.size < count) {
            stars.add(processStar(Star.ADAPTER.decode(res.getBytes(0))))
          }
          return stars
        }
  }

  fun getStarsForSector(sectorX: Long, sectorY: Long): ArrayList<Star> {
    newReader()
        .stmt("SELECT star FROM stars WHERE sector_x = ? AND sector_y = ?")
        .param(0, sectorX)
        .param(1, sectorY)
        .query().use { res ->
          val stars = ArrayList<Star>()
          while (res.next()) {
            stars.add(processStar(Star.ADAPTER.decode(res.getBytes(0))))
          }
          return stars
        }
  }

  /** Do some pre-processing on the star.  */
  private fun processStar(s: Star): Star {
    // TODO: after we've loaded all the stars at least once, remove this logic.
    val sb = s.newBuilder()
    for (i in sb.planets.indices) {
      if (sb.planets[i].colony != null) {
        val cb = sb.planets[i].colony.newBuilder()
        for (j in cb.buildings.indices) {
          val bb = cb.buildings[j].newBuilder()
          if (bb.id == null || bb.id == 0L) {
            bb.id(DataStore.i.seq().nextIdentifier())
            cb.buildings[j] = bb.build()
          }
        }
        sb.planets[i] = sb.planets[i].newBuilder().colony(cb.build()).build()
      }
    }
    return sb.build()
  }

  fun getStarsForEmpire(empireId: Long): ArrayList<Long> {
    newReader()
        .stmt("SELECT star_id FROM star_empires WHERE empire_id = ?")
        .param(0, empireId)
        .query().use { res ->
          val ids = ArrayList<Long>()
          while (res.next()) {
            ids.add(res.getLong(0))
          }
          return ids
        }
  }

  override fun onOpen(diskVersion: Int): Int {
    var version = diskVersion
    if (version == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE stars (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  sector_x INTEGER," +
                  "  sector_y INTEGER," +
                  "  next_simulation INTEGER," +
                  "  star BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_stars_sector ON stars (sector_x, sector_y)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_stars_next_simulation ON stars (next_simulation)")
          .execute()
      newWriter()
          .stmt("CREATE TABLE star_empires (empire_id INTEGER, star_id INTEGER)")
          .execute()
      version++
    }
    if (version == 1) {
      newWriter()
          .stmt("CREATE INDEX IX_star_empires ON star_empires (empire_id, star_id)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_empire_stars ON star_empires (star_id, empire_id)")
          .execute()
      version++
    }
    return version
  }
}