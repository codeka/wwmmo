package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;


public class Empire extends BaseEmpire {
    private static Bitmap sBaseShield;
    private Bitmap mEmpireShield;

    @Override
    protected BaseEmpireRank createEmpireRank(Messages.EmpireRank pb) {
        EmpireRank er = new EmpireRank();
        if (pb != null) {
            er.fromProtocolBuffer(pb);
        }
        return er;
    }

    @Override
    protected BaseStar createStar(Messages.Star pb) {
        StarSummary s = new StarSummary();
        if (pb != null) {
            s.fromProtocolBuffer(pb);
        }
        return s;
    }

    @Override
    protected BaseAlliance createAlliance(Messages.Alliance pb) {
        Alliance a = new Alliance();
        if (pb != null) {
            a.fromProtocolBuffer(pb);
        }
        return a;
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context) {
        if (mEmpireShield == null) {
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

            int newColour = getShieldColor();
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == Color.MAGENTA) {
                    pixels[i] = newColour;
                }
            }

            mEmpireShield = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        }

        return mEmpireShield;
    }

    public int getShieldColor() {
        Random rand = new Random(mKey.hashCode());
        return Color.rgb(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }
    public float[] getShieldColorFloats() {
        Random rand = new Random(mKey.hashCode());
        return new float[] {((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            1.0f};
    }
}
