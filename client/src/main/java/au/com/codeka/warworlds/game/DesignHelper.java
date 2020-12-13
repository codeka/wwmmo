package au.com.codeka.warworlds.game;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.ShipDesign;
import kotlin.comparisons.UComparisonsKt;

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

  public static PendingAsyncLoad loadAsync(Design design) {
    return loadAsync(design, null);
  }

  public static PendingAsyncLoad loadAsync(Design design, @Nullable Runnable completeRunnable) {
    final PendingAsyncLoad pending = new PendingAsyncLoad();
    Picasso.get()
        .load("file:///android_asset/sprites/" + design.getSpriteName() + ".png")
        .into(new Target() {
          @Override
          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            pending.bitmap = bitmap;
            if (completeRunnable != null) {
              completeRunnable.run();
            }
          }

          @Override
          public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            pending.placeholder = errorDrawable;
            if (completeRunnable != null) {
              completeRunnable.run();
            }
          }

          @Override
          public void onPrepareLoad(Drawable placeHolderDrawable) {
            pending.placeholder = placeHolderDrawable;
          }
        });
    return pending;
  }

    /**
     * A {@link PendingAsyncLoad} is returned from {@link #loadAsync} and contains a bitmap that will
     * be null until the load has completed.
     */
  public static class PendingAsyncLoad {
    public Drawable placeholder;
    public Bitmap bitmap;
  }
}
