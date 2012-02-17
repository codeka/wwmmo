
package au.com.codeka.warworlds;

import warworlds.Warworlds.DeviceRegistration;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
	public static String TAG = "DeviceRegistrar";

    public static void register(final Context context, final String deviceRegistrationID) {
        registerOrUnregister(context, deviceRegistrationID, true);
    }

    public static void unregister(final Context context, final String deviceRegistrationID) {
        registerOrUnregister(context, deviceRegistrationID, false);
    }

    private static void registerOrUnregister(final Context context,
            final String deviceRegistrationID, final boolean register) {

        final SharedPreferences settings = Util.getSharedPreferences(context);

        String url = "devices";

        try {
            if (register) {
                DeviceRegistration registration = DeviceRegistration.newBuilder()
                    .setDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID))
                    .setDeviceRegistrationId(deviceRegistrationID)
                    .setDeviceBuild(android.os.Build.DISPLAY)
                    .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                    .setDeviceModel(android.os.Build.MODEL)
                    .setDeviceVersion(android.os.Build.VERSION.RELEASE)
                    .build();

                ApiClient.putProtoBuf(url, registration); // TODO: check for errors...
            } else {
                url += "/registration:" + deviceRegistrationID;

                ApiClient.delete(url);
            }
        } catch(Exception ex) {
            Log.w(TAG, "Failure, got: " + ex.getMessage());
            // Clean up application state
            Util.clearDeviceRegistration(context);

            return;
        }

        if (register) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Util.DEVICE_REGISTRATION_ID, deviceRegistrationID);
            editor.commit();
        } else {
        	Util.clearDeviceRegistration(context);
        }
    }
}
