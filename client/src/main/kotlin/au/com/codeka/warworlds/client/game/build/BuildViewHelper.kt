package au.com.codeka.warworlds.client.game.build

import android.widget.ImageView
import au.com.codeka.warworlds.common.proto.Design
import com.squareup.picasso.Picasso

/**
 * Helper class for working with build views and stuff.
 */
object BuildViewHelper {
  @JvmStatic
  fun setDesignIcon(design: Design, imageView: ImageView?) {
    Picasso.get()
        .load("file:///android_asset/sprites/" + design.image_url)
        .into(imageView)
  }
}