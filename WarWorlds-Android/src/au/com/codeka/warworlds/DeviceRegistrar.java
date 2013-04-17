
package au.com.codeka.warworlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
    private static Logger log = LoggerFactory.getLogger(DeviceRegistrar.class);

    public static String register(final Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);

        String registrationKey = null;
        try {
            Messages.DeviceRegistration registration = Messages.DeviceRegistration.newBuilder()
                .setDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID))
                .setDeviceBuild(android.os.Build.DISPLAY)
                .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                .setDeviceModel(android.os.Build.MODEL)
                .setDeviceVersion(android.os.Build.VERSION.RELEASE)
                .build();

            // the post will update the key field in the protocol buffer for us
            registration = ApiClient.postProtoBuf("devices", registration,
                    Messages.DeviceRegistration.class);
            registrationKey = registration.getKey();
            log.info("Got registration key: "+registrationKey);
        } catch(Exception ex) {
            log.error("Failure registring device.", ex);
            forgetDeviceRegistration(context);
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
                String deviceRegistrationKey = getDeviceRegistrationKey(context);
                Messages.DeviceRegistration regpb = Messages.DeviceRegistration.newBuilder()
                        .setGcmRegistrationId(gcmRegistrationID)
                        .setKey(deviceRegistrationKey)
                        .build();

                String url = "devices/"+deviceRegistrationKey;
                try {
                    ApiClient.putProtoBuf(url, regpb);
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

    public static void unregister(final Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);
        String registrationKey = settings.getString("DeviceRegistrar.registrationKey", "");
        if (registrationKey == "") {
            log.info("No unregistration required, device not registered.");
            return;
        }

        try {
            String url = "devices/" + registrationKey;
            ApiClient.delete(url);
        } catch(Exception ex) {
            log.error("Failure unregistering device.", ex);
        }

        forgetDeviceRegistration(context);
    }

    public static String getDeviceRegistrationKey(Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);
        return settings.getString("DeviceRegistrar.registrationKey", "");
    }

    private static void forgetDeviceRegistration(Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("DeviceRegistrar.registrationKey");
        editor.commit();
    }
}
