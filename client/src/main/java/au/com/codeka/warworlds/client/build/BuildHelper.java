package au.com.codeka.warworlds.client.build;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import au.com.codeka.warworlds.common.proto.Design;

public class BuildHelper {
  public static void setDesignIcon(Design design, ImageView imageView) {
    Picasso.with(imageView.getContext())
        .load("file:///android_asset/sprites/" + design.image_url)
        .into(imageView);
  }
}
