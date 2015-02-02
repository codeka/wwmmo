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

  private Bitmap baseShield;

  private EmpireShieldManager() {
  }

  /**
   * Gets (or creates, if there isn't one) the {@link Bitmap} that represents this Empire's
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
  protected Bitmap getDefaultShield(ShieldInfo shieldInfo) {
    int colour = getShieldColour(shieldInfo);
    return combineShieldColour(App.i, colour);
  }

  @Override
  protected String getFetchUrl(ShieldInfo shieldInfo) {
    return "empires/" + shieldInfo.id + "/shield?final=1&size=64";
  }

  /** Combines the base shield image with the given colour. */
  public Bitmap combineShieldColour(Context context, int colour) {
    ensureBaseImage(context);

    int width = baseShield.getWidth();
    int height = baseShield.getHeight();
    int[] pixels = new int[width * height];
    baseShield.getPixels(pixels, 0, width, 0, 0, width, height);

    for (int i = 0; i < pixels.length; i++) {
      if (pixels[i] == Color.MAGENTA) {
        pixels[i] = colour;
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
    return Color.rgb(rand.nextInt(100) + 100, rand.nextInt(100) + 100, rand.nextInt(100) + 100);
  }

  private ShieldInfo getShieldInfo(Empire empire) {
    int id = empire.getKey() == null ? 0 : Integer.parseInt(empire.getKey());
    Long lastUpdate =
        empire.getShieldLastUpdate() == null ? null : empire.getShieldLastUpdate().getMillis();
    return new ShieldInfo(ShieldManager.EmpireShield, id, lastUpdate);
  }

  private void ensureBaseImage(Context context) {
    if (baseShield == null) {
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
        baseShield = BitmapFactory.decodeStream(ins, null, opts);
      } finally {
        try {
          ins.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
