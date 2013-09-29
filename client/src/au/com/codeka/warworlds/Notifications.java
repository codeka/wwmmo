
package au.com.codeka.warworlds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
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
    private static Logger log = LoggerFactory.getLogger(Notifications.class);

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

    public static void displayNotfication(Context context, String name, String value) {
        if (name.equals("sitrep")) {
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
            log.debug("Got realm: "+thisRealm.getDisplayName());
            RealmContext.i.setThreadRealm(thisRealm);
            try {
                // refresh the star this situation report is for, obviously
                // something happened that we'll want to know about
                Star star = StarManager.getInstance().refreshStarSync(pb.getStarKey(), true);
                log.debug("refreshed star: successful? "+(star == null ? "false" : "true"));
                if (star == null) { // <-- only refresh the star if we have one cached
                    // if we didn't refresh the star, then at least refresh
                    // the sector it was in (could have been a moving
                    // fleet, say)
                    star = SectorManager.getInstance().findStar(pb.getStarKey());
                    if (star != null) {
                        log.debug("star found from sector manager instead.");
                        SectorManager.getInstance().refreshSector(star.getSectorX(), star.getSectorY());
                    }
                } else {
                    log.debug("firing star updated...");
                    StarManager.getInstance().fireStarUpdated(star);
                }

                // notify the build manager, in case it's a 'build complete' or something
                log.debug("notifying build manager.");
                BuildManager.getInstance().notifySituationReport(pb);

                log.debug("displaying a notification...");
                displayNotification(context, pb);
            } finally {
                RealmContext.i.setThreadRealm(null);
            }
        } else if (name.equals("chat")) {
            byte[] blob = Base64.decode(value, Base64.DEFAULT);

            ChatMessage msg;
            try {
                Messages.ChatMessage pb = Messages.ChatMessage.parseFrom(blob);
                msg = new ChatMessage();
                msg.fromProtocolBuffer(pb);
            } catch(InvalidProtocolBufferException e) {
                log.error("Could not parse chat message!", e);
                return;
            }

            ChatConversation conversation = ChatManager.i.getConversation(msg);
            if (conversation != null) {
                conversation.addMessage(msg);
            }
        }
    }

    private static void displayNotification(final Context context,
                                            final Messages.SituationReport sitrep) {
        String starKey = sitrep.getStarKey();
        StarSummary starSummary = StarManager.getInstance().getStarSummaryNoFetch(starKey,
                Float.MAX_VALUE // always prefer a cached version, no matter how old
            );
        if (starSummary == null) {
            // TODO: this is actually an error... we need better error reporting
            log.error("Could not get star summary for star "+starKey+", cannot display notification.");
            return;
        }
        log.debug("got a star summary!");

        NotificationDetails notification = new NotificationDetails();
        notification.sitrep = sitrep;
        notification.realm = RealmContext.i.getCurrentRealm();

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        starSummary.toProtocolBuffer(star_pb);
        notification.star = star_pb.build();

        DatabaseHelper db = new DatabaseHelper();
        if (!db.addNotification(notification)) {
            log.debug("displaying notification now...");
            displayNotification(buildNotification(context, db.getNotifications()));
        } else {
            log.debug("notification already showing.");
        }
    }

    private static void displayNotification(Notification notification) {
        if (notification == null) {
            return;
        }

        if (!new GlobalOptions().notificationsEnabled()) {
            return;
        }

        NotificationManager nm = (NotificationManager) App.i.getSystemService(Context.NOTIFICATION_SERVICE);
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
            SituationReport sitrep = SituationReport.fromProtocolBuffer(notification.sitrep);

            Star star = new Star();
            star.fromProtocolBuffer(notification.star);

            GlobalOptions.NotificationKind kind = getNotificationKind(sitrep);
            GlobalOptions.NotificationOptions thisOptions = new GlobalOptions().getNotificationOptions(kind);
            if (!thisOptions.isEnabled()) {
                continue;
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

                options = thisOptions;
            }
            first = false;

            // subsequent notifications go in the expanded view
            inboxStyle.addLine(Html.fromHtml(sitrep.getSummaryLine(star)));

            num++;
        }

        if (num == 0) {
            return null;
        }

        Realm realm = RealmContext.i.getCurrentRealm();
        inboxStyle.setBigContentTitle(String.format("%s: %d events", realm.getDisplayName(), num));
        builder.setStyle(inboxStyle);
        builder.setNumber(num);
        builder.setLights(options.getLedColour(), 1000, 5000);

        return builder.build();
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
            super(App.i, "notifications.db", null, 3);
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
                      +"  timestamp INTEGER,"
                      +"  sitrep_key STRING);");
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
                if (details.sitrep.getKey() != null && details.sitrep.getKey().length() > 0) {
                    rows = db.delete("notifications",
                            "sitrep_key = '"+details.sitrep.getKey()+"' AND realm_id = "+details.realm.getID(),
                            null);
                }

                ByteArrayOutputStream sitrep = new ByteArrayOutputStream();
                ByteArrayOutputStream star = new ByteArrayOutputStream();
                try {
                    details.sitrep.writeTo(sitrep);
                    details.star.writeTo(star);
                } catch (IOException e) {
                    // we won't get the notification, but not the end of the world...
                    log.error("Error serializing notification details.", e);
                    return false;
                }

                ContentValues values = new ContentValues();
                values.put("star", star.toByteArray());
                values.put("sitrep", sitrep.toByteArray());
                values.put("sitrep_key", details.sitrep.getKey());
                values.put("timestamp", details.sitrep.getReportTime());
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
                Cursor cursor = db.query("notifications", new String[] {"star", "sitrep"},
                                         "realm_id = "+realm.getID(), null, null, null, "timestamp DESC");
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return notifications;
                }

                do {
                    try {
                        NotificationDetails notification = new NotificationDetails();
                        notification.star = Messages.Star.parseFrom(cursor.getBlob(0));
                        notification.sitrep = Messages.SituationReport.parseFrom(cursor.getBlob(1));
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
                // ignore....
            } finally {
                db.close();
            }

            return notifications;
        }

        public void clearNotifications() {
            Realm realm = RealmContext.i.getCurrentRealm();

            SQLiteDatabase db = getReadableDatabase();
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
        public Realm realm;
    }

    /**
     * This class manages the long poll we do to the server to receive notifications.
     */
    private static class NotificationLongPoller implements Runnable {
        private static Logger log = LoggerFactory.getLogger(NotificationLongPoller.class);

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
                    Messages.Notifications notifications_pb = ApiClient.getProtoBuf("notifications", Messages.Notifications.class);
                    if (notifications_pb == null) {
                        log.info("Long-poll timed out, re-requesting.");;
                        continue;
                    }

                    log.info("Long-poll complete, got "+notifications_pb.getNotificationsCount()+" notifications.");
                    for (Messages.Notification pb : notifications_pb.getNotificationsList()) {
                        final String name = pb.getName();
                        final String value = pb.getValue();
                        log.info("["+name+"]="+value);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Notifications.displayNotfication(App.i, name, value);
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

