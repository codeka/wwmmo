
package au.com.codeka.warworlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.DeviceRegistration;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
    private static Logger log = LoggerFactory.getLogger(DeviceRegistrar.class);

    public static String register() {
        final SharedPreferences settings = Util.getSharedPreferences();

        String registrationKey = null;
        try {
            DeviceRegistration registration = new DeviceRegistration.Builder()
                .device_id(Secure.getString(App.i.getContentResolver(), Secure.ANDROID_ID))
                .device_build(android.os.Build.DISPLAY)
                .device_manufacturer(android.os.Build.MANUFACTURER)
                .device_model(android.os.Build.MODEL)
                .device_version(android.os.Build.VERSION.RELEASE)
                .build();

            // the post will update the key field in the protocol buffer for us
            registration = ApiClient.postProtoBuf("devices", registration,
                    DeviceRegistration.class);
            registrationKey = registration.key;
            log.info("Got registration key: "+registrationKey);
        } catch(Exception ex) {
            log.error("Failure registring device.", ex);
            forgetDeviceRegistration();
            return null;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("DeviceRegistrar.registrationKey", registrationKey);
        editor.commit();

        return registrationKey;
    }

    public static void updateGcmRegistration(final Context context,
                                             final String gcmRegistrationID) {
        new BackgroundRunner<Void>() {
            @Override
            protected Void doInBackground() {
                String deviceRegistrationKey = getDeviceRegistrationKey();
                DeviceRegistration deviceRegistration = new DeviceRegistration.Builder()
                        .gcm_registration_id(gcmRegistrationID)
                        .key(deviceRegistrationKey)
                        .build();

                String url = "devices/"+deviceRegistrationKey;
                try {
                    ApiClient.putProtoBuf(url, deviceRegistration);
                } catch (ApiException e) {
                    log.error("Could not update online status, ignored.");
                }

                return null;
            }

            @Override
            protected void onComplete(Void arg) {
            }
        }.execute();
    }

    /**
     * "Forget's" this device's registration.
     * 
     * @param unregisterFromServer If true, we'll let the server know to no longer send us
     *        notifications by also removing our registration. If false, we won't tell the server
     *        so that we continue to receive notifications. This is useful if you just want to
     *        switch realms, for example.
     */
    public static void unregister(boolean unregisterFromServer) {
        final SharedPreferences settings = Util.getSharedPreferences();
        String registrationKey = settings.getString("DeviceRegistrar.registrationKey", "");
        if (registrationKey == "") {
            log.info("No unregistration required, device not registered.");
            return;
        }

        if (unregisterFromServer) {
            try {
                String url = "devices/" + registrationKey;
                ApiClient.delete(url);
            } catch(Exception ex) {
                log.error("Failure unregistering device.", ex);
            }
        }

        forgetDeviceRegistration();
    }

    public static String getDeviceRegistrationKey() {
        final SharedPreferences settings = Util.getSharedPreferences();
        return settings.getString("DeviceRegistrar.registrationKey", "");
    }

    private static void forgetDeviceRegistration() {
        final SharedPreferences settings = Util.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("DeviceRegistrar.registrationKey");
        editor.commit();
    }
}
