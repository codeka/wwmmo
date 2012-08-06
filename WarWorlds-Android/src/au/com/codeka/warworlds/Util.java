
package au.com.codeka.warworlds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
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
    public static Properties loadProperties(Context context) {
        if (sProperties != null) {
            // if it's already loaded, don't do it again
            return sProperties;
        }

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

        return sProperties;
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
