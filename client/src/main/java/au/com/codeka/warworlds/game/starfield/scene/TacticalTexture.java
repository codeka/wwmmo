package au.com.codeka.warworlds.game.starfield.scene;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.opengl.Texture;

/**
 * A [Texture] that returns a bitmap to use as a texture for the "tactical" SceneObject that
 * covers a sector.
 */
class TacticalTexture extends Texture {
  private static final Log log = new Log("TacticalTexture");

  // The size of the texture, in pixels, that we'll generate.
  private static final int TEXTURE_SIZE = 128;

  // The radius of the circle, in pixels, that we'll put around each empire.
  private static final int CIRCLE_RADIUS = 30;

  private static final int[] gradientColors = {0, 0};
  private static final float[] gradientStops = {0.1f, 1.0f};

  private static final TacticalTextureLruCache textureCache = new TacticalTextureLruCache(16);

  private final long sectorX;
  private final long sectorY;
  @Nullable
  private Bitmap bitmap;


  private TacticalTexture(long sectorX, long sectorY) {
    this.sectorX = sectorX;
    this.sectorY = sectorY;

    new BackgroundRunner<Bitmap>() {
      @Override
      protected Bitmap doInBackground() {
        return createBitmap(sectorX, sectorY);
      }

      @Override
      protected void onComplete(Bitmap bitmap) {
        TacticalTexture.this.bitmap = bitmap;
      }
    }.execute();
  }

  public void close() {
    if (bitmap != null) {
      bitmap.recycle();
    }
  }

  @Override
  public void bind() {
    if (bitmap != null) {
      setTextureId(createGlTexture());
      bitmap.recycle();
      bitmap = null;
    }
    super.bind();
  }

  private int createGlTexture() {
    Preconditions.checkState(bitmap != null);

    int[] textureHandleBuffer = new int[1];
    GLES20.glGenTextures(1, textureHandleBuffer, 0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0]);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    return textureHandleBuffer[0];
  }

  public static TacticalTexture create(long sectorX, long sectorY) {
    synchronized(textureCache) {
      Pair<Long, Long> sectorCoord = new Pair<>(sectorX, sectorY);
      TacticalTexture cachedTexture = textureCache.get(sectorCoord);
      if (cachedTexture != null) {
        return cachedTexture;
      }
      TacticalTexture texture = new TacticalTexture(sectorX, sectorY);
      textureCache.put(sectorCoord, texture);
      return texture;
    }
  }

  private static Bitmap createBitmap(long sectorX, long sectorY) {
    log.info("Generating tactical bitmap for %d,%d...", sectorX, sectorY);
    long startTime = System.currentTimeMillis();

    Bitmap bmp = Bitmap.createBitmap(TEXTURE_SIZE, TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.FILL);

    // We have to look at sectors around this one as well, so that edges look right
    for (long offsetY = -1; offsetY <= 1; offsetY ++) {
    for (long offsetX = -1; offsetX <= 1; offsetX ++) {
        drawCircles(sectorX, sectorY, offsetX, offsetY, canvas, paint);
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("Tactical bitmap for %d,%d generated in %dms", sectorX, sectorY, endTime - startTime);
    return bmp;
  }

  private static void drawCircles(
      long sectorX, long sectorY, long offsetX, long offsetY, Canvas canvas, Paint paint) {
    float scaleFactor = TEXTURE_SIZE / 1024f;
    Sector sector = SectorManager.i.getSector(sectorX, sectorY);
    if (sector == null) {
      return;
    }
    for (BaseStar star : sector.getStars()) {
      float x = (star.getOffsetX() + offsetX * 1024.0f) * scaleFactor;
      float y = (star.getOffsetY() + offsetY * 1024.0f) * scaleFactor;
      float radius = CIRCLE_RADIUS;
      if (x < -radius || x > TEXTURE_SIZE + radius || y < -radius || y > TEXTURE_SIZE + radius) {
        // if it's completely off the bitmap, skip drawing it
        continue;
      }
      int color = 0;
      Integer empireId = getEmpireId(star);
      if (empireId == null) {
        empireId = getFleetOnlyEmpire(star);
        if (empireId != null) {
          color = getShieldColour(empireId);
          color = 0x66ffffff & color;
        }
      } else {
        color = getShieldColour(empireId);
      }

      gradientColors[0] = color;
      gradientColors[1] = color & 0x00ffffff;
      RadialGradient gradient = new RadialGradient(
        x, y, CIRCLE_RADIUS, gradientColors, gradientStops, Shader.TileMode.CLAMP);
      paint.setShader(gradient);
      canvas.drawCircle(x, y, radius, paint);
    }
  }

  private static int getShieldColour(Integer empireId) {
    Empire empire = EmpireManager.i.getEmpire(empireId);
    if (empire == null) {
      return 0;
    }
    return EmpireShieldManager.i.getShieldColour(empire);
  }

  private static Integer getEmpireId(BaseStar star) {
    // if it's a wormhole, the empire is the owner of the wormhole
    BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
    if (wormholeExtra != null) {
      return wormholeExtra.getEmpireID();
    }

    // otherwise, pick the first colony we find to represent the star
    // TODO: what if the star is colonized by more than one empire?
    for (BaseColony colony : star.getColonies()) {
      if (colony.getEmpireKey() != null) {
        return Integer.parseInt(colony.getEmpireKey());
      }
    }

    return null;
  }

  /**
   * If we can't find an empire by colony, look for a fleet. If there's one, we'll display a circle
   * that a little dimmed.
   */
  private static Integer getFleetOnlyEmpire(BaseStar star) {
    // TODO: what if there is more than one fleet? Should try to mix the colors?
    for (BaseFleet fleet : star.getFleets()) {
      if (fleet.getEmpireKey() != null) {
        return Integer.parseInt(fleet.getEmpireKey());
      }
    }

    return null;
  }
}