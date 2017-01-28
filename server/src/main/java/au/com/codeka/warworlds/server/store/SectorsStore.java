package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
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

  public enum SectorState {
    Empty(1),
    NonEmpty(2),
    Abandoned(3);

    private int value;

    SectorState(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    public static SectorState fromValue(int value) {
      for (SectorState state : values()) {
        if (state.getValue() == value) {
          return state;
        }
      }
      return Empty;
    }
  }

  public SectorsStore(String fileName) {
    super(fileName);
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

    try {
      newWriter()
          .stmt("INSERT INTO sectors (x, y, distance_to_centre, state) VALUES (?, ?, ?, ?)")
          .param(0, sector.x)
          .param(1, sector.y)
          .param(2, Math.sqrt(sector.x * sector.x + sector.y * sector.y))
          .param(3, SectorState.Empty.getValue())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  /**
   * Finds a sector in the given state, as close to the center of the universe as possible.
   *
   * @return The {@link SectorCoord} of a sector in the given state, or null if no such sector is
   *         found.
   */
  @Nullable
  public SectorCoord findSectorByState(SectorState state) {
    try (QueryResult res = newReader()
        .stmt("SELECT x, y FROM sectors WHERE state = ? ORDER BY distance_to_centre ASC")
        .param(0, state.getValue())
        .query()) {
      if (res.next()) {
        return new SectorCoord.Builder().x(res.getLong(0)).y(res.getLong(1)).build();
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  /**
   * Update the given sector's state.
   */
  public void updateSectorState(SectorCoord coord, SectorState state) {
    try {
      newWriter()
          .stmt("UPDATE sectors SET state = ? WHERE x = ? AND y = ?")
          .param(0, state.getValue())
          .param(1, coord.x)
          .param(2, coord.y)
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
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
          .stmt("CREATE TABLE sectors (" +
                "  x INTEGER," +
                "  y INTEGER," +
                "  distance_to_centre FLOAT," +
                "  state INTEGER)")
          .execute();
      diskVersion ++;
    }
    return diskVersion;
  }
}
