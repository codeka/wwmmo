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

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.protobuf.Messages;

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
                displayNotification(context, extras.get("msg").toString());
                playNotificationSound(context);
            } else if (extras.containsKey("sitrep")) {
                byte[] blob = Base64.decode(extras.getString("sitrep"), Base64.DEFAULT);

                Messages.SituationReport sitrep;
                try {
                    sitrep = Messages.SituationReport.parseFrom(blob);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Could not parse situation report!", e);
                    return;
                }

                displayNotification(context, sitrep);
                playNotificationSound(context);

            }
        }
    }

    private static void displayNotification(final Context context,
                                            final Messages.SituationReport sitrep) {
        String starKey = sitrep.getStarKey();
        StarManager.getInstance().requestStarSummary(context, starKey,
            new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary starSummary) {
                    displayNotification(context, starSummary, sitrep);
                }
            });
    }

    private static void displayNotification(Context context, StarSummary starSummary,
                                            Messages.SituationReport sitrep) {

        String msg = "";

        Messages.SituationReport.MoveCompleteRecord mcr = sitrep.getMoveCompleteRecord();
        if (mcr != null) {
            msg += getFleetLine(mcr.getFleetDesignId(), mcr.getNumShips());
            msg += String.format(Locale.ENGLISH, " arrived at %s", starSummary.getName());
        }

        Messages.SituationReport.FleetUnderAttackRecord fuar = sitrep.getFleetUnderAttackRecord();
        if (fuar != null) {
            if (mcr != null) {
                msg += ", and is under attack";
            } else {
                msg += getFleetLine(fuar.getFleetDesignId(), fuar.getNumShips());
                msg += String.format(Locale.ENGLISH, " is under attack at %s", starSummary.getName());
            }
        }

        if (msg.length() == 0) {
            msg = "We got a situation over here!";
        }

        displayNotification(context, msg);
    }

    private static String getFleetLine(String designID, float numShips) {
        ShipDesign design = ShipDesignManager.getInstance().getDesign(designID);
        String msg = design.getDisplayName();

        int n = (int)(Math.ceil(numShips));
        if (n > 1) {
            msg += String.format(Locale.ENGLISH, " (× %d)", n);
        }

        return msg;
    }

    private static void displayNotification(Context context, String message) {
        Notification notification = buildNotification(context, message);

        SharedPreferences settings = Util.getSharedPreferences(context);
        int notificatonID = settings.getInt("notificationID", 0);

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificatonID, notification);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("notificationID", ++notificatonID % 32);
        editor.commit();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private static Notification buildNotification(Context context,
                                                  String message) {
        int icon = R.drawable.status_icon;
        long when = System.currentTimeMillis();

        // TODO: something better? this'll just launch us to the home page...
        Intent intent = new Intent(context, WarWorldsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            Notification notification = new Notification(icon, message, when);
            notification.setLatestEventInfo(context, "War Worlds", message,
                    pendingIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            return notification;
        } else {
            return new Notification.Builder(context)
                    .setSmallIcon(icon)
                    .setContentTitle("War Worlds")
                    .setContentText(message)
                    .setWhen(when)
                    .setContentIntent(pendingIntent)
                    .build();
        }
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
