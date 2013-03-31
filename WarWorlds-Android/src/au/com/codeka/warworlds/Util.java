
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
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.SpriteManager;

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
    private static boolean sWasSetup;

    /**
     * This should be called from every entry-point into the process to make
     * sure the various globals are up and running.
     */
    public static boolean setup(Context context) {
        if (sWasSetup) {
            return false;
        }

        Authenticator.configure(context);
        SpriteManager.getInstance().setup(context);
        BuildingDesignManager.getInstance().setup(context);
        ShipDesignManager.getInstance().setup(context);
        PurchaseManager.getInstance().setup(context);

        sWasSetup = true;
        return true;
    }

    public static boolean isSetup() {
        return sWasSetup;
    }

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

        String impersonateUser = sProperties.getProperty("user.on_behalf_of", null);
        if (impersonateUser != null) {
            ApiClient.impersonate(impersonateUser);
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
        final String debugValue = sProperties.getProperty("debug");
        if (debugValue.equals("auto")) {
            return isLocalDevServer();
        } else {
            return debugValue.equals("true");
        }
    }

    public static boolean isLocalDevServer() {
        final String serverDefault = sProperties.getProperty("server.default");
        return (serverDefault.equals("debug") || serverDefault.equals("emulator"));
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
