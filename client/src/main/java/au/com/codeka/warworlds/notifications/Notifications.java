package au.com.codeka.warworlds.notifications;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.game.DesignHelper;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;

import org.jetbrains.annotations.Nullable;

public class Notifications {
  private static final Log log = new Log("Notifications");

  private Notifications() {
  }

  public static void clearNotifications() {
    NotificationManager nm =
        (NotificationManager) App.i.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.cancel(RealmContext.i.getCurrentRealm().getID());

    new DatabaseHelper().clearNotifications();
  }

  public static void handleNotification(Context context, String name, String value) {
    log.info("Got notification: %s %s", name, value);

    switch (name) {
      case "sitrep":
        handleSitrepNotification(context, value);
        break;
      case "chat":
        handleChatNotification(context, value);
        break;
      case "debug-msg":
        handleDebugMsgNotification(context, value);
        break;
      case "cash":
        handleCashNotification(value);
        break;
      case "blitz_reset":
        handleBlitzResetNotification(context);
        break;
      default:
        log.error("Unknown notification name: %s", name);
    }
  }

  private static void handleSitrepNotification(Context context, String value) {
    byte[] blob = Base64.decode(value, Base64.DEFAULT);

    Messages.SituationReport pb;
    try {
      pb = Messages.SituationReport.parseFrom(blob);
    } catch (InvalidProtocolBufferException e) {
      log.error("Could not parse situation report!", e);
      return;
    }

    // we could currently be in a game, and that game could be running in a different
    // realm to this notification. So we switch this thread temporarily to whatever
    // realm this notification is for.
    Realm thisRealm = RealmManager.i.getRealmByName(pb.getRealm());
    log.debug("Got realm: %s", thisRealm.getDisplayName());
    RealmContext.i.setThreadRealm(thisRealm);
    try {
      // refresh the star this situation report is for, obviously
      // something happened that we'll want to know about
      boolean refreshed = StarManager.i.refreshStar(Integer.parseInt(pb.getStarKey()), true, null);
      log.debug("Refreshed star: successful? %s", refreshed ? "false" : "true");
      if (!refreshed) { // <-- only refresh the star if we have one cached
        // if we didn't refresh the star, then at least refresh
        // the sector it was in (could have been a moving
        // fleet, say)
        Star star = SectorManager.i.findStar(pb.getStarKey());
        if (star != null) {
          log.debug("Star found from sector manager instead.");
          SectorManager.i.refreshSector(star.getSectorX(), star.getSectorY());
        }
      }

      log.debug("Displaying a notification...");
      displayNotification(context, pb);
    } finally {
      RealmContext.i.setThreadRealm(null);
    }
  }

  private static void handleChatNotification(Context context, String value) {
    byte[] blob = Base64.decode(value, Base64.DEFAULT);

    ChatMessage msg;
    Messages.ChatMessage pb;
    try {
      pb = Messages.ChatMessage.parseFrom(blob);
      msg = new ChatMessage();
      msg.fromProtocolBuffer(pb);
    } catch (InvalidProtocolBufferException e) {
      log.error("Could not parse chat message!", e);
      return;
    }

    ChatConversation conversation = ChatManager.i.getConversation(msg);
    if (conversation != null) {
      ChatManager.i.addMessage(conversation, msg);

      if (conversation.isPrivateChat() && BackgroundDetector.i.isInBackground()) {
        if (conversation.isMuted()) {
          log.debug("Got notification, but conversation is muted.");
        } else {
          // if it's a private chat, and we're currently in the background, show a notification
          displayNotification(context, pb);
        }
      }
    }
  }

  private static void handleDebugMsgNotification(Context context, String value) {
    displayNotification(context, value);
  }

  private static void handleCashNotification(String value) {
    float newCash = Float.parseFloat(value);
    MyEmpire empire = EmpireManager.i.getEmpire();
    if (empire != null) {
      empire.updateCash(newCash);
      EmpireManager.eventBus.publish(empire);
    }
  }

  private static void handleBlitzResetNotification(Context context) {
    log.info("Blitz reset notification received.");
    // TODO??
//    ServerGreeter.clearHello();
    BackgroundDetector.i.resetBackStack();
  }

  private static void displayNotification(final Context context,
      final Messages.ChatMessage chatmsg) {
    NotificationDetails notification = new NotificationDetails();
    notification.chatMsg = chatmsg;
    notification.realm = RealmContext.i.getCurrentRealm();

    DatabaseHelper db = new DatabaseHelper();
    if (!db.addNotification(notification)) {
      displayNotification(buildNotification(context, db.getNotifications()));
    }
  }

  private static void displayNotification(final Context context,
      final Messages.SituationReport sitrep) {
    String starKey = sitrep.getStarKey();
    Futures.addCallback(
        StarManager.i.requireStar(Integer.parseInt(starKey)), new FutureCallback<Star>() {
          @Override
          public void onSuccess(@Nullable Star star) {
            if (star != null) {
              displayNotification(context, star, sitrep);
            }
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            log.error("Error fetching star, cannot show notification.", t);
          }
        }, ContextCompat.getMainExecutor(context));
  }

  private static void displayNotification(
      final Context context, Star star, Messages.SituationReport sitrep) {
    NotificationDetails notification = new NotificationDetails();
    notification.sitrep = sitrep;
    notification.realm = RealmContext.i.getCurrentRealm();

    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    star.toProtocolBuffer(star_pb);
    notification.star = star_pb.build();

    DatabaseHelper db = new DatabaseHelper();
    if (!db.addNotification(notification)) {
      displayNotification(buildNotification(context, db.getNotifications()));
    } else {
      log.info("Notification already in database, not displaying a again.");
    }
  }

  private static void displayNotification(final Context context, final String debugMsg) {
    NotificationDetails notification = new NotificationDetails();
    notification.debugMsg = debugMsg;
    notification.realm = RealmContext.i.getCurrentRealm();

    DatabaseHelper db = new DatabaseHelper();
    if (!db.addNotification(notification)) {
      displayNotification(buildNotification(context, db.getNotifications()));
    } else {
      log.info("Notification already in database, not displaying a again.");
    }
  }

  private static void displayNotification(Notification notification) {
    if (notification == null) {
      log.info("No notification.");
      return;
    }

    if (!new GlobalOptions().notificationsEnabled()) {
      log.info("Global notifications option is disabled.");
      return;
    }

    log.info("OK sending");
    NotificationManager nm =
        (NotificationManager) App.i.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(RealmContext.i.getCurrentRealm().getID(), notification);
  }

  private static Notification buildNotification(Context context,
      List<NotificationDetails> notifications) {
    // we want to add the StarfieldActivity to the "back stack" of the situation report so that
    // when you press "back" from the sitrep you go to the starfield.

    /*
    TODO: we need to start the main activity if it's not already started...
    Intent intent = new Intent(context, SitrepFragment.class);
    intent.putExtra("au.com.codeka.warworlds.RealmID", notifications.get(0).realm.getID());
    PendingIntent pendingIntent =
        TaskStackBuilder.create(context).addParentStack(SitrepFragment.class).addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
     */


    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//    builder.setContentIntent(pendingIntent);
    builder.setSmallIcon(R.drawable.status_icon);
    builder.setAutoCancel(true);

    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

    GlobalOptions.NotificationOptions options = null;
    int num = 0;
    boolean first = true;
    for (NotificationDetails notification : notifications) {
      GlobalOptions.NotificationOptions thisOptions = null;
      if (notification.sitrep != null) {
        thisOptions = addSitrepNotification(context, notification, builder, inboxStyle, first);
      } else if (notification.chatMsg != null) {
        thisOptions = addChatNotification(context, notification, builder, inboxStyle, first);
      } else if (notification.debugMsg != null) {
        thisOptions = addDebugMsgNotification(context, notification, builder, inboxStyle, first);
      }

      if (thisOptions == null) {
        // we didn't display this one, so just continue...
        continue;
      }
      options = thisOptions;

      first = false;
      num++;
    }

    if (num == 0) {
      return null;
    }

    Realm realm = RealmContext.i.getCurrentRealm();
    inboxStyle.setBigContentTitle(
        String.format(Locale.US, "%s: %d events", realm.getDisplayName(), num));
    builder.setStyle(inboxStyle);
    builder.setNumber(num);
    builder.setLights(options.getLedColour(), 1000, 5000);

    return builder.build();
  }

  private static GlobalOptions.NotificationOptions addSitrepNotification(Context context,
      NotificationDetails notification, NotificationCompat.Builder builder,
      NotificationCompat.InboxStyle inboxStyle, boolean first) {
    SituationReport sitrep = SituationReport.fromProtocolBuffer(notification.sitrep);

    Star star = new Star();
    star.fromProtocolBuffer(notification.star);

    GlobalOptions.NotificationKind kind = getNotificationKind(sitrep);
    GlobalOptions.NotificationOptions options = new GlobalOptions().getNotificationOptions(kind);
    if (!options.isEnabled()) {
      log.debug("Notification disabled by options, not displaying.");
      return null;
    }

    if (first) {
      try {
        @SuppressLint("InlinedApi")
        int iconWidth = context.getResources().getDimensionPixelSize(
            android.R.dimen.notification_large_icon_width);
        @SuppressLint("InlinedApi")
        int iconHeight = context.getResources().getDimensionPixelSize(
            android.R.dimen.notification_large_icon_height);

        Bitmap largeIcon = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(largeIcon);

        Design design = sitrep.getDesign();
        if (design != null) {
          Matrix matrix = new Matrix();
          matrix.setScale((float) iconWidth / 64f, (float) iconHeight / 64f);

          canvas.save();
          canvas.setMatrix(matrix);
          canvas.drawBitmap(DesignHelper.load(design), matrix, new Paint());
          canvas.restore();
        }

        Sprite starSprite = StarImageManager.getInstance().getSprite(star, iconWidth / 2, true);
        starSprite.draw(canvas);

        builder.setLargeIcon(largeIcon);
      } catch (Resources.NotFoundException e) {
        // Probably an old version of Android that doesn't support large icons. Ignore.
      }

      builder.setContentTitle(sitrep.getTitle());
      builder.setContentText(sitrep.getDescription(star));
      builder.setWhen(sitrep.getReportTime().getMillis());

      String ringtone = new GlobalOptions().getNotificationOptions(kind).getRingtone();
      if (ringtone != null && ringtone.length() > 0) {
        builder.setSound(Uri.parse(ringtone));
      }
    }

    ensureChannel(
        context, "game-events", "Game events",
        "Events that happen within the game (fleet moves, attacks, etc)");
    builder.setChannelId("game-events");

    // subsequent notifications go in the expanded view
    inboxStyle.addLine(Html.fromHtml(sitrep.getSummaryLine(star)));

    return options;
  }

  private static GlobalOptions.NotificationOptions addChatNotification(Context context,
      NotificationDetails notification, NotificationCompat.Builder builder,
      NotificationCompat.InboxStyle inboxStyle, boolean first) {
    ChatMessage msg = new ChatMessage();
    msg.fromProtocolBuffer(notification.chatMsg);

    GlobalOptions.NotificationKind kind = GlobalOptions.NotificationKind.CHAT_MESSAGE;
    GlobalOptions.NotificationOptions options = new GlobalOptions().getNotificationOptions(kind);
    if (!options.isEnabled()) {
      return null;
    }

    String msgContent = msg.formatPlain(new GlobalOptions().autoTranslateChatMessages());
    String empireName = "Chat";

    if (first) {
      Empire empire = EmpireManager.i.getEmpire(Integer.parseInt(msg.getEmpireKey()));
      if (empire != null) {
        empireName = empire.getDisplayName();

        Bitmap shield = EmpireShieldManager.i.getShield(context, empire);
        if (shield != null) {
          builder.setLargeIcon(shield);
        }

        builder.setContentTitle(empire.getDisplayName());
      }

      builder.setContentText(msgContent);
      builder.setWhen(msg.getDatePosted().getMillis());

      String ringtone = new GlobalOptions().getNotificationOptions(kind).getRingtone();
      if (ringtone != null && ringtone.length() > 0) {
        builder.setSound(Uri.parse(ringtone));
      }
    }

    ensureChannel(context, "chat", "Chat messages", "DMs and other chat messages");
    builder.setChannelId("chat");

    // subsequent notifications go in the expanded view
    inboxStyle.addLine(Html.fromHtml("<b>" + empireName + "</b>: " + msgContent));

    return options;
  }

  private static GlobalOptions.NotificationOptions addDebugMsgNotification(
      Context context,
      NotificationDetails notification, NotificationCompat.Builder builder,
      NotificationCompat.InboxStyle inboxStyle, boolean first) {
    GlobalOptions.NotificationKind kind = GlobalOptions.NotificationKind.CHAT_MESSAGE;
    GlobalOptions.NotificationOptions options = new GlobalOptions().getNotificationOptions(kind);
    if (!options.isEnabled()) {
      return null;
    }

    if (first) {
      builder.setContentTitle("Server Message");
      builder.setContentText(notification.debugMsg);
      builder.setWhen(System.currentTimeMillis());

      String ringtone = new GlobalOptions().getNotificationOptions(kind).getRingtone();
      if (ringtone != null && ringtone.length() > 0) {
        builder.setSound(Uri.parse(ringtone));
      }
    }

    ensureChannel(context, "debug-msg", "Server message", "Special messages from the server.");
    builder.setChannelId("debug-msg");

    // subsequent notifications go in the expanded view
    inboxStyle.addLine(Html.fromHtml("<b>Server Message</b>: " + notification.debugMsg));

    return options;
  }

  private static void ensureChannel(Context context, String id, String name, String description) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationChannel channel =
        new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription(description);
    notificationManager.createNotificationChannel(channel);
  }

  private static GlobalOptions.NotificationKind getNotificationKind(SituationReport sitrep) {
    if (sitrep.getBuildCompleteRecord() != null) {
      if (sitrep.getBuildCompleteRecord().getDesignKind() == DesignKind.BUILDING) {
        return GlobalOptions.NotificationKind.BUILDING_BUILD_COMPLETE;
      } else {
        return GlobalOptions.NotificationKind.FLEET_BUILD_COMPLETE;
      }
    }

    if (sitrep.getFleetUnderAttackRecord() != null) {
      return GlobalOptions.NotificationKind.FLEET_UNDER_ATTACK;
    }

    if (sitrep.getMoveCompleteRecord() != null) {
      return GlobalOptions.NotificationKind.FLEET_MOVE_COMPLETE;
    }

    if (sitrep.getFleetDestroyedRecord() != null) {
      return GlobalOptions.NotificationKind.FLEET_DESTROYED;
    }

    if (sitrep.getFleetVictoriousRecord() != null) {
      return GlobalOptions.NotificationKind.FLEET_VICTORIOUS;
    }

    if (sitrep.getColonyDestroyedRecord() != null) {
      return GlobalOptions.NotificationKind.COLONY_DESTROYED;
    }

    if (sitrep.getColonyAttackedRecord() != null) {
      return GlobalOptions.NotificationKind.COLONY_ATTACKED;
    }

    if (sitrep.getStarRunOutOfGoodsRecord() != null) {
      return GlobalOptions.NotificationKind.STAR_GOODS_ZERO;
    }

    return GlobalOptions.NotificationKind.OTHER;
  }

  /**
   * We store all the unacknowledged notifications in a small sqlite database, this
   * is the helper for that database.
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper() {
      super(App.i, "notifications.db", null, 6);
    }

    /**
     * This is called the first time we open the database, in order to create the required
     * tables, etc.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(
          "CREATE TABLE notifications ("
              + "  id INTEGER PRIMARY KEY,"
              + "  realm_id INTEGER,"
              + "  star BLOB,"
              + "  sitrep BLOB,"
              + "  chat_msg BLOB,"
              + "  debug_msg TEXT,"
              + "  timestamp INTEGER,"
              + "  sitrep_key TEXT,"
              + "  chat_msg_id INTEGER)");
      db.execSQL("CREATE INDEX IX_realm_id_timestamp ON notifications (realm_id, timestamp)");
      db.execSQL("CREATE INDEX IX_sitrep_key ON notifications(sitrep_key)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      if (oldVersion < 2) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN realm_id INTEGER DEFAULT "
            + RealmManager.BETA_REALM_ID);
        db.execSQL("CREATE INDEX IX_realm_id_timestamp ON notifications (realm_id, timestamp)");
      }
      if (oldVersion < 3) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN sitrep_key STRING");
        db.execSQL("CREATE INDEX IX_sitrep_key ON notifications(sitrep_key)");
      }
      if (oldVersion < 4) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN chat_msg BLOB");
      }
      if (oldVersion < 5) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN chat_msg_id INTEGER");
      }
      if (oldVersion < 6) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN debug_msg TEXT");
      }
    }

    /**
     * Adds a notification to the database.
     *
     * @return true if the notification already existed.
     */
    public boolean addNotification(NotificationDetails details) {
      SQLiteDatabase db = getWritableDatabase();
      try {
        // if there's an existing one, delete it first
        int rows = 0;
        if (details.sitrep != null && details.sitrep.getKey() != null
            && details.sitrep.getKey().length() > 0) {
          rows = db.delete("notifications",
              "sitrep_key = '" + details.sitrep.getKey() + "' AND realm_id = " + details.realm
                  .getID(), null);
        } else if (details.chatMsg != null && details.chatMsg.getId() > 0) {
          rows = db.delete("notifications",
              "chat_msg_id = '" + details.chatMsg.getId() + "' AND realm_id = " + details.realm
                  .getID(), null);
        } else if (details.debugMsg != null) {
          // Note: we never delete a debug message.
        }

        ByteArrayOutputStream sitrep = new ByteArrayOutputStream();
        ByteArrayOutputStream star = new ByteArrayOutputStream();
        ByteArrayOutputStream chatMsg = new ByteArrayOutputStream();
        try {
          if (details.sitrep != null) {
            details.sitrep.writeTo(sitrep);
          }
          if (details.star != null) {
            details.star.writeTo(star);
          }
          if (details.chatMsg != null) {
            details.chatMsg.writeTo(chatMsg);
          }
        } catch (IOException e) {
          // we won't get the notification, but not the end of the world...
          log.error("Error serializing notification details.", e);
          return false;
        }

        ContentValues values = new ContentValues();
        if (details.star != null) {
          values.put("star", star.toByteArray());
        }
        if (details.sitrep != null) {
          values.put("sitrep", sitrep.toByteArray());
          values.put("sitrep_key", details.sitrep.getKey());
          values.put("timestamp", details.sitrep.getReportTime());
        }
        if (details.chatMsg != null) {
          values.put("chat_msg", chatMsg.toByteArray());
          values.put("chat_msg_id", details.chatMsg.getId());
          values.put("timestamp", details.chatMsg.getDatePosted());
        }
        if (details.debugMsg != null) {
          values.put("debug_msg", details.debugMsg);
        }
        values.put("realm_id", details.realm.getID());
        db.insert("notifications", null, values);

        return (rows > 0);
      } finally {
        db.close();
      }
    }

    public List<NotificationDetails> getNotifications() {
      ArrayList<NotificationDetails> notifications = new ArrayList<>();

      Realm realm = RealmContext.i.getCurrentRealm();
      SQLiteDatabase db = getReadableDatabase();
      try {
        Cursor cursor = db.query("notifications",
            new String[] {"star", "sitrep", "chat_msg", "debug_msg"},
            "realm_id = " + realm.getID(), null, null, null, "timestamp DESC");
        if (!cursor.moveToFirst()) {
          cursor.close();
          return notifications;
        }

        do {
          try {
            NotificationDetails notification = new NotificationDetails();
            byte[] blob = cursor.getBlob(0);
            if (blob != null && blob.length > 0) {
              notification.star = Messages.Star.parseFrom(blob);
            }
            blob = cursor.getBlob(1);
            if (blob != null && blob.length > 0) {
              notification.sitrep = Messages.SituationReport.parseFrom(blob);
            }
            blob = cursor.getBlob(2);
            if (blob != null && blob.length > 0) {
              notification.chatMsg = Messages.ChatMessage.parseFrom(blob);
            }
            String text = cursor.getString(3);
            if (text != null && !text.isEmpty()) {
              notification.debugMsg = text;
            }
            notification.realm = realm;
            notifications.add(notification);
          } catch (InvalidProtocolBufferException e) {
            // any errors here and we'll just skip this notification
          } catch (IllegalStateException e) {
            // we can sometimes get this if there's issues with the database
            break;
          }
        } while (cursor.moveToNext());
        cursor.close();
      } catch (Exception e) {
        log.error("Error fetching notifications.", e);
      } finally {
        db.close();
      }

      return notifications;
    }

    public void clearNotifications() {
      Realm realm = RealmContext.i.getCurrentRealm();

      SQLiteDatabase db = getWritableDatabase();
      try {
        db.execSQL("DELETE FROM notifications WHERE realm_id=" + realm.getID());
      } finally {
        db.close();
      }
    }
  }

  private static class NotificationDetails {
    public Messages.SituationReport sitrep;
    public Messages.Star star;
    public Messages.ChatMessage chatMsg;
    public String debugMsg;
    public Realm realm;
  }
}
