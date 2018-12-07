package au.com.codeka.warworlds.client.game.world;

import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Helper class to get the URL for the various images (stars, planets, empire shields, etc).
 */
public class ImageHelper {
  private static String getDensityName(int densityDpi) {
    if (densityDpi > DisplayMetrics.DENSITY_XXHIGH) {
      return "xxxhdpi";
    } else if (densityDpi > DisplayMetrics.DENSITY_XHIGH) {
      return "xxhdpi";
    } else if (densityDpi > DisplayMetrics.DENSITY_HIGH) {
      return "xhdpi";
    } else if (densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
      return "hdpi";
    } else if (densityDpi > DisplayMetrics.DENSITY_LOW) {
      return "mdpi";
    } else {
      return "ldpi";
    }
  }

  public static String getStarImageUrl(Context context, Star star, int width, int height) {
    String dpi = getDensityName(context.getResources().getDisplayMetrics().densityDpi);

    return String.format(Locale.ENGLISH, "%srender/star/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), star.id, width, height, dpi);
  }

  public static String getPlanetImageUrl(
      Context context, Star star, int planetIndex, int width, int height) {
    String dpi = getDensityName(context.getResources().getDisplayMetrics().densityDpi);

    return String.format(Locale.ENGLISH, "%srender/planet/%d/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), star.id, planetIndex, width, height, dpi);
  }

  public static String getEmpireImageUrl(
      Context context, Empire empire, int width, int height) {
    String dpi = getDensityName(context.getResources().getDisplayMetrics().densityDpi);

    return String.format(Locale.ENGLISH, "%srender/empire/%d/%dx%d/%s.png",
        ServerUrl.getUrl(), empire.id == null ? 0 : empire.id, width, height, dpi);
  }

  /** Gets the URL for fetching an empire's logo, with exact pixel dimensions (a.k.a. mdpi). */
  public static String getEmpireImageUrlExactDimens(
      Context context, Empire empire, int width, int height) {
    return String.format(Locale.ENGLISH, "%srender/empire/%d/%dx%d/mdpi.png",
        ServerUrl.getUrl(), empire.id, width, height);
  }

  /**
   * Bind an image with the given URL to the given {@link ImageView}.
   */
  public static void bindImage(ImageView view, String imageUrl) {
    Picasso.get()
        .load(imageUrl)
        .into(view);
  }

  /**
   * Bind an empire's shield image to the given {@link ImageView}.
   */
  public static void bindEmpireShield(ImageView view, @Nullable Empire empire) {
    if (empire == null) {
      view.setImageDrawable(null);
      return;
    }

    DimensionResolver resolver = new DimensionResolver(view.getContext());
    int width = (int) resolver.px2dp(view.getLayoutParams().width);
    int height = (int) resolver.px2dp(view.getLayoutParams().height);
    bindImage(view, getEmpireImageUrl(view.getContext(), empire, width, height));
  }


  /**
   * Bind a planet's image to the given {@link ImageView}.
   */
  public static void bindPlanetIcon(ImageView view, @Nullable Star star, @Nullable Planet planet) {
    if (star == null || planet == null) {
      return;
    }

    DimensionResolver resolver = new DimensionResolver(view.getContext());
    int width = (int) resolver.px2dp(view.getLayoutParams().width);
    int height = (int) resolver.px2dp(view.getLayoutParams().height);
    bindImage(view, getPlanetImageUrl(
        view.getContext(), star, star.planets.indexOf(planet), width, height));
  }
}
