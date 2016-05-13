package au.com.codeka.warworlds.server.world;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

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

  private class StarWatcher implements WatchableObject.Watcher<Star> {
    private final SectorCoord coord;

    public StarWatcher(SectorCoord coord) {
      this.coord = Preconditions.checkNotNull(coord);
    }

    @Override
    public void onUpdate(WatchableObject<Star> object) {
      WatchableObject<Sector> sector = sectors.get(coord);
      List<Star> stars = sector.get().stars;
      for (int i = 0; i < stars.size(); i++) {
        if (stars.get(i).id.equals(object.get().id)) {
          stars.remove(i);
          break;
        }
      }
      stars.add(object.get());

      sector.set(sector.get().newBuilder().stars(stars).build());
    }
  }
}
