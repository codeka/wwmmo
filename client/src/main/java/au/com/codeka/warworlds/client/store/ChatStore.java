package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ChatMessage;

/**
 * Store for storing chats, etc.
 */
public class ChatStore extends BaseStore {
  private static final Log log = new Log("ChatStore");

  private final String name;
  private final SQLiteOpenHelper helper;

  public ChatStore(String name, SQLiteOpenHelper helper) {
    super(name, helper);
    this.name = name;
    this.helper = helper;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE " + name + "_rooms ("
            + "  id INTEGER PRIMARY KEY,"
            + "  room BLOB)");
    db.execSQL(
        "CREATE TABLE " + name + "_messages ("
            + "  id INTEGER PRIMARY KEY,"
            + "  room_id INTEGER,"
            + "  date_posted INTEGER,"
            + "  msg BLOB)");
    db.execSQL(
        "CREATE INDEX IX_messages_room_date_posted ON " + name + "_messages ("
            + "  room_id, date_posted, id)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }

  /** Adds the given messages to the store. */
  public void addMessages(List<ChatMessage> msgs) {
    SQLiteDatabase db = helper.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    for (ChatMessage msg : msgs) {
      contentValues.put("id", msg.id);
      contentValues.put("room_id", msg.room_id);
      contentValues.put("date_posted", msg.date_posted);
      contentValues.put("msg", msg.encode());
      db.insertWithOnConflict(
          name + "_messages", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }
  }

  /** Gets count messages starting from startTime and going back in time. */
  public List<ChatMessage> getMessages(Long roomId, long startTime, int count) {
    String query;
    if (roomId == null) {
      query = "room_id IS NULL";
    } else {
      query = "room_id = ?";
    }
    query += " AND date_posted <= ?";

    String[] queryArgs = new String[roomId == null ? 1 : 2];
    int index = 0;
    if (roomId != null) {
      queryArgs[index++] = String.format(Locale.US, "%d", roomId);
    }
    queryArgs[index] = String.format(Locale.US, "%d", startTime);

    ArrayList<ChatMessage> msgs = new ArrayList<>();
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            name + "_messages",
            new String[] { "msg" } /* columns */,
            query /* selection */,
            queryArgs/* selectionArgs */,
            null /* groupBy */,
            null /* having */,
            "date_posted DESC" /* orderBy */,
            String.format(Locale.US, "%d", count) /* limit */)) {
      while (cursor.moveToNext()) {
        msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)));
      }
      Collections.reverse(msgs); // Make it oldest-first.
    } catch (IOException e) {
      log.error("Error fetching chat messages.", e);
    }
    return msgs;
  }

  /** Gets count messages starting from startTime and going back in time. */
  public List<ChatMessage> getMessagesAfter(Long roomId, long time) {
    String query;
    if (roomId == null) {
      query = "room_id IS NULL";
    } else {
      query = "room_id = ?";
    }
    query += " AND date_posted > ?";

    String[] queryArgs = new String[roomId == null ? 1 : 2];
    int index = 0;
    if (roomId != null) {
      queryArgs[index++] = String.format(Locale.US, "%d", roomId);
    }
    queryArgs[index] = String.format(Locale.US, "%d", time);

    ArrayList<ChatMessage> msgs = new ArrayList<>();
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            name + "_messages",
            new String[] { "msg" } /* columns */,
            query /* selection */,
            queryArgs/* selectionArgs */,
            null /* groupBy */,
            null /* having */,
            "date_posted DESC" /* orderBy */)) {
      while (cursor.moveToNext()) {
        msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)));
      }
      Collections.reverse(msgs); // Make it oldest-first.
    } catch (IOException e) {
      log.error("Error fetching chat messages.", e);
    }
    return msgs;
  }

  /** Gets all messages, regardless of room, from the given start time. */
  public List<ChatMessage> getMessages(long time, int count) {
    ArrayList<ChatMessage> msgs = new ArrayList<>();
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            name + "_messages",
            new String[] { "msg" } /* columns */,
            "date_posted <= ?" /* selection */,
            new String[] { String.format(Locale.US, "%d", time) } /* selectionArgs */,
            null /* groupBy */,
            null /* having */,
            "date_posted DESC" /* orderBy */,
            String.format(Locale.US, "%d", count))) {
      while (cursor.moveToNext()) {
        msgs.add(ChatMessage.ADAPTER.decode(cursor.getBlob(0)));
      }
      Collections.reverse(msgs); // Make it oldest-first.
    } catch (IOException e) {
      log.error("Error fetching chat messages.", e);
    }
    return msgs;
  }

  public long getLastChatTime() {
    SQLiteDatabase db = helper.getReadableDatabase();
    try (
        Cursor cursor = db.query(
            false, name + "_messages", new String[]{ "date_posted" }, null,
            null, null, null, "date_posted DESC", null)) {
      if (cursor.moveToFirst()) {
        return cursor.getLong(0);
      }
    }

    return 0;
  }

  public void removeHistory() {
    SQLiteDatabase db = helper.getReadableDatabase();
    db.execSQL("DELETE FROM " + name + "_messages");
  }
}
