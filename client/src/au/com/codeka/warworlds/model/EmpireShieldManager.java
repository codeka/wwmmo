package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

public class EmpireShieldManager {
    public static EmpireShieldManager i = new EmpireShieldManager();

    private Bitmap sBaseShield;
    private Map<String, SoftReference<Bitmap>> mEmpireShields;

    private EmpireShieldManager() {
        mEmpireShields = new HashMap<String, SoftReference<Bitmap>>();
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context, Empire empire) {
        Bitmap bmp = null;
        SoftReference<Bitmap> bmpref = mEmpireShields.get(empire.getKey());
        if (bmpref != null) {
            bmp = bmpref.get();
        }

        if (bmp == null) {
            ensureBaseImage(context);

            int width = sBaseShield.getWidth();
            int height = sBaseShield.getHeight();
            int[] pixels = new int[width * height];
            sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

            int newColour = getShieldColour(empire);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == Color.MAGENTA) {
                    pixels[i] = newColour;
                }
            }

            bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            mEmpireShields.put(empire.getKey(), new SoftReference<Bitmap>(bmp));
        }

        return bmp;
    }

    public Bitmap combineShieldImage(Context context, Bitmap otherImage) {
        ensureBaseImage(context);

        int width = sBaseShield.getWidth();
        int height = sBaseShield.getHeight();
        int[] pixels = new int[width * height];
        sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

        float sx = (float) otherImage.getWidth() / (float) width;
        float sy = (float) otherImage.getHeight() / (float) height;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.MAGENTA) {
                int y = i / width;
                int x = i % width;
                pixels[i] = otherImage.getPixel((int)(x * sx), (int)(y * sy));
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public int getShieldColour(Empire empire) {
        if (empire.getKey() == null) {
            return Color.TRANSPARENT;
        }

        Random rand = new Random(empire.getKey().hashCode());
        return Color.rgb(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }

    public float[] getShieldColourFloats(Empire empire) {
        if (empire.getKey() == null) {
            return new float[] {0.0f, 0.0f, 0.0f, 0.0f};
        }

        Random rand = new Random(empire.getKey().hashCode());
        return new float[] {((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            1.0f};
    }

    private void ensureBaseImage(Context context) {
        if (sBaseShield == null) {
            AssetManager assetManager = context.getAssets();
            InputStream ins;
            try {
                ins = assetManager.open("img/shield.png");
            } catch (IOException e) {
                // should never happen!
                return;
            }

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPurgeable = true;
                opts.inInputShareable = true;
                sBaseShield = BitmapFactory.decodeStream(ins, null, opts);
            } finally {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
