package au.com.codeka.warworlds;

import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;

/**
 * Registers this device with FCM and notifies the server of our device key.
 */
public class DeviceRegistrar {
  private static final Log log = new Log("DeviceRegistrar");

  /** Registers this device on the server. This MUST be called on a background thread. */
  public static String register() throws ApiException {
    final SharedPreferences settings = Util.getSharedPreferences();

    Messages.DeviceRegistration registration = Messages.DeviceRegistration.newBuilder()
        .setDeviceId(Secure.getString(App.i.getContentResolver(), Secure.ANDROID_ID))
        .setDeviceBuild(android.os.Build.DISPLAY)
        .setDeviceManufacturer(android.os.Build.MANUFACTURER)
        .setDeviceModel(android.os.Build.MODEL).setDeviceVersion(android.os.Build.VERSION.RELEASE)
        .build();

    // the post will update the key field in the protocol buffer for us
    ApiRequest request = new ApiRequest.Builder("devices", "POST")
        .body(registration)
        .build();
    RequestManager.i.sendRequestSync(request);
    registration = request.body(Messages.DeviceRegistration.class);
    if (registration == null) {
      forgetDeviceRegistration();
      throw new ApiException("Error registering device.");
    }
    String registrationKey = registration.getKey();
    log.info("Got registration key: %s", registrationKey);

    SharedPreferences.Editor editor = settings.edit();
    editor.putString("DeviceRegistrar.registrationKey", registrationKey);
    editor.apply();

    return registrationKey;
  }

  public static void updateFcmToken(final String fcmToken) {
    String deviceRegistrationKey = getDeviceRegistrationKey();
    if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
      return;
    }

    Messages.DeviceRegistration registrationPb = Messages.DeviceRegistration.newBuilder()
        .setFcmToken(fcmToken)
        .setKey(deviceRegistrationKey).build();

    RequestManager.i.sendRequest(new ApiRequest.Builder("devices/" + deviceRegistrationKey, "PUT")
        .body(registrationPb)
        .build());
  }

  /**
   * "Forget's" this device's registration.
   *
   * @param unregisterFromServer If true, we'll let the server know to no longer send us
   *                             notifications by also removing our registration. If false,
   *                             we won't tell the server so that we continue to receive
   *                             notifications. This is useful if you just want to switch realms,
   *                             for example.
   */
  public static void unregister(boolean unregisterFromServer) {
    String registrationKey = getDeviceRegistrationKey();
    if (registrationKey == null || registrationKey.length() == 0) {
      log.info("No unregister required, device not registered.");
      return;
    }

    if (unregisterFromServer) {
      try {
        RequestManager.i.sendRequest(
            new ApiRequest.Builder("devices/" + registrationKey, "DELETE").build());
      } catch (Exception ex) {
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
    editor.apply();
  }
}
