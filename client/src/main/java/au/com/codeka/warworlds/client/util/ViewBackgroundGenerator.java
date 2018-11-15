package au.com.codeka.warworlds.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

/**
 * Generates an image that can be used at the background for the various non-game activities
 * (such as the startup activity, account activities, etc).
 */
public class ViewBackgroundGenerator {
  private static final Log log = new Log("BackgroundGenerator");

  /**
   * This is the {@link Bitmap} which contains the background. We'll create it the first time you
   * set a background, and then keep it and reuse it.
   */
  @Nullable private static Bitmap bitmap;

  /**
   * The renderer we'll use to render the background.
   */
  @Nullable private static BackgroundRenderer renderer;

  /**
   * The last seed we used to generate the bitmap. If we're called with the same seed again, we'll
   * just reuse the current bitmap.
   */
  @Nullable private static Long lastSeed;

  // The scale to apply to the bitmap.
  private static float bitmapScale;

  public interface OnDrawHandler {
    void onDraw(Canvas canvas);
  }

  /**
   * Sets the background of the given {@link View} to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   */
  public static void setBackground(View view) {
    setBackground(view, (OnDrawHandler) null);
  }

  /**
   * Sets the background of the given {@link View} to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   * @param onDrawHandler An optional {@link OnDrawHandler} that we'll call when we draw the
   *                      background.
   */
  public static void setBackground(View view, @Nullable OnDrawHandler onDrawHandler) {
    setBackground(view, onDrawHandler, new Random().nextLong());
  }

  /**
   * Sets the background of the given {@link View} to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   * @param onDrawHandler An optional {@link OnDrawHandler} that we'll call when we draw the
   *                      background.
   * @param seed A seed that we use to generate the image. You can use this to ensure the same
   *             image gets generated for the same seed.
   */
  public static void setBackground(View view, @Nullable OnDrawHandler onDrawHandler, long seed) {
    Preconditions.checkState(Threads.UI.isCurrentThread());

    if (bitmap == null || renderer == null) {
      WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
      Display display = wm.getDefaultDisplay();
      DisplayMetrics metrics = new DisplayMetrics();
      display.getMetrics(metrics);

      bitmapScale = metrics.density;
      bitmap = Bitmap.createBitmap(
          (int)(metrics.widthPixels / bitmapScale),
          (int)(metrics.heightPixels / bitmapScale),
          Bitmap.Config.ARGB_8888);
      renderer = new BackgroundRenderer(view.getContext());
    }

    if (lastSeed == null || seed != lastSeed) {
      renderer.render(bitmap, seed);
      lastSeed = seed;
    }

    setBackground(view, new BackgroundDrawable(bitmap, onDrawHandler));
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

  /** A custom {@link Drawable} which we use to draw the bitmap to the screen. */
  public static class BackgroundDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint;
    private final Matrix matrix;
    @Nullable private final OnDrawHandler onDrawHandler;

    public BackgroundDrawable(Bitmap bitmap, @Nullable OnDrawHandler onDrawHandler) {
      this.bitmap = bitmap;
      this.onDrawHandler = onDrawHandler;
      paint = new Paint();
      paint.setStyle(Paint.Style.STROKE);
      paint.setARGB(255, 255, 255, 255);
      matrix = new Matrix();
      matrix.setScale(bitmapScale, bitmapScale);
    }

    @Override
    public void draw(Canvas canvas) {
      canvas.drawBitmap(bitmap, matrix, paint);
      if (onDrawHandler != null) {
        onDrawHandler.onDraw(canvas);
      }
    }

    @Override
    public int getIntrinsicWidth() {
      return bitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
      return bitmap.getHeight();
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

  /**
   * This class is responsible for actually rendering the background.
   */
  private static class BackgroundRenderer {
    private Paint backgroundPaint;

    private Bitmap starfieldBitmap;
    private Bitmap gasBitmap;

    public BackgroundRenderer(Context context) {
      try {
        AssetManager assetMgr = context.getAssets();
        starfieldBitmap = loadBitmap(assetMgr, "stars/starfield.png");
        gasBitmap = loadBitmap(assetMgr, "stars/gas.png");
      } catch (Exception e) {
        // Ignore.
      }
    }

    /**
     * Renders the background to the given bitmap, which we can then use to render the background
     * again later.
     */
    private void render(Bitmap bmp, long seed) {
      // start off black
      Canvas canvas = new Canvas(bmp);
      canvas.drawColor(Color.BLACK);

      Rect src;
      Rect dest;
      Random r = new Random(seed);
      backgroundPaint = new Paint();
      backgroundPaint.setStyle(Paint.Style.STROKE);
      backgroundPaint.setARGB(255, 255, 255, 255);

      src = new Rect(0, 0, starfieldBitmap.getWidth(), starfieldBitmap.getHeight());
      for (int x = 0; x < bmp.getWidth(); x += src.width()) {
        for (int y = 0; y < bmp.getHeight(); y += src.height()) {
          dest = new Rect(x, y, x + src.width(), y + src.height());
          canvas.drawBitmap(starfieldBitmap, src, dest, backgroundPaint);
        }
      }

      int gasSize = gasBitmap.getWidth() / 4;
      for (int i = 0; i < 10; i++) {
        int x = r.nextInt(4) * gasSize;
        int y = r.nextInt(4) * gasSize;
        src = new Rect(x, y, x + gasSize, y + gasSize);

        x = r.nextInt(canvas.getWidth()) - src.width();
        y = r.nextInt(canvas.getHeight()) - src.height();
        dest = new Rect(x, y, x + (src.width() * 2), y + (src.height() * 2));
        canvas.drawBitmap(gasBitmap, src, dest, backgroundPaint);
      }
    }

    /** Loads all bitmaps from a given asset subfolder into an array. */
    private Bitmap loadBitmap(AssetManager assetMgr, String path) {
      Bitmap bitmap = null;
      InputStream ins = null;
      try {
        ins = assetMgr.open(path);
        bitmap = BitmapFactory.decodeStream(ins, null, new BitmapFactory.Options());
      } catch (IOException e) {
        log.error("Error loading image :", path, e);
      } finally {
        if (ins != null) {
          try {
            ins.close();
          } catch (IOException e) {
          }
        }
      }
      return bitmap;
    }
  }
}
