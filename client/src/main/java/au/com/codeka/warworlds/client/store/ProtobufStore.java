package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.common.base.Preconditions;
import com.squareup.wire.Message;

import java.util.Map;

/** Class for storing protos in a sqlite key-value store, keyed by a long id. */
public class ProtobufStore<M extends Message<?, ?>> {
  private final String name;
  private final SQLiteOpenHelper helper;
  private final ProtobufSerializer<M> serializer;

  public ProtobufStore(String name, Class<M> protoClass, SQLiteOpenHelper helper) {
    this.name = Preconditions.checkNotNull(name);
    this.helper = Preconditions.checkNotNull(helper);
    this.serializer = new ProtobufSerializer<>(protoClass);
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE " + name + " ("
            + "  key INTEGER PRIMARY KEY,"
            + "  value BLOB)");
  }

  public M get(long id) {
    SQLiteDatabase db = helper.getReadableDatabase();
    try (Cursor cursor = db.query(false, name, new String[]{"value"},
        "key = ?", new String[]{Long.toString(id)},
        null, null, null, null)) {
      if (cursor.moveToFirst()) {
        return serializer.deserialize(cursor.getBlob(0));
      }
    }

    return null;
  }

  public void put(long id, M value) {
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
