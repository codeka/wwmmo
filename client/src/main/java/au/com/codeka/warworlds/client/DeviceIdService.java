package au.com.codeka.warworlds.client;

import au.com.codeka.warworlds.common.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Our implementation of {@link FirebaseInstanceIdService}.
 */
public class DeviceIdService extends FirebaseInstanceIdService {
  private static final Log log = new Log("DeviceIdService");

  @Override
  public void onTokenRefresh() {
    // Get updated InstanceID token.
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    log.debug("Refreshed token: " + refreshedToken);

    // TODO: Implement this method to send any registration to your app's servers.

  }
}
