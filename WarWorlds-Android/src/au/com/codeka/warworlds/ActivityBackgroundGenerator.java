package au.com.codeka.warworlds;

import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;

/**
 * Generates an image that can be used at the background for the various non-game activities
 * (such as the startup activity, account activities, etc).
 */
public class ActivityBackgroundGenerator {
    /**
     * Sets the background of the given \c View to our custom bitmap.
     */
    public static void setBackground(View view) {
        WindowManager wm = (WindowManager) view.getContext().getSystemService(
                Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        android.util.DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        StarfieldBackgroundRenderer renderer = new StarfieldBackgroundRenderer(view.getContext(),
                new long[] {new Random().nextLong()});
        BackgroundDrawable drawable = new BackgroundDrawable(renderer, metrics.heightPixels);
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
        private StarfieldBackgroundRenderer mRenderer;
        private int mBaseSize;

        public BackgroundDrawable(StarfieldBackgroundRenderer renderer, int deviceHeight) {
            mRenderer = renderer;
            mBaseSize = 1024;
            if (deviceHeight > mBaseSize) {
                mBaseSize = deviceHeight;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            mRenderer.drawBackground(canvas, 0, 0, mBaseSize, mBaseSize);
        }

        @Override
        public int getIntrinsicWidth() {
            return mBaseSize;
        }

        @Override
        public int getIntrinsicHeight() {
            return mBaseSize;
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
