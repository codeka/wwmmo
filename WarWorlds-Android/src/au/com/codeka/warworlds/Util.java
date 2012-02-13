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
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * Utility methods for getting the base URL for client-server communication and
 * retrieving shared preferences.
 */
public class Util {
    private static Logger log = LoggerFactory.getLogger(Util.class);

    // Shared constants

    /**
     * Key for account name in shared preferences.
     */
    public static final String ACCOUNT_NAME = "accountName";

    /**
     * Key for auth cookie name in shared preferences.
     */
    public static final String AUTH_COOKIE = "authCookie";

    /**
     * Key for device registration id in shared preferences.
     */
    public static final String DEVICE_REGISTRATION_ID = "deviceRegistrationID";

    /**
     * An intent name for receiving registration/unregistration status.
     */
    public static final String UPDATE_UI_INTENT = getPackageName() + ".UPDATE_UI";

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
    public static void loadSettings(Context context) {
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

        SharedPreferences prefs = getSharedPreferences(context);

        try {
            URI uri = new URI(getBaseUrl());
            ApiClient.configure(uri);
        } catch(URISyntaxException e) {
            // !!!
        }

        // if we've saved off the authentication cookie, cool!
        String authCookie = prefs.getString(Util.AUTH_COOKIE, null);
        if (authCookie != null) {
            ApiClient.getCookies().add(authCookie);
        }
    }

    /**
     * Gets the contents of the warworlds.properties as a \c Properties.
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
     * (Re-)authenticate with the AppEngine service. We'll use the account name
     * we have saved in the preferences and re-generate a new AUTH_COOKIE.
     */
    public static void reauthenticate(Context context, final Activity activity,
            final Callable<Void> onComplete) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final AccountManager mgr = AccountManager.get(context);

        String accountName = prefs.getString(ACCOUNT_NAME, null);
        log.info("(re-)authenticating \""+accountName+"\"...");

        Account[] accts = mgr.getAccountsByType("com.google");
        for (Account acct : accts) {
            final Account account = acct;
            if (account.name.equals(accountName)) {
                if (isDebug()) {
                    log.info("Account found, setting up with debug auth cookie.");
                    // Use a fake cookie for the dev mode app engine server
                    // The cookie has the form email:isAdmin:userId
                    // We set the userId to be the same as the email
                    String authCookie = "dev_appserver_login=" + accountName +
                            ":false:" + accountName;
                    prefs.edit().putString(Util.AUTH_COOKIE, authCookie).commit();

                    ApiClient.getCookies().clear();
                    ApiClient.getCookies().add(authCookie);

                    log.info("AUTH_COOKIE has been saved.");
                    try {
                        onComplete.call();
                    } catch(Exception e) {
                    }
                } else {
                    log.info("Account found, fetching authentication token...");

                    // Get the auth token from the AccountManager and convert
                    // it into a cookie for the AppEngine server
                    mgr.getAuthToken(account, "ah", null, activity,
                            new AccountManagerCallback<Bundle>() {
                        public void run(AccountManagerFuture<Bundle> future) {
                            String authToken = getAuthToken(future);

                            // Ensure the token is not expired by invalidating
                            // it and obtaining a new one
                            mgr.invalidateAuthToken(account.type, authToken);
                            mgr.getAuthToken(account, "ah", null, activity,
                                    new AccountManagerCallback<Bundle>() {
                                public void run(final AccountManagerFuture<Bundle> future) {
                                    final String newAuthToken = getAuthToken(future);

                                    // can't call getAuthCookie() on the main thread
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... arg0) {
                                            try {
                                                // Convert the token into a cookie for future use
                                                String authCookie = ApiClient.authenticate(
                                                        newAuthToken);

                                                Editor editor = prefs.edit();
                                                editor.putString(Util.AUTH_COOKIE, authCookie);
                                                editor.commit();

                                                log.info("AUTH_COOKIE has been saved.");
                                                try {
                                                    onComplete.call();
                                                } catch(Exception e) {
                                                }
                                            } catch(Exception e) {
                                                // todo?
                                            }
                                            return null;
                                        }
                                    }.execute();
                                }
                            }, null);
                        }
                    }, null);
                }
                break;
            }
        }
    }

    private static String getAuthToken(AccountManagerFuture<Bundle> future) {
        try {
            Bundle authTokenBundle = future.getResult();
            String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
            return authToken;
        } catch (Exception e) {
            log.warn("Got Exception " + e);
            return null;
        }
    }

    /**
     * Removes all traces of our device's registration from the preferences.
     */
    public static void clearDeviceRegistration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, 0);

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Util.ACCOUNT_NAME);
        editor.remove(Util.AUTH_COOKIE);
        editor.remove(Util.DEVICE_REGISTRATION_ID);
        editor.commit();
    }

    /**
     * Helper method to get a SharedPreferences instance.
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, 0);

        String savedBaseUrl = prefs.getString("pref.baseUrl", "");
        String baseUrl = getBaseUrl();
        if (!savedBaseUrl.equalsIgnoreCase(baseUrl)) {
            // if the base URL has changed, it means we're now talking to a
            // different instance of the app (debug vs. release probably). We'll need
            // to clear out some preferences first.
            log.warn("BaseURL has changed (\""+baseUrl+"\" != \""+savedBaseUrl+"\"), clearing device registration");

            clearDeviceRegistration(context);

            prefs = context.getSharedPreferences(SHARED_PREFS, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("pref.baseUrl", baseUrl);
            editor.commit();
        }

        return prefs;
    }

    /**
     * Returns the package name of this class.
     */
    private static String getPackageName() {
        return Util.class.getPackage().getName();
    }
}
