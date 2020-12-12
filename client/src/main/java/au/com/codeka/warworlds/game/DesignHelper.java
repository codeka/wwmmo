package au.com.codeka.warworlds.game;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.ShipDesign;

public class DesignHelper {
  public static void setDesignIcon(Design design, ImageView imageView) {
    Picasso.get()
        .load("file:///android_asset/sprites/" + design.getSpriteName() + ".png")
        .into(imageView);
  }

  public static void setDesignIcon(ShipDesign.Upgrade upgrade, ImageView imageView) {
    Picasso.get()
        .load("file:///android_asset/sprites/" + upgrade.getSpriteName() + ".png")
        .into(imageView);
  }

  @Nullable
  public static Bitmap load(Design design) {
    try {
      return Picasso.get()
          .load("file:///android_asset/sprites/" + design.getSpriteName() + ".png")
          .get();
    } catch (IOException e) {
      return null;
    }
  }
}
