package au.com.codeka.warworlds.game.starfield;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

/**
 * An {@link IBitmapTextureAtlasSource} that returns a bitmap to use as a texture for the tactical
 * sprite that covers a sector.
 */
public class TacticalBitmapTextureSource extends BaseTextureAtlasSource
    implements IBitmapTextureAtlasSource {

  // The size of the texture, in pixels, that we'll generate.
  private static final int TEXTURE_SIZE = 128;

  // The radius of the circle, in pixels, that we'll put around each empire.
  private static final int CIRCLE_RADIUS = 20;

  private Sector sector;
  private Bitmap bitmap;

  public static TacticalBitmapTextureSource create(Sector sector) {
    return new TacticalBitmapTextureSource(sector);
  }

  private TacticalBitmapTextureSource(Sector sector) {
    this(sector, null);
  }

  private TacticalBitmapTextureSource(Sector sector, Bitmap bitmap) {
    super(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
    this.sector = sector;
    this.bitmap = bitmap;
  }

  @Override
  public IBitmapTextureAtlasSource deepCopy() {
    return new TacticalBitmapTextureSource(sector, bitmap);
  }

  @Override
  public Bitmap onLoadBitmap(Bitmap.Config config) {
    return onLoadBitmap(config, false);
  }

  @Override
  public Bitmap onLoadBitmap(Bitmap.Config config, boolean mutable) {
    if (bitmap == null) {
      // NOTE: This assumes the config never changes, is that OK?
      bitmap = createBitmap(config);
    }
    return bitmap;
  }

  private Bitmap createBitmap(Bitmap.Config config) {
    Bitmap bmp = Bitmap.createBitmap(TEXTURE_SIZE, TEXTURE_SIZE, config);
    Canvas canvas = new Canvas(bmp);

    Paint paint = new Paint();
    paint.setStyle(Paint.Style.FILL);

    // We have to look at sectors around this one as well, so that edges look right
    for (int offsetY = -1; offsetY <= 1; offsetY++) {
      for (int offsetX = -1; offsetX <= 1; offsetX++) {
        Sector s;
        if (offsetX == 0 && offsetY == 0) {
          s = sector;
        } else {
          s = SectorManager.i.getSector(sector.getX() + offsetX, sector.getY() + offsetY);
        }
        if (s == null) {
          continue;
        }

        drawCircles(s, offsetX, offsetY, canvas, paint);
      }
    }

    return bmp;
  }

  private void drawCircles(Sector sector, int offsetX, int offsetY, Canvas canvas, Paint paint) {
    float scaleFactor = (float) TEXTURE_SIZE / Sector.SECTOR_SIZE;

    for (BaseStar baseStar : sector.getStars()) {
      float x = (baseStar.getOffsetX() + offsetX * Sector.SECTOR_SIZE) * scaleFactor;
      float y = (baseStar.getOffsetY() + offsetY * Sector.SECTOR_SIZE) * scaleFactor;
      float radius = CIRCLE_RADIUS;

      if (x < -radius || x > TEXTURE_SIZE + radius || y < -radius || y > TEXTURE_SIZE + radius) {
        // if it's completely off the bitmap, skip drawing it
        continue;
      }

      Empire empire = getEmpire((Star) baseStar);
      if (empire == null) {
        continue;
      }
      paint.setColor(EmpireShieldManager.i.getShieldColour(empire));

      canvas.drawCircle(x, y, radius, paint);
    }
  }

  private static Empire getEmpire(Star star) {
    // if it's a wormhole, the empire is the owner of the wormhole
    BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
    if (wormholeExtra != null) {
      return EmpireManager.i.getEmpire(wormholeExtra.getEmpireID());
    }

    // otherwise, pick the first colony we find to represent the star
    // TODO: what if the star is colonized by more than one empire?
    for (BaseColony colony : star.getColonies()) {
      if (colony.getEmpireKey() != null) {
        return EmpireManager.i.getEmpire(Integer.parseInt(colony.getEmpireKey()));
      }
    }

    return null;
  }
}
