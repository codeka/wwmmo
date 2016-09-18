package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.wire.Message;

import java.util.Map;

import javax.annotation.Nonnull;

/** Class for storing protos in a sqlite key-value store, keyed by a long id. */
public class ProtobufStore<M extends Message<?, ?>> extends BaseStore<Long, M> {
  private final String name;
  private final ProtobufSerializer<M> serializer;
  private final SQLiteOpenHelper helper;

  public ProtobufStore(String name, Class<M> protoClass, SQLiteOpenHelper helper) {
    super(name, helper);
    this.name = name;
    this.helper = helper;
    this.serializer = new ProtobufSerializer<>(protoClass);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE " + name + " ("
            + "  key INTEGER PRIMARY KEY,"
            + "  value BLOB)");
  }


  /**
   * Gets the value with the given ID, or {@code null} if it's not found.
   */
  public M get(long key) {
    SQLiteDatabase db = helper.getReadableDatabase();
    try (Cursor cursor = db.query(false, name, new String[]{ "value" },
        "key = ?", new String[]{ Long.toString(key) },
        null, null, null, null)) {
      if (cursor.moveToFirst()) {
        return serializer.deserialize(cursor.getBlob(0));
      }
    }

    return null;
  }

  /**
   * Puts the given value to the data store.
   */
  public void put(long id, @Nonnull M value) {
    SQLiteDatabase db = helper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("key", id);
    values.put("value", serializer.serialize(value));
    db.insertWithOnConflict(name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  /** Puts all of the given values into the data store in a single transaction. */
  public void putAll(Map<Long, M> values) {
    SQLiteDatabase db = helper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues contentValues = new ContentValues();
      for (Map.Entry<Long, M> kvp : values.entrySet()) {
        contentValues.put("key", kvp.getKey());
        contentValues.put("value", serializer.serialize(kvp.getValue()));
        db.insertWithOnConflict(name, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
