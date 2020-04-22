package au.com.codeka.warworlds.client.store

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.ChatMessage
import java.io.IOException
import java.util.*

/**
 * Store for storing chats, etc.
 */
class ChatStore(private val name: String, private val helper: SQLiteOpenHelper) : BaseStore(name, helper) {
  override fun onCreate(db: SQLiteDatabase?) {
    db!!.execSQL(
        "CREATE TABLE " + name + "_rooms ("
            + "  id INTEGER PRIMARY KEY,"
            + "  room BLOB)")
    db.execSQL(
        "CREATE TABLE " + name + "_messages ("
            + "  id INTEGER PRIMARY KEY,"
            + "  room_id INTEGER,"
            + "  date_posted INTEGER,"
            + "  msg BLOB)")
    db.execSQL(
        "CREATE INDEX IX_messages_room_date_posted ON " + name + "_messages ("
            + "  room_id, date_posted, id)")
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

  /** Adds the given messages to the store.  */
  fun addMessages(msgs: List<ChatMessage>) {
    val db = helper.writableDatabase
    val contentValues = ContentValues()
    for (msg in msgs) {
      contentValues.put("id", msg.id)
      contentValues.put("room_id", msg.room_id)
      contentValues.put("date_posted", msg.date_posted)
      contentValues.put("msg", msg.encode())
      db.insertWithOnConflict(
          name + "_messages", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }
  }

  /** Gets count messages starting from startTime and going back in time.  */
  fun getMessages(roomId: Long?, startTime: Long, count: Int): List<ChatMessage?> {
    var query: String
    query = if (roomId == null) {
      "room_id IS NULL"
    } else {
      "room_id = ?"
    }
    query += " AND date_posted <= ?"
    val queryArgs = arrayOfNulls<String>(if (roomId == null) 1 else 2)
    var index = 0
    if (roomId != null) {
      queryArgs[index++] = String.format(Locale.US, "%d", roomId)
    }
    queryArgs[index] = String.format(Locale.US, "%d", startTime)
    val msgs = ArrayList<ChatMessage?>()
    val db = helper.readableDatabase
    try {
      db.query(
          name + "_messages", arrayOf("msg"),
          query /* selection */,
          queryArgs /* selectionArgs */,
          null /* groupBy */,
          null /* having */,
          "date_posted DESC" /* orderBy */, String.format(Locale.US, "%d", count)).use { cursor ->
        while (cursor.moveToNext()) {
          msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)))
        }
        Collections.reverse(msgs) // Make it oldest-first.
      }
    } catch (e: IOException) {
      log.error("Error fetching chat messages.", e)
    }
    return msgs
  }

  /** Gets count messages starting from startTime and going back in time.  */
  fun getMessagesAfter(roomId: Long?, time: Long): List<ChatMessage?> {
    var query: String
    query = if (roomId == null) {
      "room_id IS NULL"
    } else {
      "room_id = ?"
    }
    query += " AND date_posted > ?"
    val queryArgs = arrayOfNulls<String>(if (roomId == null) 1 else 2)
    var index = 0
    if (roomId != null) {
      queryArgs[index++] = String.format(Locale.US, "%d", roomId)
    }
    queryArgs[index] = String.format(Locale.US, "%d", time)
    val msgs = ArrayList<ChatMessage?>()
    val db = helper.readableDatabase
    try {
      db.query(
          name + "_messages", arrayOf("msg"),
          query /* selection */,
          queryArgs /* selectionArgs */,
          null /* groupBy */,
          null /* having */,
          "date_posted DESC" /* orderBy */).use { cursor ->
        while (cursor.moveToNext()) {
          msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)))
        }
        Collections.reverse(msgs) // Make it oldest-first.
      }
    } catch (e: IOException) {
      log.error("Error fetching chat messages.", e)
    }
    return msgs
  }

  /** Gets all messages, regardless of room, from the given start time.  */
  fun getMessages(time: Long, count: Int): List<ChatMessage?> {
    val msgs = ArrayList<ChatMessage?>()
    val db = helper.readableDatabase
    try {
      db.query(
          name + "_messages", arrayOf("msg"),
          "date_posted <= ?" /* selection */, arrayOf(String.format(Locale.US, "%d", time)),
          null /* groupBy */,
          null /* having */,
          "date_posted DESC" /* orderBy */, String.format(Locale.US, "%d", count)).use { cursor ->
        while (cursor.moveToNext()) {
          msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)))
        }
        Collections.reverse(msgs) // Make it oldest-first.
      }
    } catch (e: IOException) {
      log.error("Error fetching chat messages.", e)
    }
    return msgs
  }

  val lastChatTime: Long
    get() {
      val db = helper.readableDatabase
      db.query(
          false, name + "_messages", arrayOf("date_posted"), null,
          null, null, null, "date_posted DESC", null).use { cursor ->
        if (cursor.moveToFirst()) {
          return cursor.getLong(0)
        }
      }
      return 0
    }

  fun removeHistory() {
    val db = helper.readableDatabase
    db.execSQL("DELETE FROM " + name + "_messages")
  }

  companion object {
    private val log = Log("ChatStore")
  }

}