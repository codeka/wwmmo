
package au.com.codeka.warworlds;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SpriteManager;

/**
 * Utility methods for getting the base URL for client-server communication and
 * retrieving shared preferences.
 */
public class Util {
  private static final Log log = new Log("Util");
  /**
   * Key for shared preferences.
   */
  private static final String SHARED_PREFS = "WARWORLDS_PREFS";

  private static Properties sProperties;
  private static boolean sWasSetup;
  private static String sVersion;

  /**
   * This should be called from every entry-point into the process to make
   * sure the various globals are up and running.
   */
  public static boolean setup(Context context) {
    if (sWasSetup) {
      return false;
    }

    LogImpl.setup();

    SpriteManager.i.setup(context);
    DesignManager.setup(context);
    PurchaseManager.i.setup(context);
    RealmManager.i.setup();
    RequestManager.i.setup(context);

    try {
      PackageInfo packageInfo = App.i.getPackageManager().getPackageInfo(App.i.getPackageName(), 0);
      sVersion = packageInfo.versionName;
    } catch (Exception e) {
      sVersion = "??";
    }

    sWasSetup = true;
    return true;
  }

  public static boolean isSetup() {
    return sWasSetup;
  }

  public static String getVersion() {
    return sVersion;
  }

  /**
   * Must be called before other methods on this class. We load up the initial properties,
   * preferences and settings to make later calls easier (and not require a \c Context parameter).
   */
  public static Properties loadProperties() {
    if (sProperties != null) {
      // if it's already loaded, don't do it again
      return sProperties;
    }

    // load the warworlds.properties file and populate mProperties.
    AssetManager assetManager = App.i.getAssets();

    InputStream inputStream = null;
    try {
      inputStream = assetManager.open("warworlds.properties");
      sProperties = new Properties();
      sProperties.load(inputStream);
    } catch (IOException e) {
      log.info("exception", e);
      sProperties = null;
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
      }
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
   * Returns true if we are running against a dev mode appengine instance.
   */
  public static boolean isDebug() {
    final String debugValue = sProperties.getProperty("debug");
    return debugValue.equals("true");
  }

  /**
   * Helper method to get a SharedPreferences instance.
   */
  public static SharedPreferences getSharedPreferences() {
    return App.i.getSharedPreferences(SHARED_PREFS, 0);
  }

  public static boolean isAnonymous() {
    return getSharedPreferences().getString("AccountName", "").endsWith("@anon.war-worlds.com");
  }
}
