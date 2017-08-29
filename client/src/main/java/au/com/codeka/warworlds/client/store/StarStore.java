package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Specialization of {@link BaseStore} which stores stars. We have a custom class here because
 * we want to add some extra columns for indexing.
 */
public class StarStore extends BaseStore {
  private final String name;
  private final SQLiteOpenHelper helper;

  public StarStore(String name, SQLiteOpenHelper helper) {
    super (name, helper);
    this.name = name;
    this.helper = helper;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE " + name + " ("
            + "  key INTEGER PRIMARY KEY,"
            + "  my_empire INTEGER," // 1 if my empire has something on this star, 0 if not.
            + "  last_simulation INTEGER,"
            + "  name STRING,"
            + "  value BLOB)");
    db.execSQL(
        "CREATE INDEX IX_" + name + "_my_empire_name ON " + name + " (my_empire, name)");
  }

  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }

  public StarCursor getMyStars() {
    return new StarCursor(helper.getReadableDatabase().query(
        name,
        new String[] { "value" } /* columns */,
        "my_empire = 1" /* selection */,
        null /* selectionArgs */,
        null /* groupBy */,
        null /* having */,
        null /* orderBy */));
  }

  public StarCursor searchMyStars(String search) {
    String likeOperand = search.replace("%", "%%") + "%";
    return new StarCursor(helper.getReadableDatabase().query(
        name,
        new String[] { "value" } /* columns */,
        "my_empire = 1 AND name LIKE ?" /* selection */,
        new String[] { likeOperand } /* selectionArgs */,
        null /* groupBy */,
        null /* having */,
        null /* orderBy */));
  }

  /**
   * Gets the most recent value of last_simulation out of all our empire's stars. See {@link
   * au.com.codeka.warworlds.client.game.world.StarManager} for details.
   */
  public Long getLastSimulationOfOurStar() {
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            false, name, new String[]{ "last_simulation" }, "my_empire=1",
            null, null, null, "last_simulation DESC", null)) {
      if (cursor.moveToFirst()) {
        return cursor.getLong(0);
      }
    }

    return null;
  }

  /**
   * Gets the {@link Star} with the given ID, or {@code null} if it's not found.
   */
  public Star get(long key) {
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            false, name, new String[]{ "value" }, "key = ?", new String[]{ Long.toString(key) },
            null, null, null, null)) {
      if (cursor.moveToFirst()) {
        try {
          return Star.ADAPTER.decode(cursor.getBlob(0));
        } catch (IOException e) {
          return null;
        }
      }
    }

    return null;
  }

  /**
   * Puts the given value to the data store.
   */
  public void put(long id, @Nonnull Star star, @Nonnull Empire myEmpire) {
    SQLiteDatabase db = helper.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put("key", id);
    contentValues.put("my_empire", isMyStar(star, myEmpire) ? 1 : 0);
    contentValues.put("last_simulation", star.last_simulation);
    contentValues.put("value", star.encode());
    db.insertWithOnConflict(name, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
  }

  /** Puts all of the given values into the data store in a single transaction. */
  public void putAll(Map<Long, Star> values, @Nonnull Empire myEmpire) {
    SQLiteDatabase db = helper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues contentValues = new ContentValues();
      for (Map.Entry<Long, Star> kvp : values.entrySet()) {
        contentValues.put("key", kvp.getKey());
        contentValues.put("my_empire", isMyStar(kvp.getValue(), myEmpire) ? 1 : 0);
        contentValues.put("last_simulation", kvp.getValue().last_simulation);
        contentValues.put("value", kvp.getValue().encode());
        db.insertWithOnConflict(name, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private static boolean isMyStar(Star star, Empire myEmpire) {
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null) {
        if (planet.colony.empire_id.equals(myEmpire.id)) {
          return true;
        }
      }
    }
    for (Fleet fleet : star.fleets) {
      if (fleet.empire_id != null && fleet.empire_id.equals(myEmpire.id)) {
        return true;
      }
    }
    return false;
  }
}
