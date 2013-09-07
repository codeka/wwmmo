
package au.com.codeka.warworlds;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.Base64;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.ChatMessage;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.SituationReport;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.SituationReportHelper;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;


public class Notifications {
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
            SituationReport sitrep;
            try {
                sitrep = Model.wire.parseFrom(blob, SituationReport.class);
            } catch(IOException e) {
                sitrep = null;
            }

            // we could currently be in a game, and that game could be running in a different
            // realm to this notification. So we switch this thread temporarily to whatever
            // realm this notification is for.
            Realm thisRealm = RealmManager.i.getRealmByName(sitrep.realm);
            RealmContext.i.setThreadRealm(thisRealm);
            try {
                // refresh the star this situation report is for, obviously
                // something happened that we'll want to know about
                Star star = StarManager.i.refreshStarSync(sitrep.star_key, true);
                if (star == null) { // <-- only refresh the star if we have one cached
                    // if we didn't refresh the star, then at least refresh
                    // the sector it was in (could have been a moving
                    // fleet, say)
                    star = SectorManager.i.findStar(sitrep.star_key);
                    if (star != null) {
                        SectorManager.i.refreshSector(star.sector_x, star.sector_y);
                    }
                } else {
                    StarManager.i.fireStarUpdated(star);
                }

                // notify the build manager, in case it's a 'build complete' or something
                BuildManager.getInstance().notifySituationReport(sitrep);

                displayNotification(context, sitrep);
            } finally {
                RealmContext.i.setThreadRealm(null);
            }
        } else if (name.equals("chat")) {
            byte[] blob = Base64.decode(value, Base64.DEFAULT);
            ChatMessage msg;
            try {
                msg = Model.wire.parseFrom(blob, ChatMessage.class);
            } catch (IOException e) {
                msg = null;
            }

            // don't add our own chats, since they'll have been added automatically
            Empire myEmpire = EmpireManager.i.getEmpire();
            if (myEmpire == null) {
                return;
            }
            if (msg.empire_key == null || msg.empire_key.equals(myEmpire.key)) {
                return;
            }

            ChatManager.getInstance().addMessage(msg);
        }
    }

    private static void displayNotification(final Context context,
                                            final SituationReport sitrep) {
        String starKey = sitrep.star_key;
        Star starSummary = StarManager.i.requestStarSummarySync(starKey,
                Float.MAX_VALUE // always prefer a cached version, no matter how old
            );
        if (starSummary == null) {
            // TODO: this is actually an error... we need better error reporting
            return;
        }

        NotificationDetails notification = new NotificationDetails();
        notification.sitrep = sitrep;
        notification.realm = RealmContext.i.getCurrentRealm();
        try {
            notification.star = Model.wire.parseFrom(starSummary.toByteArray(), Star.class);
        } catch (IOException e) {
        }

        DatabaseHelper db = new DatabaseHelper();
        if (!db.addNotification(notification)) {
            displayNotification(context, buildNotification(context, db.getNotifications()));
        }
    }

    private static void displayNotification(Context context, Notification notification) {
        if (notification == null) {
            return;
        }

        if (!new GlobalOptions().notificationsEnabled()) {
            return;
        }

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
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
            GlobalOptions.NotificationKind kind = getNotificationKind(notification.sitrep);
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

                    Sprite designSprite = SituationReportHelper.getDesignSprite(notification.sitrep);
                    if (designSprite != null) {
                        Matrix matrix = new Matrix();
                        matrix.setScale((float) iconWidth / (float) designSprite.getWidth(),
                                        (float) iconHeight / (float) designSprite.getHeight());

                        canvas.save();
                        canvas.setMatrix(matrix);
                        designSprite.draw(canvas);
                        canvas.restore();
                    }

                    Sprite starSprite = StarImageManager.getInstance().getSprite(notification.star, iconWidth / 2, true);
                    starSprite.draw(canvas);

                    builder.setLargeIcon(largeIcon);
                } catch (Resources.NotFoundException e) {
                    // probably an old version of Android that doesn't support
                    // large icons
                }

                builder.setContentTitle(SituationReportHelper.getTitle(notification.sitrep));
                builder.setContentText(SituationReportHelper.getDescription(notification.sitrep, notification.star));
                builder.setWhen(notification.sitrep.report_time * 1000);

                String ringtone = new GlobalOptions().getNotificationOptions(kind).getRingtone();
                if (ringtone != null && ringtone.length() > 0) {
                    builder.setSound(Uri.parse(ringtone));
                }

                options = thisOptions;
            }
            first = false;

            // subsequent notifications go in the expanded view
            inboxStyle.addLine(Html.fromHtml(SituationReportHelper.getSummaryLine(notification.star, notification.sitrep)));

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
        if (sitrep.build_complete_record != null) {
            if (sitrep.build_complete_record.build_kind == BuildRequest.BUILD_KIND.BUILDING) {
                return GlobalOptions.NotificationKind.BUILDING_BUILD_COMPLETE;
            } else {
                return GlobalOptions.NotificationKind.FLEET_BUILD_COMPLETE;
            }
        }

        if (sitrep.fleet_under_attack_record != null) {
            return GlobalOptions.NotificationKind.FLEET_UNDER_ATTACK;
        }

        if (sitrep.move_complete_record != null) {
            return GlobalOptions.NotificationKind.FLEET_MOVE_COMPLETE;
        }

        if (sitrep.fleet_destroyed_record != null) {
            return GlobalOptions.NotificationKind.FLEET_DESTROYED;
        }

        if (sitrep.fleet_victorious_record != null) {
            return GlobalOptions.NotificationKind.FLEET_VICTORIOUS;
        }

        if (sitrep.colony_destroyed_record != null) {
            return GlobalOptions.NotificationKind.COLONY_DESTROYED;
        }

        if (sitrep.colony_attacked_record != null) {
            return GlobalOptions.NotificationKind.COLONY_ATTACKED;
        }

        if (sitrep.star_ran_out_of_goods_record != null) {
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
                int rows = db.delete("notifications",
                        "sitrep_key = '"+details.sitrep.key+"' AND realm_id = "+details.realm.getID(),
                        null);

                ContentValues values = new ContentValues();
                values.put("star", details.star.toByteArray());
                values.put("sitrep", details.sitrep.toByteArray());
                values.put("sitrep_key", details.sitrep.key);
                values.put("timestamp", (long) details.sitrep.report_time / 1000);
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
                        notification.star = Model.wire.parseFrom(cursor.getBlob(0), Star.class);
                        notification.sitrep = Model.wire.parseFrom(cursor.getBlob(1), SituationReport.class);
                        notification.realm = realm;
                        notifications.add(notification);
                    } catch (IOException e) {
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
        public SituationReport sitrep;
        public Star star;
        public Realm realm;
    }
}

