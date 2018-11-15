package au.com.codeka.warworlds.client.game.build;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import au.com.codeka.warworlds.common.proto.Design;

/**
 * Helper class for working with build views and stuff.
 */
public class BuildViewHelper {
  public static void setDesignIcon(Design design, ImageView imageView) {
    Picasso.get()
        .load("file:///android_asset/sprites/" + design.image_url)
        .into(imageView);
  }
}
