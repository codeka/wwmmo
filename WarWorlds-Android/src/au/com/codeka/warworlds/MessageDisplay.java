/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package au.com.codeka.warworlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.protobuf.Messages;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Display a message as a notification, with an accompanying sound.
 */
public class MessageDisplay {
    private static Logger log = LoggerFactory.getLogger(MessageDisplay.class);

    private MessageDisplay() {
    }

    /*
     * App-specific methods for the sample application - 1) parse the incoming
     * message; 2) generate a notification; 3) play a sound
     */

    public static void displayMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for(String key : extras.keySet()) {
                log.debug(String.format("%s = %s", key, extras.get(key)));
            }
            if (extras.containsKey("msg")) {
                displayNotification(context, buildNotification(context, extras.get("msg").toString()));
                playNotificationSound(context);
            } else if (extras.containsKey("sitrep")) {
                byte[] blob = Base64.decode(extras.getString("sitrep"), Base64.DEFAULT);

                SituationReport sitrep;
                try {
                    Messages.SituationReport pb = Messages.SituationReport.parseFrom(blob);
                    sitrep = SituationReport.fromProtocolBuffer(pb);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Could not parse situation report!", e);
                    return;
                }

                // refresh the star this situation report is for, obviously
                // something happened that we'll want to know about
                if (!StarManager.getInstance().refreshStar(
                            context,
                            sitrep.getStarKey(),
                            true)) { // <-- only refresh the star if we have one cached
                    // if we didn't refresh the star, then at least refresh
                    // the sector it was in (could have been a moving
                    // fleet, say)
                    Star star = SectorManager.getInstance().findStar(sitrep.getStarKey());
                    if (star != null) {
                        SectorManager.getInstance().refreshSector(star.getSectorX(), star.getSectorY());
                    }
                }

                displayNotification(context, sitrep);
                playNotificationSound(context);
            } else if (extras.containsKey("chat")) {
                byte[] blob = Base64.decode(extras.getString("chat"), Base64.DEFAULT);

                ChatMessage msg;
                try {
                    Messages.ChatMessage pb = Messages.ChatMessage.parseFrom(blob);
                    msg = ChatMessage.fromProtocolBuffer(pb);
                } catch(InvalidProtocolBufferException e) {
                    log.error("Could not parse chat message!", e);
                    return;
                }

                ChatManager.getInstance().addMessage(msg);
            }
        }
    }

    private static void displayNotification(final Context context,
                                            final SituationReport sitrep) {
        String starKey = sitrep.getStarKey();
        StarManager.getInstance().requestStarSummary(context, starKey,
            new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary starSummary) {
                    displayNotification(context, buildNotification(context, starSummary, sitrep));
                }
            });
    }

    private static void displayNotification(Context context, Notification notification) {
        if (notification == null) {
            return;
        }

        SharedPreferences settings = Util.getSharedPreferences(context);
        int notificatonID = settings.getInt("notificationID", 0);

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificatonID, notification);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("notificationID", ++notificatonID % 32);
        editor.commit();
    }

    private static Notification buildNotification(Context context,
                                                  String message) {
        int icon = R.drawable.status_icon;
        long when = System.currentTimeMillis();

        Intent intent = new Intent(context, WarWorldsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("War Worlds");
        builder.setContentText(message);
        builder.setWhen(when);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(icon);
        builder.setAutoCancel(true);
        builder.setLights(Color.RED, 1000, 3000);

        return builder.getNotification();
    }

    private static Notification buildNotification(Context context,
                                                  StarSummary star,
                                                  SituationReport sitrep) {

        GlobalOptions.NotificationKind kind = getNotificationKind(sitrep);
        GlobalOptions.NotificationOptions options = new GlobalOptions(context).getNotificationOptions(kind);
        if (!options.isEnabled()) {
            return null;
        }

        // TODO: make this show the situation report
        Intent intent = new Intent(context, WarWorldsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        try {
            int iconWidth = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
            int iconHeight = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

            Bitmap largeIcon;
            Sprite designSprite = sitrep.getDesignSprite();
            if (designSprite != null) {
                largeIcon = designSprite.createIcon(iconWidth, iconHeight);
            } else {
                largeIcon = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(largeIcon);
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
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.status_icon);
        builder.setAutoCancel(true);
        builder.setLights(options.getLedColour(), 1000, 3000);

        return builder.getNotification();
    }

    private static GlobalOptions.NotificationKind getNotificationKind(SituationReport sitrep) {
        if (sitrep.getBuildCompleteRecord() != null) {
            if (sitrep.getBuildCompleteRecord().getBuildKind() == BuildRequest.BuildKind.BUILDING) {
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

    private static void playNotificationSound(Context context) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri != null) {
            Ringtone rt = RingtoneManager.getRingtone(context, uri);
            if (rt != null) {
                rt.setStreamType(AudioManager.STREAM_NOTIFICATION);
                rt.play();
            }
        }
    }
}
