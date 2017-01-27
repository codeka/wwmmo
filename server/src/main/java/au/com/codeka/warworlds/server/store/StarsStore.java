package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A special {@link ProtobufStore} for storing stars, including some extra indices for special
 * queries that we can do.
 */
public class StarsStore extends ProtobufStore<Star> {
  private static final Log log = new Log("StarsStore");

  StarsStore(String fileName) {
    super(fileName, Star.class);
  }

  @Nullable
  @Override
  public Star get(long id) {
    try (QueryResult res =
             newReader().stmt("SELECT star FROM stars WHERE id = ?").param(0, id).query()) {
      if (res.next()) {
        return Star.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  @Override
  public void put(long id, Star star) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO stars (id, sector_x, sector_y, last_simulation, star) VALUES (?, ?, ?, ?, ?)")
          .param(0, id)
          .param(1, star.sector_x)
          .param(2, star.sector_y)
          .param(3, star.last_simulation)
          .param(4, star.encode())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  @Nullable
  public Star nextStarForSimulate() {
    // TODO
    return null;
  }

  public ArrayList<Star> getStarsForSector(long sectorX, long sectorY) {
    try (QueryResult res = newReader()
        .stmt("SELECT star FROM stars WHERE sector_x = ? AND sector_y = ?")
        .param(0, sectorX)
        .param(1, sectorY)
        .query()) {
      ArrayList<Star> stars = new ArrayList<>();
      while (res.next()) {
        stars.add(Star.ADAPTER.decode(res.getBytes(0)));
      }
      return stars;
    } catch (Exception e) {
      log.error("Unexpected.", e);
      return null;
    }
  }

  public Iterable<Long> getStarsForEmpire(long empireId) {
    return new ArrayList<>();
  }

  /**
   * We override the {@link ProtobufStore#onOpen(int)} with our own version that adds the extra
   * columns and indices that we care about.
   */
  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE stars (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  sector_x INTEGER," +
                  "  sector_y INTEGER," +
                  "  last_simulation INTEGER," +
                  "  star BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_stars_sector ON stars (sector_x, sector_y)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_stars_last_simulation ON stars (last_simulation)")
          .execute();
      diskVersion++;
    }

    return diskVersion;
  }
}
