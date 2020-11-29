package au.com.codeka.warworlds;

import au.com.codeka.common.protobuf.Messages;

public class FeatureFlags {
  private static Messages.FeatureFlags values;

  public static void setup(Messages.FeatureFlags values) {
    if (values == null) {
      FeatureFlags.values = Messages.FeatureFlags.getDefaultInstance();
    } else {
      FeatureFlags.values = values;
    }
  }

  public static Messages.FeatureFlags get() {
    return values;
  }
}
