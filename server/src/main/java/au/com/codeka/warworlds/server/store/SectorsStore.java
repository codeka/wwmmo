package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.store.base.Transaction;

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
    /** A brand new sector, hasn't even had stars generated for it yet. */
    New(0),

    /** The sector is being generated, don't use yet (but also don't try to re-generate it) */
    Generating(4),

    /** An empty sector, only stars with native colonies exist. */
    Empty(1),

    /** A normal sector, has stars with colonies. */
    NonEmpty(2),

    /** An abandoned sector, has stars with colonies that have been abandoned by a player. */
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
   * Gets the sector at the given x,y coordinates. Returns a blank sector in state "new" if we
   * don't have one created yet.
   */
  public Sector getSector(long x, long y) {
    Sector.Builder sectorBuilder = new Sector.Builder();

    try (QueryResult res = newReader()
            .stmt("SELECT state FROM sectors WHERE x = ? AND y = ?")
            .param(0, x)
            .param(1, y)
            .query()) {
      if (res.next()) {
        SectorState state = SectorState.fromValue(res.getInt(0));
        log.info("Got sector state: %d", state.getValue());
        sectorBuilder.state(state.getValue());
      } else {
        // No sector yet.
        log.info("No sector at (%d, %d)", x, y);
        sectorBuilder.state(SectorState.New.getValue());
      }
    } catch (Exception e) {
      log.error("Unexpected error fetching sector state.", e);
      throw new RuntimeException("Unexpected.", e);
    }

    if (sectorBuilder.state != SectorState.New.getValue()) {
      sectorBuilder.stars(DataStore.i.stars().getStarsForSector(x, y));
    }

    return sectorBuilder
            .x(x)
            .y(y)
            .build();
  }

  /**
   * Finds a sector in the given state, as close to the center of the universe as possible.
   *
   * @return The {@link SectorCoord} of a sector in the given state, or null if no such sector is
   *         found.
   */
  @Nullable
  public SectorCoord findSectorByState(SectorState state) {
    ArrayList<SectorCoord> sectorCoords = findSectorsByState(state, 1);
    if (sectorCoords.isEmpty()) {
      return null;
    }
    return sectorCoords.get(0);
  }

  /**
   * Find the top {@code count} sectors in the given state, ordered by how far they are from the
   * center of the universe.
   *
   * @param state The {@link SectorState} you want to find sectors in.
   * @param count The number of sectors to return.
   * @return An array of {@link SectorCoord} of at most {@code count} sectors, ordered by their
   *         distance to the center of the universe.
   */
  public ArrayList<SectorCoord> findSectorsByState(SectorState state, int count) {
    try (QueryResult res = newReader()
        .stmt("SELECT x, y FROM sectors WHERE state = ? ORDER BY distance_to_centre ASC")
        .param(0, state.getValue())
        .query()) {
      ArrayList<SectorCoord> coords = new ArrayList<>(count);
      while (res.next() && coords.size() < count) {
        coords.add(new SectorCoord.Builder().x(res.getLong(0)).y(res.getLong(1)).build());
      }
      return coords;
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return new ArrayList<>();
  }

  /**
   * Update the given sector's state.
   *
   * @return true if the sector was updated, false if it was not (because the current state didn't
   * match most likely).
   */
  public boolean updateSectorState(SectorCoord coord, SectorState currState, SectorState newState) {
    try {
      int count = newWriter()
          .stmt("UPDATE sectors SET state = ? WHERE x = ? AND y = ? AND state = ?")
          .param(0, newState.getValue())
          .param(1, coord.x)
          .param(2, coord.y)
          .param(3, currState.getValue())
          .execute();
      if (count == 0 && currState == SectorState.New) {
        insertNewSector(coord);
        return true;
      }
      return count == 1;
    } catch (StoreException e) {
      log.error("Unexpected.", e);
      return false;
    }
  }

  /**
   * Expands the universe, making it one bigger than before, and creating a bunch of sectors to be
   * generated.
   */
  public void expandUniverse() {
    try (Transaction trans = newTransaction()) {
      // Find the current bounds of the universe.
      long minX = 0, minY = 0, maxX = 0, maxY = 0;
      try (
          QueryResult res = newReader(trans)
              .stmt("SELECT MIN(x), MIN(y), MAX(x), MAX(y) FROM sectors")
              .query()
      ) {
        if (res.next()) {
          minX = res.getLong(0);
          minY = res.getLong(1);
          maxX = res.getLong(2);
          maxY = res.getLong(3);
        }
      } catch (Exception e) {
        log.error("Unexpected.", e);
      }

      // Find any sectors that are missing within that bounds.
      ArrayList<SectorCoord> missing = new ArrayList<>();
      for (long y = minY; y <= maxY; y++) {
        Set<Long> xs = new HashSet<>();
        try (
            QueryResult res = newReader(trans)
                .stmt("SELECT x FROM sectors WHERE y = ?")
                .param(0, y)
                .query()
        ) {
          while (res.next()) {
            xs.add(res.getLong(0));
          }
        } catch (Exception e) {
          log.error("Unexpected.", e);
        }

        for (long x = minX; x <= maxX; x++) {
          if (!xs.contains(x)) {
            missing.add(new SectorCoord.Builder().x(x).y(y).build());
          }
        }
      }

      // If there's no (or not many) gaps, expand the universe by one and add all of those instead.
      if (missing.size() < 10) {
        for (long x = minX - 1; x <= maxX + 1; x++) {
          missing.add(new SectorCoord.Builder().x(x).y(minY - 1).build());
          missing.add(new SectorCoord.Builder().x(x).y(maxY + 1).build());
        }
        for (long y = minY; y <= maxY; y++) {
          missing.add(new SectorCoord.Builder().x(minX - 1).y(y).build());
          missing.add(new SectorCoord.Builder().x(maxX + 1).y(y).build());
        }
      }

      // Now add all the new sectors.
      for (SectorCoord coord : missing) {
        insertNewSector(coord);
      }

      trans.commit();
    } catch(Exception e) {
      log.error("Unexpected.", e);
    }
  }

  private void insertNewSector(SectorCoord coord) throws StoreException{
    newWriter()
            .stmt("INSERT INTO sectors (x, y, distance_to_centre, state) VALUES (?, ?, ?, ?)")
            .param(0, coord.x)
            .param(1, coord.y)
            .param(2, Math.sqrt(coord.x * coord.x + coord.y * coord.y))
            .param(3, SectorState.New.getValue())
            .execute();
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
