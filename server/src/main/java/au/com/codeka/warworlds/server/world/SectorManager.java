package au.com.codeka.warworlds.server.world;

import com.google.common.base.Preconditions;

import java.util.HashMap;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.generator.SectorGenerator;

/**
 * Manages the sectors we have loaded.
 */
public class SectorManager {
  public static final SectorManager i = new SectorManager();
  public static final int SECTOR_SIZE = 1024;

  private final HashMap<SectorCoord, WatchableObject<Sector>> sectors = new HashMap<>();

  /** Gets the sector with the given {@link SectorCoord}, creating a new one if necessary. */
  public WatchableObject<Sector> getSector(@Nonnull SectorCoord coord) {
    synchronized (sectors) {
      WatchableObject<Sector> sector = sectors.get(coord);
      if (sector == null) {
        Sector s = DataStore.i.sectors().getSector(coord.x, coord.y);
        if (s == null) {
          s = new SectorGenerator().generate(coord.x, coord.y);
        }
        sector = new WatchableObject<>(s);
        sectors.put(coord, sector);

        // Watch all the stars so that we can update the sector when the star is updated.
        WatchableObject.Watcher<Star> watcher = new StarWatcher(coord);
        for (Star sectorStar : sector.get().stars) {
          WatchableObject<Star> star = StarManager.i.getStar(sectorStar.id);
          star.addWatcher(watcher);
        }
      }
      return sector;
    }
  }

  /**
   * Called in the very rare situations where we need to forget the whole sector (for example,
   * when a star is deleted). Package private because we shouldn't normally want to call this
   * directly.
   */
  void forgetSector(@Nonnull SectorCoord coord) {
    synchronized (sectors) {
      sectors.remove(coord);
    }
  }

  /**
   * Go through all of the stars in the given {@link Sector} and make sure any stars which are
   * eligible for a native colony have one.
   */
  public void verifyNativeColonies(WatchableObject<Sector> sector) {
    for (Star star : sector.get().stars) {
      // If there's any fleets on it, it's not eligible.
      if (star.fleets.size() > 0) {
        continue;
      }

      // If there's any colonies, it's also not eligible.
      int numColonies = 0;
      for (Planet planet: star.planets) {
        if (planet.colony != null) {
          numColonies ++;
        }
      }
      if (numColonies > 0) {
        continue;
      }

      // If it was emptied < 3 days ago, it's not eligible.
      if (star.time_emptied != null
          && (System.currentTimeMillis() - star.time_emptied) < (3 * Time.DAY)) {
        continue;
      }

      // If there's no planets with a population congeniality above 500, it's not eligible.
      int numEligiblePlanets = 0;
      for (Planet planet: star.planets) {
        if (planet.population_congeniality > 500) {
          numEligiblePlanets ++;
        }
      }
      if (numEligiblePlanets == 0) {
        continue;
      }

      // Looks like it's eligible, let's do it.
      StarManager.i.addNativeColonies(star.id);
    }
  }

  private class StarWatcher implements WatchableObject.Watcher<Star> {
    private final SectorCoord coord;

    public StarWatcher(SectorCoord coord) {
      this.coord = Preconditions.checkNotNull(coord);
    }

    @Override
    public void onUpdate(WatchableObject<Star> object) {
      WatchableObject<Sector> sector = sectors.get(coord);
      Sector.Builder newSector = sector.get().newBuilder();
      for (int i = 0; i < newSector.stars.size(); i++) {
        if (newSector.stars.get(i).id.equals(object.get().id)) {
          newSector.stars.remove(i);
          break;
        }
      }
      newSector.stars.add(object.get());

      sector.set(newSector.build());
    }
  }
}
