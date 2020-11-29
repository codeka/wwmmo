package au.com.codeka.warworlds.server;

import java.io.File;
import java.io.IOException;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.utils.IniFileParser;

public class FeatureFlags {
  private static Messages.FeatureFlags values;

  public static void setup() throws IOException {
    Messages.FeatureFlags.Builder featureFlags = Messages.FeatureFlags.newBuilder();
    IniFileParser.builder(
        new File(Configuration.i.getConfigDirectory(), "feature-flags.ini").getAbsolutePath())
        .parse(featureFlags);
    values = featureFlags.build();
  }

  public static Messages.FeatureFlags get() {
    return values;
  }
}
