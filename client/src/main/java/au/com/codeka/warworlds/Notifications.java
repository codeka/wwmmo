
package au.com.codeka.warworlds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.Base64;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.game.SitrepActivity;
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
import au.com.codeka.warworlds.model.StarSummary;

import com.google.protobuf.InvalidProtocolBufferException;

public class Notifications {
    private static final Log log = new Log("Notifications");

    private static NotificationLongPoller sLongPoller;

    public static void startLongPoll() {
        if (sLongPoller != null) {
            return;
        }

        sLongPoller = new NotificationLongPoller();
        sLongPoller.start();
    }

    private Notifications() {
    }

    public static void clearNotifications() {
        NotificationManager nm = (NotificationManager) App.i.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(RealmContext.i.getCurrentRealm().getID());

        new DatabaseHelper().clearNotifications();
    }

    public static void handleNotfication(Context context, String name, String value) {
        if (name.equals("sitrep")) {
            handleSitrepNotification(context, value);
        } else if (name.equals("chat")) {
            handleChatNotification(context, value);
        } else if (name.equals("cash")) {
            handleCashNotification(value);
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
            boolean refreshed = StarManager.i.refreshStar(Integer.parseInt(pb.getStarKey()), true);
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
        } catch(InvalidProtocolBufferException e) {
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

    private static void handleCashNotification(String value) {
        float newCash = Float.parseFloat(value);
        MyEmpire empire = EmpireManager.i.getEmpire();
        empire.updateCash(newCash);
        EmpireManager.eventBus.publish(empire);
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
        Star star = StarManager.i.getStar(Integer.parseInt(starKey));
        if (star == null) {
            log.error("Could not get star summary for star %s, cannot display notification.",
                    starKey);
            return;
        }

        NotificationDetails notification = new NotificationDetails();
        notification.sitrep = sitrep;
        notification.realm = RealmContext.i.getCurrentRealm();

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        notification.star = star_pb.build();

        DatabaseHelper db = new DatabaseHelper();
        if (!db.addNotification(notification)) {
            displayNotification(buildNotification(context, db.getNotifications()));
        }
    }

    private static void displayNotification(Notification notification) {
        if (notification == null) {
            return;
        }

        if (!new GlobalOptions().notificationsEnabled()) {
            return;
        }

        NotificationManager nm = (NotificationManager) App.i.getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.notify(RealmContext.i.getCurrentRealm().getID(), notification);
    }

    private static Notification buildNotification(Context context,
                                                  List<NotificationDetails> notifications) {
        // we want to add the StarfieldActivity to the "back stack" of the situation report so that
        // when you press "back" from the sitrep you go to the starfield.
        Intent intent = new Intent(context, SitrepActivity.class);
        intent.putExtra("au.com.codeka.warworlds.RealmID", notifications.get(0).realm.getID());
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                                        .addParentStack(SitrepActivity.class)
                                        .addNextIntent(intent)
                                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendingIntent);
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
            }

            if (thisOptions == null) {
                // we didn't display this one, so just continue...
                continue;
            }
            options = thisOptions;

            first = false;
            num++;
        }

        if (num == 0 || options == null) {
            return null;
        }

        Realm realm = RealmContext.i.getCurrentRealm();
        inboxStyle.setBigContentTitle(String.format("%s: %d events", realm.getDisplayName(), num));
        builder.setStyle(inboxStyle);
        builder.setNumber(num);
        builder.setLights(options.getLedColour(), 1000, 5000);

        return builder.build();
    }

    private static GlobalOptions.NotificationOptions addSitrepNotification(Context context, NotificationDetails notification,
            NotificationCompat.Builder builder, NotificationCompat.InboxStyle inboxStyle, boolean first) {
        SituationReport sitrep = SituationReport.fromProtocolBuffer(notification.sitrep);

        Star star = new Star();
        star.fromProtocolBuffer(notification.star);

        GlobalOptions.NotificationKind kind = getNotificationKind(sitrep);
        GlobalOptions.NotificationOptions options = new GlobalOptions().getNotificationOptions(kind);
        if (!options.isEnabled()) {
            return null;
        }

        if (first) {
            try {
                @SuppressLint("InlinedApi")
                int iconWidth = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                @SuppressLint("InlinedApi")
                int iconHeight = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

                Bitmap largeIcon = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(largeIcon);

                Sprite designSprite = sitrep.getDesignSprite();
                if (designSprite != null) {
                    Matrix matrix = new Matrix();
                    matrix.setScale((float) iconWidth / (float) designSprite.getWidth(),
                                    (float) iconHeight / (float) designSprite.getHeight());

                    canvas.save();
                    canvas.setMatrix(matrix);
                    designSprite.draw(canvas);
                    canvas.restore();
                }

                Sprite starSprite = StarImageManager.getInstance().getSprite(star, iconWidth / 2, true);
                starSprite.draw(canvas);

                builder.setLargeIcon(largeIcon);
            } catch (Resources.NotFoundException e) {
                // probably an old version of Android that doesn't support
                // large icons
            }

            builder.setContentTitle(sitrep.getTitle());
            builder.setContentText(sitrep.getDescription(star));
            builder.setWhen(sitrep.getReportTime().getMillis());

            String ringtone = new GlobalOptions().getNotificationOptions(kind).getRingtone();
            if (ringtone != null && ringtone.length() > 0) {
                builder.setSound(Uri.parse(ringtone));
            }
        }

        // subsequent notifications go in the expanded view
        inboxStyle.addLine(Html.fromHtml(sitrep.getSummaryLine(star)));

        return options;
    }

    private static GlobalOptions.NotificationOptions addChatNotification(Context context, NotificationDetails notification,
            NotificationCompat.Builder builder, NotificationCompat.InboxStyle inboxStyle, boolean first) {
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
            Empire empire = EmpireManager.i.getEmpire(Integer.parseInt(msg.getEmpireKey()), true);
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

        // subsequent notifications go in the expanded view
        inboxStyle.addLine(Html.fromHtml("<b>"+empireName+"</b>: "+msgContent));

        return options;
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
            super(App.i, "notifications.db", null, 5);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notifications ("
                      +"  id INTEGER PRIMARY KEY,"
                      +"  realm_id INTEGER,"
                      +"  star BLOB,"
                      +"  sitrep BLOB,"
                      +"  chat_msg BLOB,"
                      +"  timestamp INTEGER,"
                      +"  sitrep_key STRING,"
                      +"  chat_msg_id INTEGER)");
            db.execSQL("CREATE INDEX IX_realm_id_timestamp ON notifications (realm_id, timestamp)");
            db.execSQL("CREATE INDEX IX_sitrep_key ON notifications(sitrep_key)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE notifications "
                          +"ADD COLUMN realm_id INTEGER DEFAULT "+RealmManager.BETA_REALM_ID);
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
                if (details.sitrep != null && details.sitrep.getKey() != null && details.sitrep.getKey().length() > 0) {
                    rows = db.delete("notifications",
                            "sitrep_key = '"+details.sitrep.getKey()+"' AND realm_id = "+details.realm.getID(),
                            null);
                } else if (details.chatMsg != null && details.chatMsg.getId() > 0) {
                    rows = db.delete("notifications",
                            "chat_msg_id = '"+details.chatMsg.getId()+"' AND realm_id = "+details.realm.getID(),
                            null);
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
                values.put("realm_id", details.realm.getID());
                db.insert("notifications", null, values);

                return (rows > 0);
            } finally {
                db.close();
            }
        }

        public List<NotificationDetails> getNotifications() {
            ArrayList<NotificationDetails> notifications = new ArrayList<NotificationDetails>();

            Realm realm = RealmContext.i.getCurrentRealm();
            SQLiteDatabase db = getReadableDatabase();
            try {
                Cursor cursor = db.query("notifications", new String[] {"star", "sitrep", "chat_msg"},
                                         "realm_id = "+realm.getID(), null, null, null, "timestamp DESC");
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
                db.execSQL("DELETE FROM notifications WHERE realm_id="+realm.getID());
            } finally {
                db.close();
            }
        }
    }

    private static class NotificationDetails {
        public Messages.SituationReport sitrep;
        public Messages.Star star;
        public Messages.ChatMessage chatMsg;
        public Realm realm;
    }

    /**
     * This class manages the long poll we do to the server to receive notifications.
     */
    private static class NotificationLongPoller implements Runnable {
        private static final Log log = new Log("NotificationLongPoller");

        private Thread mPollThread;
        private Handler mHandler;

        public void start() {
            log.debug("Notification long-poll starting.");
            mHandler = new Handler();
            mPollThread = new Thread(this);
            mPollThread.setDaemon(true);
            mPollThread.start();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Messages.Notifications notifications_pb = ApiClient.getProtoBuf(
                            "notifications", Messages.Notifications.class);
                    if (notifications_pb == null) {
                        log.info("Long-poll timed out, re-requesting.");;
                        continue;
                    }

                    log.info("Long-poll complete, got %d notifications.",
                            notifications_pb.getNotificationsCount());
                    for (Messages.Notification pb : notifications_pb.getNotificationsList()) {
                        final String name = pb.getName();
                        final String value = pb.getValue();
                        log.info("[%s] = %s", name, value);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Notifications.handleNotfication(App.i, name, value);
                            }
                        });
                    }
                } catch(Exception e) {
                    log.error("Exception caught in long-polling, waiting a bit then re-trying.", e);
                    try {
                        Thread.sleep(1000 + ((int) (Math.random() * 9000))); // wait from 1 to 10 seconds...
                    } catch (InterruptedException e1) { }
                }
            }
        }
    }
}

