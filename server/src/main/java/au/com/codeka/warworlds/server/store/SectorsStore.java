package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.IdentifierArray;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.SectorCoordArray;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Sectors store is a special store for storing the details of the sectors.
 *
 * <p>We don't serialize the actual {@link Sector} object, since that includes a list of derived
 * objects (stars and colonies, etc). We also need to be able to store things like the list of
 * currently-empty sectors, the current bounds of the universe and so on.
 */
public class SectorsStore extends BaseStore {
  private final Log log = new Log("SectorsStore");
  private final ProtobufSerializer<IdentifierArray> idsArraySerializer;
  private final ProtobufSerializer<SectorCoordArray> sectorCoordArraySerializer;

  public SectorsStore(String fileName) {
    super(fileName);

    idsArraySerializer = new ProtobufSerializer<>(IdentifierArray.class);
    sectorCoordArraySerializer = new ProtobufSerializer<>(SectorCoordArray.class);
  }

  /**
   * Gets the sector at the given x,y coordinates. Returns null if we don't have that sector stored
   * yet.
   */
  @Nullable
  public Sector getSector(long x, long y) {
    ArrayList<Star> stars = DataStore.i.stars().getStarsForSector(x, y);
    if (stars.isEmpty()) {
      return null;
    }

    return new Sector.Builder().x(x).y(y).stars(stars).build();
  }

  /**
   * Creates a new sector in the store. We assume the sector does not already exist, and if it does
   * then an {@link IllegalStateException} will be thrown.
   *
   * TODO: make this a transaction. Can you even do that across databases?
   */
  public void createSector(Sector sector) {
    for (Star star : sector.stars) {
      DataStore.i.stars().put(star.id, star);
    }

    SectorCoord coord = new SectorCoord.Builder().x(sector.x).y(sector.y).build();
    addEmptySector(coord);
    removeUngeneratedSector(coord);
  }

  /**
   * Returns a single empty sector, as close to the center of the universe as possible.
   *
   * @return The {@link SectorCoord} of an empty sector, or null if there are no empty sectors left.
   */
  @Nullable
  public SectorCoord getEmptySector() {
    // TODO:
    return new SectorCoord.Builder().x(0L).y(0L).build();
  }

  /**
   * Remove the given {@link SectorCoord} from the "empty" set: we'll no longer return that sector
   * from {@link #getEmptySector()}.
   */
  public void removeEmptySector(SectorCoord coord) {
    // TODO
  }

  /**
   * Gets a list of sectors that we haven't currently generated yet. The sectors will be as close
   * to the center of the universe as possible, and serve as good candidates for new empires.
   *
   * @param minSectors The minimum number of sectors you want. We'll make sure to return at least
   *                   that number (though maybe more).
   * @return A list of {@link SectorCoord}s for each ungenerated sector.
   */
  public List<SectorCoord> getUngeneratedSectors(int minSectors) {
    HashSet<SectorCoord> coords = new HashSet<>();
    // TODO
    return new ArrayList<>(coords);
  }

  /** Adds the given {@link SectorCoord} to our list of empty sectors. */
  private void addEmptySector(SectorCoord coord) {
    //TODO
  }

  /**
   * Expands the universe, making it one bigger than before, and creating a bunch of sectors to be
   * generated.
   */
  private void expandUniverse() {
    //TODO
  }

  /**
   * Remove the given {@link SectorCoord} from the "ungenerated" sectors list.
   */
  private void removeUngeneratedSector(SectorCoord coord) {
    //TODO
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE sectors (x INTEGER, y INTEGER)")
          .execute();
      diskVersion ++;
    }
    return diskVersion;
  }
}
