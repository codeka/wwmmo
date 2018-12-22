package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.store.base.StoreWriter;
import au.com.codeka.warworlds.server.store.base.Transaction;

/**
 * A store for storing stars, including some extra indices for special queries that we can do.
 */
public class StarsStore extends BaseStore {
  private static final Log log = new Log("StarsStore");

  StarsStore(String fileName) {
    super(fileName);
  }

  @Nullable
  public Star get(long id) {
    try (QueryResult res =
             newReader().stmt("SELECT star FROM stars WHERE id = ?").param(0, id).query()) {
      if (res.next()) {
        return processStar(Star.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public void put(long id, Star star) {
    Set<Long> empireIds = new HashSet<>();
    for (Fleet fleet : star.fleets) {
      if (fleet.empire_id != null) {
        empireIds.add(fleet.empire_id);
      }
    }
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null) {
        empireIds.add(planet.colony.empire_id);
      }
    }

    try(Transaction trans = newTransaction()) {
      newWriter(trans)
          .stmt("INSERT OR REPLACE INTO stars (id, sector_x, sector_y, next_simulation, star) VALUES (?, ?, ?, ?, ?)")
          .param(0, id)
          .param(1, star.sector_x)
          .param(2, star.sector_y)
          .param(3, star.next_simulation)
          .param(4, star.encode())
          .execute();

      newWriter(trans)
          .stmt("DELETE FROM star_empires WHERE star_id = ?")
          .param(0, id)
          .execute();

      StoreWriter writer = newWriter(trans)
          .stmt("INSERT INTO star_empires (empire_id, star_id) VALUES (?, ?)")
          .param(1, id);
      for (Long empireId : empireIds) {
        writer.param(0, empireId)
            .execute();
      }

      trans.commit();
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
  }

  public void delete(long id) {
    try (Transaction trans = newTransaction()) {
      newWriter(trans)
          .stmt("DELETE FROM star_empires WHERE star_id = ?")
          .param(0, id)
          .execute();

      newWriter(trans)
          .stmt("DELETE FROM stars WHERE id = ?")
          .param(0, id)
          .execute();

      trans.commit();
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
  }

  @Nullable
  public Star nextStarForSimulate() {
    try (
        QueryResult res = newReader()
            .stmt("SELECT star FROM stars WHERE next_simulation IS NOT NULL ORDER BY next_simulation ASC")
            .query()) {
      if (res.next()) {
        return processStar(Star.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  /**
   * Fetches all stars that are queued for simulation, in order. {@link #nextStarForSimulate()} will
   * just return the first one.
   */
  public ArrayList<Star> fetchSimulationQueue(int count) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT star FROM stars WHERE next_simulation IS NOT NULL ORDER BY next_simulation ASC")
            .query()) {
      ArrayList<Star> stars = new ArrayList<>();
      while (res.next() && stars.size() < count) {
        stars.add(processStar(Star.ADAPTER.decode(res.getBytes(0))));
      }
      return stars;
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return new ArrayList<>();
  }

  public ArrayList<Star> getStarsForSector(long sectorX, long sectorY) {
    try (QueryResult res = newReader()
        .stmt("SELECT star FROM stars WHERE sector_x = ? AND sector_y = ?")
        .param(0, sectorX)
        .param(1, sectorY)
        .query()) {
      ArrayList<Star> stars = new ArrayList<>();
      while (res.next()) {
        stars.add(processStar(Star.ADAPTER.decode(res.getBytes(0))));
      }
      return stars;
    } catch (Exception e) {
      log.error("Unexpected.", e);
      return null;
    }
  }

  /** Do some pre-processing on the star. */
  private Star processStar(Star s) {
    // TODO: after we've loaded all the stars at least once, remove this logic.
    Star.Builder sb = s.newBuilder();
    for (int i = 0 ; i < sb.planets.size(); i++) {
      if (sb.planets.get(i).colony != null) {
        Colony.Builder cb = sb.planets.get(i).colony.newBuilder();
        for (int j = 0; j < cb.buildings.size(); j++) {
          Building.Builder bb = cb.buildings.get(j).newBuilder();
          if (bb.id == null || bb.id == 0) {
            bb.id(DataStore.i.seq().nextIdentifier());
            cb.buildings.set(j, bb.build());
          }
        }
        sb.planets.set(i, sb.planets.get(i).newBuilder().colony(cb.build()).build());
      }
    }
    return sb.build();
  }

  public ArrayList<Long> getStarsForEmpire(long empireId) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT star_id FROM star_empires WHERE empire_id = ?")
            .param(0, empireId)
            .query()) {
      ArrayList<Long> ids = new ArrayList<>();
      while (res.next()) {
        ids.add(res.getLong(0));
      }
      return ids;
    } catch (Exception e) {
      log.error("Unexpected.", e);
      return new ArrayList<>();
    }
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE stars (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  sector_x INTEGER," +
                  "  sector_y INTEGER," +
                  "  next_simulation INTEGER," +
                  "  star BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_stars_sector ON stars (sector_x, sector_y)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_stars_next_simulation ON stars (next_simulation)")
          .execute();

      newWriter()
          .stmt("CREATE TABLE star_empires (empire_id INTEGER, star_id INTEGER)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_star_empires ON star_empires (empire_id, star_id)");
      newWriter()
          .stmt("CREATE INDEX IX_empire_stars ON star_empires (star_id, empire_id)");

      diskVersion++;
    }

    return diskVersion;
  }
}
