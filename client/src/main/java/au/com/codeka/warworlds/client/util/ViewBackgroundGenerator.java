package au.com.codeka.warworlds.client.util;

import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Generates an image that can be used at the background for the various non-game activities
 * (such as the startup activity, account activities, etc).
 */
public class ViewBackgroundGenerator {
  public interface OnDrawHandler {
    void onDraw(Canvas canvas);
  }

  public static void setBackground(View view) {
    setBackground(view, (OnDrawHandler) null);
  }

  /**
   * Sets the background of the given {@link View} to our custom bitmap.
   *
   * @param view The view to set the background on.
   * @param onDrawHandler An optional {@link OnDrawHandler} that we'll call when we draw the
   *                      background.
   */
  public static void setBackground(View view, @Nullable OnDrawHandler onDrawHandler) {
    WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    android.util.DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);

    StarfieldBackgroundRenderer renderer =
        new StarfieldBackgroundRenderer(view.getContext(), new long[]{new Random().nextLong()});
    BackgroundDrawable drawable = new BackgroundDrawable(
        renderer, metrics.heightPixels, onDrawHandler);
    setBackground(view, drawable);
  }

  @SuppressLint("NewApi")
  @SuppressWarnings("deprecation")
  private static void setBackground(View view, BackgroundDrawable drawable) {
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackgroundDrawable(drawable);
    } else {
      view.setBackground(drawable);
    }
  }

  public static class BackgroundDrawable extends Drawable {
    private final StarfieldBackgroundRenderer renderer;
    private final int baseSize;
    @Nullable private final OnDrawHandler onDrawHandler;

    public BackgroundDrawable(
        StarfieldBackgroundRenderer renderer,
        int deviceHeight,
        @Nullable OnDrawHandler onDrawHandler) {
      this.renderer = renderer;
      this.onDrawHandler = onDrawHandler;
      baseSize = Math.max(deviceHeight, 1024);
    }

    @Override
    public void draw(Canvas canvas) {
      renderer.drawBackground(canvas, 0, 0, baseSize, baseSize);
      if (onDrawHandler != null) {
        onDrawHandler.onDraw(canvas);
      }
    }

    @Override
    public int getIntrinsicWidth() {
      return baseSize;
    }

    @Override
    public int getIntrinsicHeight() {
      return baseSize;
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
  }
}
