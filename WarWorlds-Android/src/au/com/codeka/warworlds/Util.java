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
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import org.restlet.Client;
import org.restlet.data.Cookie;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * Utility methods for getting the base URL for client-server communication and
 * retrieving shared preferences.
 */
public class Util {

	private static Properties props = null;
	
    /**
     * Tag for logging.
     */
    private static final String TAG = "Util";

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

    // End shared constants

    /**
     * Key for shared preferences.
     */
    private static final String SHARED_PREFS = "warworlds".toUpperCase(Locale.ENGLISH) + "_PREFS";

    /**
     * Gets a reference to the "warworlds.properties" file, that's deployed with the package.
     * @return
     */
    public static Properties getProperties(Context context) {
    	if (props == null) {
	    	AssetManager assetManager = context.getAssets();
	
	    	InputStream inputStream = null;
	    	try {
	    	    inputStream = assetManager.open("warworlds.properties");
	    	    props = new Properties();
	    	    props.load(inputStream);
	    	} catch (IOException e) {
	    		props = null;
	    	} finally {
	    		try {
	    			if (inputStream != null) {
	    				inputStream.close();
	    			}
	    		} catch(IOException e) {
	    		}
	    	}
    	}
    	
    	return props;
    }

    /**
     * Returns the (debug or production) URL associated with the registration
     * service.
     */
    public static String getBaseUrl(Context context) {
    	Properties p = getProperties(context);
    	String serverDefault = p.getProperty("server.default");
    	String url = p.getProperty("server."+serverDefault);
    	return url;
    }
    
    /**
     * Returns true if we are running against a dev mode appengine instance.
     */
    public static boolean isDebug(Context context) {
    	Properties p = getProperties(context);
    	String serverDefault = p.getProperty("server.default");
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
     * Creates a \c ClientResource pointing at the given URL and that will return data for the given
     * resource type.
     */
    public static <T> T getClientResource(Context context,
    		String url, Class<T> factoryClass) {

    	org.restlet.Context.getCurrentLogger().setLevel(Level.FINEST);
    	

    	String baseUrl = getBaseUrl(context);
    	if (baseUrl.endsWith("/")) {
    		baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    	}
    	if (!url.startsWith("/")) {
    		url = "/"+url;
    	}

    	URI uri = null;
    	try {
    		uri = new URI(baseUrl + "/api/v1" + url);
    	} catch(URISyntaxException e) {
    		Log.e(TAG, "Invalid URL: "+baseUrl+url);
    		return null;
    	}

    	Log.i(TAG, "ClientResource: "+uri);
    	ClientResource cr = new ClientResource(uri);
    	cr.setRequestEntityBuffering(true);

    	Client c = new Client(new org.restlet.Context(), Protocol.HTTP);
    	c.getContext().getParameters().add("retryHandler", "au.com.codeka.warworlds.RequestRetryHandler");
    	cr.setNext(c);

		// add the authentication cookie to the request as well
		SharedPreferences prefs = getSharedPreferences(context);
        String authCookie = prefs.getString(Util.AUTH_COOKIE, null);
        if (authCookie != null) {
	        String nvp[] = authCookie.split("=");
	        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
	        cookies.add(new Cookie(nvp[0], nvp[1]));
	        cr.setCookies(cookies);
        }

        return cr.wrap(factoryClass);
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
        String baseUrl = getBaseUrl(context);
        if (!savedBaseUrl.equalsIgnoreCase(baseUrl)) {
        	// if the base URL has changed, it means we're now talking to a
        	// different instance of the app (debug vs. release probably). We'll need
        	// to clear out some preferences first.
        	Log.w(TAG, "BaseURL has changed (\""+baseUrl+"\" != \""+savedBaseUrl+"\"), clearing device registration");

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
