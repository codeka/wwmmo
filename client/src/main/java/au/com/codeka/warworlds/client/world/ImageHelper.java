package au.com.codeka.warworlds.client.world;

import android.content.Context;
import android.util.DisplayMetrics;

import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;

import au.com.codeka.warworlds.client.BuildConfig;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Helper class to get the URL for the various images (stars, planets, empire shields, etc).
 */
public class ImageHelper {
  private static final Map<Integer, String> BUCKET_NAMES = ImmutableMap.<Integer, String>builder()
      .put(DisplayMetrics.DENSITY_LOW, "ldpi")
      .put(DisplayMetrics.DENSITY_MEDIUM, "mdpi")
      .put(DisplayMetrics.DENSITY_HIGH, "hdpi")
      .put(DisplayMetrics.DENSITY_XHIGH, "xhdpi")
      .put(DisplayMetrics.DENSITY_XXHIGH, "xxhdpi")
      .put(DisplayMetrics.DENSITY_XXXHIGH, "xxxhdpi")
      .build();

  public static String getStarImageUrl(Context context, Star star, int width, int height) {
    String dpi = BUCKET_NAMES.get(context.getResources().getDisplayMetrics().densityDpi);
    if (dpi == null) {
      dpi = "hdpi";
    }

    return String.format(Locale.ENGLISH, "%srender/star/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), star.id, width, height, dpi);
  }

  public static String getPlanetImageUrl(
      Context context, Star star, int planetIndex, int width, int height) {
    String dpi = BUCKET_NAMES.get(context.getResources().getDisplayMetrics().densityDpi);
    if (dpi == null) {
      dpi = "hdpi";
    }

    return String.format(Locale.ENGLISH, "%srender/planet/%d/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), star.id, planetIndex, width, height, dpi);
  }

  public static String getEmpireImageUrl(
      Context context, Empire empire, int width, int height) {
    String dpi = BUCKET_NAMES.get(context.getResources().getDisplayMetrics().densityDpi);
    if (dpi == null) {
      dpi = "hdpi";
    }

    return String.format(Locale.ENGLISH, "%srender/empire/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), empire.id, width, height, dpi);
  }

}
