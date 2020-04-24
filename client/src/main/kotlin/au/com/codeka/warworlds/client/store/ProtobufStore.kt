package au.com.codeka.warworlds.client.store

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.squareup.wire.Message

/** Class for storing protos in a sqlite key-value store, keyed by a long id.  */
class ProtobufStore<M : Message<*, *>?>(
    private val name: String, protoClass: Class<M>, private val helper: SQLiteOpenHelper)
  : BaseStore(name, helper) {
  private val serializer: ProtobufSerializer<M>
  override fun onCreate(db: SQLiteDatabase?) {
    db!!.execSQL(
        "CREATE TABLE " + name + " ("
            + "  key INTEGER PRIMARY KEY,"
            + "  value BLOB)")
  }

  /**
   * Gets the value with the given ID, or `null` if it's not found.
   */
  operator fun get(key: Long): M? {
    val db = helper.readableDatabase
    db.query(false, name, arrayOf("value"),
        "key = ?", arrayOf(java.lang.Long.toString(key)),
        null, null, null, null).use { cursor ->
      if (cursor.moveToFirst()) {
        return serializer.deserialize(cursor.getBlob(0))
      }
    }
    return null
  }

  /**
   * Puts the given value to the data store.
   */
  fun put(id: Long, value: M) {
    val db = helper.writableDatabase
    val values = ContentValues()
    values.put("key", id)
    values.put("value", serializer.serialize(value))
    db.insertWithOnConflict(name, null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  /** Puts all of the given values into the data store in a single transaction.  */
  fun putAll(values: Map<Long?, M>) {
    val db = helper.writableDatabase
    db.beginTransaction()
    try {
      val contentValues = ContentValues()
      for ((key, value) in values) {
        contentValues.put("key", key)
        contentValues.put("value", serializer.serialize(value))
        db.insertWithOnConflict(name, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  init {
    serializer = ProtobufSerializer(protoClass)
  }
}