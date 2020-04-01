package au.com.codeka.warworlds;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.accessibility.AccessibilityManager;

import com.google.common.collect.Lists;

import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;

/**
 * A class that gathers info about active accessibility services, and reports them to the server.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class AccessibilityServiceReporter {
  private static final Log log = new Log("AccessibilityServiceReporter");

  private static int[] ALL_CAPABILITIES = new int[]{
      AccessibilityServiceInfo.CAPABILITY_CAN_CONTROL_MAGNIFICATION,
      AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES,
      AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS,
      AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES,
      AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION,
      AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT,
  };

  public static Messages.AccessibilitySettingsInfo get(Context context) {
    Messages.AccessibilitySettingsInfo.Builder builder =
        Messages.AccessibilitySettingsInfo.newBuilder();

    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        // This version doesn't support the APIs we need, so just ignore.
        builder.setSupported(false);
        return builder.build();
      }
      builder.setSupported(true);

      AccessibilityManager am =
          (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
      if (am == null) {
        builder.setSupported(false);
        return builder.build();
      }

      List<AccessibilityServiceInfo> enabledServices =
          am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

      for (AccessibilityServiceInfo enabledService : enabledServices) {
        Messages.AccessibilitySettingsInfo.AccessibilityService.Builder serviceBuilder =
            Messages.AccessibilitySettingsInfo.AccessibilityService.newBuilder();

        serviceBuilder.setName(enabledService.getId());

        int capabilities = enabledService.getCapabilities();
        for (int i = 0; i < ALL_CAPABILITIES.length; i++) {
          if ((capabilities & ALL_CAPABILITIES[i]) != 0) {
            serviceBuilder.addCapability(
                AccessibilityServiceInfo.capabilityToString(ALL_CAPABILITIES[i]));
          }
        }

        if (enabledService.packageNames != null) {
          serviceBuilder.addAllPackageNames(Lists.newArrayList(enabledService.packageNames));
        }

        builder.addService(serviceBuilder);
      }
    } catch (Throwable e) {
      log.error("Error getting accessibility services.", e);
    }

    return builder.build();
  }

  /**
   * Watches for changes in the number of accessibility services that are enabled on the devices
   * and calls the given runnable when a service is enabled or disabled.
   *
   * We only call the callback once and then unregister ourselves. If you want to get multiple
   * callbacks, you must re-register every time.
   */
  public static void watchForChanges(Context context, Runnable changeOccurredRunnable) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }

    AccessibilityManager am =
        (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (am == null) {
      return;
    }

    int numEnabledServices =
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).size();
    am.addAccessibilityStateChangeListener(
        new ChangeListener(am, numEnabledServices, changeOccurredRunnable));
  }

  private static final class ChangeListener
      implements AccessibilityManager.AccessibilityStateChangeListener {
    private final AccessibilityManager am;
    private final Runnable callback;
    private int numEnabledServices;

    public ChangeListener(AccessibilityManager am, int numEnabledServices, Runnable callback) {
      this.am = am;
      this.callback = callback;
      this.numEnabledServices = numEnabledServices;
    }

    @Override
    public void onAccessibilityStateChanged(boolean b) {
      int currentlyEnabledServices =
          am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).size();
      if (currentlyEnabledServices != numEnabledServices) {
        am.removeAccessibilityStateChangeListener(this);
        callback.run();
      }
    }

  }
}
