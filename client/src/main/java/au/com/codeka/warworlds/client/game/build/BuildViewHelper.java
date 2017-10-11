package au.com.codeka.warworlds.client.game.build;

import android.widget.ImageView;
import au.com.codeka.warworlds.common.proto.Design;
import com.squareup.picasso.Picasso;

/**
 * Helper class for working with build views and stuff.
 */
public class BuildViewHelper {
  public static void setDesignIcon(Design design, ImageView imageView) {
    Picasso.with(imageView.getContext())
        .load("file:///android_asset/sprites/" + design.image_url)
        .into(imageView);
  }
}
