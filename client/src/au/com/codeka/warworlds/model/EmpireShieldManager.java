package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.andengine.opengl.texture.region.ITextureRegion;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseGlActivity;

public class EmpireShieldManager extends ShieldManager {
    public static EmpireShieldManager i = new EmpireShieldManager();

    private Bitmap sBaseShield;

    private EmpireShieldManager() {
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context, Empire empire) {
        return getShield(context, getShieldInfo(empire));
    }

    /** Gets (or creates) the empire's shield as an andengine texture. */
    public ITextureRegion getShieldTexture(BaseGlActivity glActivity, Empire empire) {
        return getShieldTexture(glActivity, getShieldInfo(empire));
    }

    @Override
    protected Bitmap processShieldImage(Bitmap bmp) {
        return combineShieldImage(App.i, bmp);
    }

    @Override
    protected Bitmap getDefaultShield(ShieldInfo shieldInfo) {
        int colour = getShieldColour(shieldInfo);
        return combineShieldColour(App.i, colour);
    }

    @Override
    protected String getFetchUrl(ShieldInfo shieldInfo) {
        return "empires/" + shieldInfo.id + "/shield";
    }

    /**
     * Combines the base shield image with the given colour.
     */
    public Bitmap combineShieldColour(Context context, int colour) {
        ensureBaseImage(context);

        int width = sBaseShield.getWidth();
        int height = sBaseShield.getHeight();
        int[] pixels = new int[width * height];
        sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.MAGENTA) {
                pixels[i] = colour;
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Combines the given image with the base shield image.
     */
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
        return getShieldColour(getShieldInfo(empire));
    }

    public int getShieldColour(ShieldInfo shieldInfo) {
        if (shieldInfo.id == 0) {
            return Color.TRANSPARENT;
        }

        Random rand = new Random(Integer.toString(shieldInfo.id).hashCode());
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

    private ShieldInfo getShieldInfo(Empire empire) {
        int id = empire.getKey() == null ? 0 : Integer.parseInt(empire.getKey());
        Long lastUpdate = empire.getShieldLastUpdate() == null ? null : empire.getShieldLastUpdate().getMillis();
        return new ShieldInfo("empire", id, lastUpdate);
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
