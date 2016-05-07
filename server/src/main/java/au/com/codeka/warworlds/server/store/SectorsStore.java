package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

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
public class SectorsStore {
  private static final DatabaseEntry UNGENERATED_SECTORS_KEY =
      new DatabaseEntry("ungenerated-sectors".getBytes(Charset.defaultCharset()));
  private static final DatabaseEntry EMPTY_SECTORS_KEY =
      new DatabaseEntry("empty-sectors".getBytes(Charset.defaultCharset()));
  private static final DatabaseEntry UNIVERSE_BOUNDS_KEY =
      new DatabaseEntry("universe-bounds".getBytes(Charset.defaultCharset()));

  private final Database db;
  private final ProtobufStore<Star> starsStore;
  private final ProtobufSerializer<IdentifierArray> idsArraySerializer;
  private final ProtobufSerializer<SectorCoordArray> sectorCoordArraySerializer;

  public SectorsStore(Database db, ProtobufStore<Star> starsStore) {
    this.db = Preconditions.checkNotNull(db);
    this.starsStore = Preconditions.checkNotNull(starsStore);
    idsArraySerializer = new ProtobufSerializer<>(IdentifierArray.class);
    sectorCoordArraySerializer = new ProtobufSerializer<>(SectorCoordArray.class);
  }

  /**
   * Gets the sector at the given x,y coordinates. Returns null if we don't have that sector stored
   * yet.
   */
  @Nullable
  public Sector getSector(long x, long y) {
    DatabaseEntry key = makeKey(x, y);
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = db.get(null, key, value, LockMode.DEFAULT);
    if (status != OperationStatus.SUCCESS) {
      return null;
    }
    List<Long> ids = idsArraySerializer.deserialize(value).ids;

    ArrayList<Star> stars = new ArrayList<>();
    for (long id : ids) {
      stars.add(Preconditions.checkNotNull(starsStore.get(id)));
    }

    return new Sector.Builder()
        .x(x)
        .y(y)
        .stars(stars)
        .build();
  }

  /**
   * Creates a new sector in the store. We assume the sector does not already exist, and if it does
   * then an {@link IllegalStateException} will be thrown.
   *
   * TODO: make this a transaction. Can you even do that across databases?
   */
  public void createSector(Sector sector) {
    ArrayList<Long> ids = new ArrayList<>();
    for (Star star : sector.stars) {
      ids.add(star.id);
      starsStore.put(star.id, star);
    }

    DatabaseEntry key = makeKey(sector.x, sector.y);
    DatabaseEntry value =
        idsArraySerializer.serialize(new IdentifierArray.Builder().ids(ids).build());
    db.put(null, key, value);

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
    DatabaseEntry value = new DatabaseEntry();
    if (db.get(null, EMPTY_SECTORS_KEY, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      List<SectorCoord> coords = sectorCoordArraySerializer.deserialize(value).coords;

      // Find the one closest to (0,0)
      SectorCoord closest = null;
      float closestDistance = 0;
      for (SectorCoord coord : coords) {
        // square of the distance, actually, but it doesn't matter
        float distance = (coord.x * coord.x) + (coord.y * coord.y);
        if (closest == null || distance < closestDistance) {
          closest = coord;
          closestDistance = distance;
        }
      }

      return closest;
    } else {
      return null;
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

    while (coords.size() < minSectors) {
      DatabaseEntry value = new DatabaseEntry();
      if (db.get(null, UNGENERATED_SECTORS_KEY, value, LockMode.DEFAULT)
          == OperationStatus.SUCCESS) {
        for (SectorCoord coord : sectorCoordArraySerializer.deserialize(value).coords) {
          coords.add(coord);
        }
      }

      if (coords.size() < minSectors) {
        expandUniverse();
      }
    }

    return new ArrayList<>(coords);
  }

  /** Adds the given {@link SectorCoord} to our list of empty sectors. */
  private void addEmptySector(SectorCoord coord) {
    Transaction trans = db.getEnvironment().beginTransaction(null, null);
    try {
      List<SectorCoord> coords;
      DatabaseEntry value = new DatabaseEntry();
      if (db.get(trans, EMPTY_SECTORS_KEY, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        coords = sectorCoordArraySerializer.deserialize(value).coords;
      } else {
        coords = new ArrayList<>();
      }
      coords.add(coord);
      value = sectorCoordArraySerializer.serialize(
          new SectorCoordArray.Builder().coords(coords).build());
      db.put(trans, EMPTY_SECTORS_KEY, value);
      trans.commit();
      trans = null;
    } finally {
      if (trans != null) {
        trans.abort();
      }
    }
  }

  /**
   * Expands the universe, making it one bigger than before, and creating a bunch of sectors to be
   * generated.
   */
  private void expandUniverse() {
    Transaction trans = db.getEnvironment().beginTransaction(null, null);
    try {
      List<SectorCoord> bounds;
      DatabaseEntry value = new DatabaseEntry();
      if (db.get(trans, UNIVERSE_BOUNDS_KEY, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        bounds = sectorCoordArraySerializer.deserialize(value).coords;
        Preconditions.checkState(bounds.size() == 2,
            "Expected universe bounds to be size==2, found size==%d", bounds.size());
      } else {
        bounds = new ArrayList<>();
        bounds.add(new SectorCoord.Builder().x(0L).y(0L).build());
        bounds.add(new SectorCoord.Builder().x(0L).y(0L).build());
      }

      bounds.set(0, new SectorCoord.Builder()
          .x(bounds.get(0).x - 1)
          .y(bounds.get(0).y - 1)
          .build());
      bounds.set(1, new SectorCoord.Builder()
          .x(bounds.get(1).x + 1)
          .y(bounds.get(1).y + 1)
          .build());
      value = sectorCoordArraySerializer.serialize(
          new SectorCoordArray.Builder().coords(bounds).build());
      db.put(trans, UNIVERSE_BOUNDS_KEY, value);

      // Now save the newly-created ungenerated sectors.
      List<SectorCoord> coords;
      value = new DatabaseEntry();
      if (db.get(trans, UNGENERATED_SECTORS_KEY, value, LockMode.DEFAULT)
          == OperationStatus.SUCCESS) {
        coords = sectorCoordArraySerializer.deserialize(value).coords;
      } else {
        coords = new ArrayList<>();
      }
      for (long x = bounds.get(0).x; x <= bounds.get(1).x; x++) {
        coords.add(new SectorCoord.Builder().x(x).y(bounds.get(0).y).build());
        coords.add(new SectorCoord.Builder().x(x).y(bounds.get(1).y).build());
      }
      for (long y = bounds.get(0).y; y <= bounds.get(1).y; y++) {
        coords.add(new SectorCoord.Builder().x(bounds.get(0).x).y(y).build());
        coords.add(new SectorCoord.Builder().x(bounds.get(1).x).y(y).build());
      }
      db.put(
          trans,
          UNGENERATED_SECTORS_KEY,
          sectorCoordArraySerializer.serialize(
              new SectorCoordArray.Builder().coords(coords).build()));

      trans.commit();
      trans = null;
    } finally {
      if (trans != null) {
        trans.abort();
      }
    }
  }

  /**
   * Remove the given {@link SectorCoord} from the "ungenerated" sectors list.
   */
  private void removeUngeneratedSector(SectorCoord coord) {
    Transaction trans = db.getEnvironment().beginTransaction(null, null);
    try {
      List<SectorCoord> coords;
      DatabaseEntry value = new DatabaseEntry();
      if (db.get(trans, UNGENERATED_SECTORS_KEY, value, LockMode.DEFAULT)
          == OperationStatus.SUCCESS) {
        coords = sectorCoordArraySerializer.deserialize(value).coords;
      } else {
        return;
      }
      for (int i = 0; i < coords.size(); i++) {
        if (coords.get(i).equals(coord)) {
          coords.remove(i);
          break;
        }
      }
      db.put(
          trans,
          UNGENERATED_SECTORS_KEY,
          sectorCoordArraySerializer.serialize(
              new SectorCoordArray.Builder().coords(coords).build()));

      trans.commit();
      trans = null;
    } finally {
      if (trans != null) {
        trans.abort();
      }
    }
  }


  private DatabaseEntry makeKey(long x, long y) {
    String key = String.format(Locale.US, "sector:%d,%d", x, y);
    return new DatabaseEntry(key.getBytes(Charset.defaultCharset()));
  }
}
