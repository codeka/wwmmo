package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.proto.Sector
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.world.generator.SectorGenerator
import com.google.common.base.Preconditions
import java.util.*

/**
 * Manages the sectors we have loaded.
 */
class SectorManager {
  private val sectors = HashMap<SectorCoord, WatchableObject<Sector>>()

  /** Gets the sector with the given [SectorCoord], creating a new one if necessary.  */
  fun getSector(coord: SectorCoord): WatchableObject<Sector> {
    synchronized(sectors) {
      var sector = sectors[coord]
      if (sector == null) {
        var s: Sector? = DataStore.i.sectors().getSector(coord.x, coord.y)
        if (s!!.state == SectorState.New.value) {
          s = SectorGenerator().generate(s)
        }
        sector = WatchableObject(s!!)
        sectors[coord] = sector

        // Watch all the stars so that we can update the sector when the star is updated.
        val watcher: WatchableObject.Watcher<Star> = StarWatcher(coord)
        for (sectorStar in sector.get().stars) {
          val star: WatchableObject<Star>? = StarManager.i.getStar(sectorStar.id)
          star?.addWatcher(watcher)
        }
      }
      return sector
    }
  }

  /**
   * Called in the very rare situations where we need to forget the whole sector (for example,
   * when a star is deleted). Package private because we shouldn't normally want to call this
   * directly.
   */
  fun forgetSector(coord: SectorCoord) {
    synchronized(sectors) { sectors.remove(coord) }
  }

  /**
   * Go through all of the stars in the given [Sector] and make sure any stars which are
   * eligible for a native colony have one.
   */
  fun verifyNativeColonies(sector: WatchableObject<Sector>) {
    for (star in sector.get().stars) {
      // If there's any fleets on it, it's not eligible.
      if (star.fleets.size > 0) {
        continue
      }

      // If there's any colonies, it's also not eligible.
      var numColonies = 0
      for (planet in star.planets) {
        if (planet.colony != null) {
          numColonies++
        }
      }
      if (numColonies > 0) {
        continue
      }

      // If it was emptied < 3 days ago, it's not eligible.
      if (star.time_emptied != null
          && System.currentTimeMillis() - star.time_emptied < 3 * Time.DAY) {
        continue
      }

      // If there's no planets with a population congeniality above 500, it's not eligible.
      var numEligiblePlanets = 0
      for (planet in star.planets) {
        if (planet.population_congeniality > 500) {
          numEligiblePlanets++
        }
      }
      if (numEligiblePlanets == 0) {
        continue
      }

      // Looks like it's eligible, let's do it.
      StarManager.Companion.i.addNativeColonies(star.id)
    }
  }

  private inner class StarWatcher(coord: SectorCoord) : WatchableObject.Watcher<Star> {
    private val coord: SectorCoord
    override fun onUpdate(`object`: WatchableObject<Star>) {
      val sector = sectors[coord]!!
      val newSector = sector.get()!!.newBuilder()
      for (i in newSector.stars.indices) {
        if (newSector.stars[i].id == `object`.get().id) {
          newSector.stars.removeAt(i)
          break
        }
      }
      newSector.stars.add(`object`.get())
      sector.set(newSector.build())
    }

    init {
      this.coord = Preconditions.checkNotNull(coord)
    }
  }

  companion object {
    val i = SectorManager()
    const val SECTOR_SIZE = 1024
  }
}