
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
import au.com.codeka.warworlds.model.BuildRequest;
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

    public static void clearNotifications(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();

        new DatabaseHelper(context).clearNotifications();
    }

    public static void displayNotification(final Context context,
                                           final Messages.SituationReport sitrep) {
        String starKey = sitrep.getStarKey();
        StarManager.getInstance().requestStarSummary(context, starKey,
            new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary starSummary) {
                    NotificationDetails notification = new NotificationDetails();
                    notification.sitrep = sitrep;

                    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
                    starSummary.toProtocolBuffer(star_pb);
                    notification.star = star_pb.build();

                    DatabaseHelper db = new DatabaseHelper(context);
                    db.addNotification(notification);

                    displayNotification(context, buildNotification(context, db.getNotifications()));
                }
            });
    }

    private static void displayNotification(Context context, Notification notification) {
        if (notification == null) {
            return;
        }

        if (!new GlobalOptions(context).notificationsEnabled()) {
            return;
        }

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, notification);
    }

    private static Notification buildNotification(Context context,
                                                  List<NotificationDetails> notifications) {

        Intent intent = new Intent(context, SitrepActivity.class);

        // we want to add the StarfieldActivity to the "back stack" of the situation report so that
        // when you press "back" from the sitrep you go to the starfield.
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
            GlobalOptions.NotificationOptions thisOptions = new GlobalOptions(context).getNotificationOptions(kind);
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

                    Sprite starSprite = StarImageManager.getInstance().getSprite(context, star, iconWidth / 2);
                    starSprite.draw(canvas);

                    builder.setLargeIcon(largeIcon);
                } catch (Resources.NotFoundException e) {
                    // probably an old version of Android that doesn't support
                    // large icons
                }

                builder.setContentTitle(sitrep.getTitle());
                builder.setContentText(sitrep.getDescription(star));
                builder.setWhen(sitrep.getReportTime().getMillis());

                String ringtone = new GlobalOptions(context).getNotificationOptions(kind).getRingtone();
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

        inboxStyle.setBigContentTitle(String.format("%d events have happend", num));
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

        return GlobalOptions.NotificationKind.OTHER;
    }

    /**
     * We store all the unacknowledged notifications in a small sqlite database, this
     * is the helper for that database.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "notifications.db", null, 1);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notifications ("
                      +"  id INTEGER PRIMARY KEY,"
                      +"  star BLOB,"
                      +"  sitrep BLOB,"
                      +"  timestamp INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
                db.insert("notifications", null, values);
            } finally {
                db.close();
            }
        }

        public List<NotificationDetails> getNotifications() {
            ArrayList<NotificationDetails> notifications = new ArrayList<NotificationDetails>();

            SQLiteDatabase db = getReadableDatabase();
            try {
                Cursor cursor = db.query("notifications", new String[] {"star", "sitrep"},
                                         null, null, null, null, "timestamp DESC");
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return notifications;
                }

                do {
                    try {
                        NotificationDetails notification = new NotificationDetails();
                        notification.star = Messages.Star.parseFrom(cursor.getBlob(0));
                        notification.sitrep = Messages.SituationReport.parseFrom(cursor.getBlob(1));
                        notifications.add(notification);
                    } catch (InvalidProtocolBufferException e) {
                        // any errors here and we'll just skip this notification
                    }
                } while (cursor.moveToNext());
                cursor.close();
            } finally {
                db.close();
            }

            return notifications;
        }

        public void clearNotifications() {
            SQLiteDatabase db = getReadableDatabase();
            try {
                db.execSQL("DELETE FROM notifications;");
            } finally {
                db.close();
            }
        }
    }

    private static class NotificationDetails {
        public Messages.SituationReport sitrep;
        public Messages.Star star;
    }
}
