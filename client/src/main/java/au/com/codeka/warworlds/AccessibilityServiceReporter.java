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
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;

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

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      // This version doesn't support the APIs we need, so just ignore.
      builder.setSupported(false);
      return builder.build();
    }
    builder.setSupported(true);

    AccessibilityManager am =
        (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
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

    return builder.build();
  }
}
