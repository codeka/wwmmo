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
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.restlet.Client;
import org.restlet.data.Cookie;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

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
     * Cookie name for authorisation.
     */
    private static final String AUTH_COOKIE_NAME = "SACSID";

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
     * (Re-)authenticate with the AppEngine service. We'll use the account name
     * we have saved in the preferences and re-generate a new AUTH_COOKIE.
     */
    public static void reauthenticate(Context context, final Activity activity, final Callable<Void> onComplete) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final AccountManager mgr = AccountManager.get(context);

        String accountName = prefs.getString(ACCOUNT_NAME, null);
        Log.i(TAG, "(re-)authenticating \""+accountName+"\"...");

        Account[] accts = mgr.getAccountsByType("com.google");
        for (Account acct : accts) {
            final Account account = acct;
            if (account.name.equals(accountName)) {
                if (isDebug(context)) {
                    Log.i(TAG, "Account found, setting up with debug auth cookie.");
                    // Use a fake cookie for the dev mode app engine server
                    // The cookie has the form email:isAdmin:userId
                    // We set the userId to be the same as the email
                    String authCookie = "dev_appserver_login=" + accountName + ":false:" + accountName;
                    prefs.edit().putString(Util.AUTH_COOKIE, authCookie).commit();

                    Log.i(TAG, "AUTH_COOKIE has been saved.");
                    try {
                        onComplete.call();
                    } catch(Exception e) {
                    }
                } else {
                    Log.i(TAG, "Account found, fetching authentication token...");

                    // Get the auth token from the AccountManager and convert
                    // it into a cookie for the AppEngine server
                    mgr.getAuthToken(account, "ah", null, activity, new AccountManagerCallback<Bundle>() {
                        public void run(AccountManagerFuture<Bundle> future) {
                            String authToken = getAuthToken(future);

                            // Ensure the token is not expired by invalidating it and obtaining a new one
                            mgr.invalidateAuthToken(account.type, authToken);
                            mgr.getAuthToken(account, "ah", null, activity, new AccountManagerCallback<Bundle>() {
                                public void run(final AccountManagerFuture<Bundle> future) {
                                    final String newAuthToken = getAuthToken(future);

                                    // can't call getAuthCookie() on the main thread
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... arg0) {
                                            try {
                                                // Convert the token into a cookie for future use
                                                String authCookie = getAuthCookie(newAuthToken);
                                                Editor editor = prefs.edit();
                                                editor.putString(Util.AUTH_COOKIE, authCookie);
                                                editor.commit();

                                                Log.i(TAG, "AUTH_COOKIE has been saved.");
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
            Log.w(TAG, "Got Exception " + e);
            return null;
        }
    }

    /**
     * Retrieves the authorisation cookie associated with the given token. This
     * method should only be used when running against a production AppEngine
     * backend (as opposed to a dev mode server).
     */
    private static String getAuthCookie(String authToken) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            // Get SACSID cookie
            httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
            String uri = Setup.PROD_URL + "/_ah/login?continue=http://localhost/&auth=" + authToken;
            HttpGet method = new HttpGet(uri);

            HttpResponse res = httpClient.execute(method);
            StatusLine statusLine = res.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            Header[] headers = res.getHeaders("Set-Cookie");
            if (statusCode != 302 || headers.length == 0) {
                return null;
            }

            for (org.apache.http.cookie.Cookie cookie : httpClient.getCookieStore().getCookies()) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return AUTH_COOKIE_NAME + "=" + cookie.getValue();
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Got IOException " + e);
            Log.w(TAG, Log.getStackTraceString(e));
        } finally {
            httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        }

        return null;
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
