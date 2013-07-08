
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

import com.google.protobuf.InvalidProtocolBufferException;

public class Notifications {
    private Notifications() {
    }

    public static void clearNotifications() {
        NotificationManager nm = (NotificationManager) App.i.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(RealmContext.i.getCurrentRealm().getID());

        new DatabaseHelper().clearNotifications();
    }

    public static void displayNotification(final Context context,
                                           final Messages.SituationReport sitrep) {
        String starKey = sitrep.getStarKey();
        StarSummary starSummary = StarManager.getInstance().requestStarSummarySync(starKey,
                Float.MAX_VALUE // always prefer a cached version, no matter how old
            );
        if (starSummary == null) {
            // TODO: this is actually an error... we need better error reporting
            return;
        }

        NotificationDetails notification = new NotificationDetails();
        notification.sitrep = sitrep;
        notification.realm = RealmContext.i.getCurrentRealm();

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        starSummary.toProtocolBuffer(star_pb);
        notification.star = star_pb.build();

        DatabaseHelper db = new DatabaseHelper();
        db.addNotification(notification);

        displayNotification(context, buildNotification(context, db.getNotifications()));
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
            super(App.i, "notifications.db", null, 2);
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
                      +"  timestamp INTEGER);");
            db.execSQL("CREATE INDEX IX_realm_id_timestamp ON notifications (realm_id, timestamp)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 2) {
                db.execSQL("ALTER TABLE notifications "
                          +"ADD COLUMN realm_id INTEGER DEFAULT "+RealmManager.BETA_REALM_ID);
                db.execSQL("CREATE INDEX IX_realm_id_timestamp ON notifications (realm_id, timestamp)");
            }
        }

        public void addNotification(NotificationDetails details) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                ByteArrayOutputStream sitrep = new ByteArrayOutputStream();
                ByteArrayOutputStream star = new ByteArrayOutputStream();
                try {
                    details.sitrep.writeTo(sitrep);
                    details.star.writeTo(star);
                } catch (IOException e) {
                    // we won't get the notification, but not the end of the world...
                    return;
                }

                ContentValues values = new ContentValues();
                values.put("star", star.toByteArray());
                values.put("sitrep", sitrep.toByteArray());
                values.put("timestamp", details.sitrep.getReportTime());
                values.put("realm_id", details.realm.getID());
                db.insert("notifications", null, values);
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
}

