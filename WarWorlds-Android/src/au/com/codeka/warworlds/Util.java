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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * Utility methods for getting the base URL for client-server communication and
 * retrieving shared preferences.
 */
public class Util {
    private static Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * Key for shared preferences.
     */
    private static final String SHARED_PREFS = "WARWORLDS_PREFS";

    private static Properties sProperties;

    /**
     * Must be called before other methods on this class. We load up the initial
     * properties, preferences and settings to make later calls easier (and not
     * require a \c Context parameter)
     */
    public static void loadProperties(Context context, Activity activity) {
        // load the warworlds.properties file and populate mProperties.
        AssetManager assetManager = context.getAssets();

        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("warworlds.properties");
            sProperties = new Properties();
            sProperties.load(inputStream);
        } catch (IOException e) {
            sProperties = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch(IOException e) {
            }
        }

        try {
            URI uri = new URI(getBaseUrl());
            if (ApiClient.getBaseUri() == null || !ApiClient.getBaseUri().equals(uri)) {
                ApiClient.configure(uri);
            }
        } catch(URISyntaxException e) {
            // !!!
        }
    }

    /**
     * Gets the contents of the warworlds.properties as a \c Properties. These are the static
     * properties that govern things like which server to connect to and so on.
     */
    public static Properties getProperties() {
        return sProperties;
    }

    /**
     * Returns the (debug or production) URL associated with the registration
     * service.
     */
    public static String getBaseUrl() {
        final String serverDefault = sProperties.getProperty("server.default");
        final String url = sProperties.getProperty("server."+serverDefault);
        return url;
    }

    /**
     * Returns true if we are running against a dev mode appengine instance.
     */
    public static boolean isDebug() {
        final String serverDefault = sProperties.getProperty("server.default");
        return (serverDefault.equals("debug"));
    }

    /**
     * Display a notification containing the given string.
     */
    public static void generateNotification(Context context, String message) {
        int icon = R.drawable.status_icon;
        long when = System.currentTimeMillis();

        // TODO: something better? this'll just launch us to the home page...
        Intent intent = new Intent(context, WarWorldsActivity.class);

        Notification notification = new Notification(icon, message, when);
        notification.setLatestEventInfo(context, "War Worlds", message,
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        SharedPreferences settings = Util.getSharedPreferences(context);
        int notificatonID = settings.getInt("notificationID", 0);

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificatonID, notification);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("notificationID", ++notificatonID % 32);
        editor.commit();
    }

    /**
     * Helper method to get a SharedPreferences instance.
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, 0);

        String savedBaseUrl = prefs.getString("Util.baseUrl", "");
        String baseUrl = getBaseUrl();
        if (!savedBaseUrl.equalsIgnoreCase(baseUrl)) {
            // if the base URL has changed, it means we're now talking to a
            // different instance of the app (debug vs. release probably). We'll need
            // to clear out some preferences first.
            log.warn("BaseURL has changed (\""+baseUrl+"\" != \""+savedBaseUrl+"\"), clearing device registration");

            prefs = context.getSharedPreferences(SHARED_PREFS, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("DeviceRegistrar.registrationKey");
            editor.remove("AccountName");
            editor.putString("Util.baseUrl", baseUrl);
            editor.commit();
        }

        return prefs;
    }
}
