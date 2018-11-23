package au.com.codeka.warworlds.client.game.starfield;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.google.common.base.Preconditions;

import java.util.Random;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.opengl.Texture;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A {@link Texture} that returns a bitmap to use as a texture for the "tactical" SceneObject that
 * covers a sector.
 */
public class TacticalTexture extends Texture {
  // The size of the texture, in pixels, that we'll generate.
  private static final int TEXTURE_SIZE = 128;

  // The radius of the circle, in pixels, that we'll put around each empire.
  private static final int CIRCLE_RADIUS = 20;

  @Nullable private Bitmap bitmap;

  public static TacticalTexture create(Sector sector) {
    return new TacticalTexture(sector);
  }

  private TacticalTexture(Sector sector) {
    App.i.getTaskRunner().runTask(() -> {
      bitmap = createBitmap(sector);
    }, Threads.BACKGROUND);
  }

  @Override
  public void bind() {
    if (bitmap != null) {
      setTextureId(createGlTexture());
    }

    super.bind();
  }

  private int createGlTexture() {
    Preconditions.checkState(bitmap != null);

    final int[] textureHandleBuffer = new int[1];
    GLES20.glGenTextures(1, textureHandleBuffer, 0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0]);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

    return textureHandleBuffer[0];
  }

  private static Bitmap createBitmap(Sector sector) {
    Bitmap bmp = Bitmap.createBitmap(TEXTURE_SIZE, TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
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
          s = null; // TODO SectorManager.i.getSector(sector.getX() + offsetX, sector.getY() + offsetY);
        }
        if (s == null) {
          continue;
        }

        drawCircles(s, offsetX, offsetY, canvas, paint);
      }
    }

    return bmp;
  }

  private static void drawCircles(Sector sector, int offsetX, int offsetY, Canvas canvas, Paint paint) {
    float scaleFactor = (float) TEXTURE_SIZE / 1024.f;

    for (Star star : sector.stars) {
      float x = (star.offset_x + offsetX * 1024.0f) * scaleFactor;
      float y = (star.offset_y + offsetY * 1024.0f) * scaleFactor;
      float radius = CIRCLE_RADIUS;

      if (x < -radius || x > TEXTURE_SIZE + radius || y < -radius || y > TEXTURE_SIZE + radius) {
        // if it's completely off the bitmap, skip drawing it
        continue;
      }

      int color = 0;
      Long empireId = getEmpireId(star);
      if (empireId == null) {
        empireId = getFleetOnlyEmpire(star);
        if (empireId != null) {
          color = getShieldColour(empireId);
          color = 0x66ffffff & color;
        }
      } else {
        color = getShieldColour(empireId);
      }

      paint.setColor(color);
      canvas.drawCircle(x, y, radius, paint);
    }
  }

  // TODO: this logic is basicaly cut'n'paste from EmpireRendererHandler on the server.
  private static int getShieldColour(Long empireId) {
    if (empireId == null) {
      return 0;
    }

    Random rand = new Random(empireId ^ 7438274364563846L);
    return 0xff000000 |
        (rand.nextInt(100) + 100) << 16 |
        (rand.nextInt(100) + 100) << 8 |
        (rand.nextInt(100) + 100);
  }

  private static Long getEmpireId(Star star) {
    // if it's a wormhole, the empire is the owner of the wormhole
//    BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
//    if (wormholeExtra != null) {
//      return EmpireManager.i.getEmpire(wormholeExtra.getEmpireID());
//    }

    // otherwise, pick the first colony we find to represent the star
    // TODO: what if the star is colonized by more than one empire?
    for (Planet planet : star.planets) {
      if (planet.colony != null) {
        return planet.colony.empire_id;
      }
    }

    return null;
  }

  /**
   * If we can't find an empire by colony, look for a fleet. If there's one, we'll display a circle
   * that a little dimmed.
   */
  private static Long getFleetOnlyEmpire(Star star) {
    // TODO: what if there is more than one fleet? Should try to mix the colors?
    for (Fleet fleet : star.fleets) {
      return fleet.empire_id;
    }

    return null;
  }
}
