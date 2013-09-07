package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import au.com.codeka.common.model.Empire;


public class EmpireHelper {
    private static Bitmap sBaseShield;
    private static HashMap<String, Bitmap> sEmpireShields;

    static {
        sEmpireShields = new HashMap<String, Bitmap>();
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public static Bitmap getShield(Context context, Empire empire) {
        Bitmap bmp = sEmpireShields.get(empire.key);
        if (bmp == null) {
            if (sBaseShield == null) {
                AssetManager assetManager = context.getAssets();
                InputStream ins;
                try {
                    ins = assetManager.open("img/shield.png");
                } catch (IOException e) {
                    // should never happen!
                    return null;
                }

                try {
                    sBaseShield = BitmapFactory.decodeStream(ins);
                } finally {
                    try {
                        ins.close();
                    } catch (IOException e) {
                    }
                }
            }

            int width = sBaseShield.getWidth();
            int height = sBaseShield.getHeight();
            int[] pixels = new int[width * height];
            sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

            int newColour = getShieldColor(empire);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == Color.MAGENTA) {
                    pixels[i] = newColour;
                }
            }

            bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            sEmpireShields.put(empire.key, bmp);
        }

        return bmp;
    }

    public static int getShieldColor(Empire empire) {
        Random rand = new Random(empire.key.hashCode());
        return Color.rgb(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }

    public static float[] getShieldColorFloats(Empire empire) {
        Random rand = new Random(empire.key.hashCode());
        return new float[] {((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            1.0f};
    }
}
