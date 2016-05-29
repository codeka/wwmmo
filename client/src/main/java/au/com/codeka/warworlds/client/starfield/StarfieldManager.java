package au.com.codeka.warworlds.client.starfield;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.opengl.Camera;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.client.opengl.Sprite;
import au.com.codeka.warworlds.client.opengl.SpriteTemplate;
import au.com.codeka.warworlds.client.opengl.TextSceneObject;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.Vector3;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;

/**
 * {@link StarfieldManager} manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that {@link StarfieldFragment} cares about) and the
 * move-fleet view, etc.
 */
public class StarfieldManager {
  public interface TapListener {
    void onStarTapped(@Nullable Star star);
  }

  private static final Log log = new Log("StarfieldManager");
  private final Scene scene;
  private final Camera camera;
  private final StarfieldGestureDetector gestureDetector;
  private final Context context;
  private boolean initialized;
  @Nullable private TapListener tapListener;

  private long centerSectorX;
  private long centerSectorY;
  private int sectorRadius;

  // The following are the top/left/right/bottom of the rectangle of sectors that we're currently
  // listening to for updates. Generally this will be the same as centreSectorX,centreSectorY +
  // sectorRadius, but not always (in particular, just before you call updateSectorBounds())
  private long sectorLeft;
  private long sectorTop;
  private long sectorRight;
  private long sectorBottom;

  private final Map<Long, SceneObject> starSceneObjects = new HashMap<>();
  private final Map<Pair<Long,Long>, ArrayList<SceneObject>> sectorSceneObjects = new HashMap<>();

  public StarfieldManager(RenderSurfaceView renderSurfaceView) {
    this.scene = renderSurfaceView.createScene();
    this.camera = renderSurfaceView.getCamera();
    this.context = renderSurfaceView.getContext();
    gestureDetector = new StarfieldGestureDetector(renderSurfaceView, gestureListener);
    renderSurfaceView.setScene(scene);
  }

  public void create() {
    App.i.getEventBus().register(eventListener);
    if (App.i.getServer().getCurrState().getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      // If we're already connected, then call onConnected now.
      onConnected();
    }

    camera.setCameraUpdateListener(cameraUpdateListener);
    gestureDetector.create();
  }

  public void destroy() {
    gestureDetector.destroy();
    camera.setCameraUpdateListener(null);

    App.i.getEventBus().unregister(eventListener);
  }

  public void setTapListener(@Nullable TapListener tapListener) {
    this.tapListener = tapListener;
  }

  public void warpTo(Star star) {
    warpTo(star.sector_x, star.sector_y, star.offset_x - 512.0f, star.offset_y - 512.0f);
  }

  public void warpTo(long sectorX, long sectorY, float offsetX, float offsetY) {
    centerSectorX = sectorX;
    centerSectorY = sectorY;
    sectorRadius = 1;

    updateSectorBounds(centerSectorX - sectorRadius, centerSectorY - sectorRadius,
        centerSectorX + sectorRadius, centerSectorY + sectorRadius);
  }

  /**
   * This is called after we're connected to the server. If this is our first time being called,
   * we'll initialize the scene at the default viewport.
   */
  public void onConnected() {
    if (initialized) {
      return;
    }
    initialized = true;

    // Shouldn't be null after we're connected to the server.
    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    warpTo(myEmpire.home_star);
  }

  /**
   * Update the sector "bounds", that is the area of the universe that we're currently looking at.
   * We'll need to remove stars that have gone out of bounds, add a background for stars that are
   * now in-bounds, and ask the server to keep us updated of the new stars.
   */
  private void updateSectorBounds(long left, long top, long right, long bottom) {
    // Remove the objects that are now out of bounds.
    for (long sy = sectorTop; sy <= sectorBottom; sy++) {
      for (long sx = sectorLeft; sx <= sectorRight; sx++) {
        if (sy < top || sy > bottom || sx < left || sx > right) {
          removeSector(Pair.create(sx, sy));
        }
      }
    }

    // Create the background for all of the new sectors.
    for (long sy = top; sy <= bottom; sy ++) {
      for (long sx = left; sx <= right; sx ++) {
        if (sy < sectorTop || sy > sectorBottom || sx < sectorLeft || sx > sectorRight) {
          createSectorBackground(sx, sy);
        }
      }
    }

    // Tell the server we want to watch these new sectors, it'll send us back all the stars we
    // don't have yet.
    if (first == true) {
      App.i.getServer().send(new Packet.Builder()
          .watch_sectors(new WatchSectorsPacket.Builder()
              .top(top).left(left).bottom(bottom).right(right).build())
          .build());
      first = false;
    }

    sectorTop = top;
    sectorLeft = left;
    sectorBottom = bottom;
    sectorRight = right;
  }
boolean first = true;
  /**
   * This is called when the camera moves to a new (x,y) coord. We'll want to check whether we
   * need to re-caculate the bounds and warp the camera back to the center.
   *
   * @param x The distance the camera has translated from the origin in the X direction.
   * @param y The distance the camera has translated from the origin in the Y direction.
   */
  private void onCameraTranslate(float x, float y) {
    long newCentreX = centerSectorX + Math.round(scene.getDimensionResolver().px2dp(x / 1024.0f));
    long newCentreY = centerSectorY + Math.round(scene.getDimensionResolver().px2dp(y / 1024.0f));
    long top = newCentreY - sectorRadius;
    long left = newCentreX - sectorRadius;
    long bottom = newCentreY + sectorRadius;
    long right = newCentreX + sectorRadius;
    if (top != sectorTop || left != sectorLeft || bottom != sectorBottom || right != sectorRight) {
      log.info("onCameraTranslate: bounds=%d,%d - %d,%d", left, top, right, bottom);
      updateSectorBounds(left, top, right, bottom);
    }
  }

  /** Called when a star is updated, we may need to update the sprite for it. */
  private void updateStar(Star star) {
    SceneObject container = starSceneObjects.get(star.id);
    if (container == null) {
      container = new SceneObject(scene.getDimensionResolver());
      container.setClipRadius(80.0f);
      container.setTapTargetRadius(80.0f);
      container.setTag(star);
      addSectorSceneObject(Pair.create(star.sector_x, star.sector_y), container);

      float x = (star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f);
      float y = (star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f);
      container.translate(x, y);

      Vector2 uvTopLeft = getStarUvTopLeft(star);
      Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
          .shader(scene.getSpriteShader())
          .texture(scene.getTextureManager().loadTexture("stars/stars.png"))
          .uvTopLeft(uvTopLeft)
          .uvBottomRight(new Vector2(
              uvTopLeft.x + (star.classification == Star.CLASSIFICATION.NEUTRON ? 0.5f : 0.25f),
              uvTopLeft.y + (star.classification == Star.CLASSIFICATION.NEUTRON ? 0.5f : 0.25f)))
          .build());
      if (star.classification == Star.CLASSIFICATION.NEUTRON) {
        sprite.setSize(90.0f, 90.0f);
      } else {
        sprite.setSize(40.0f, 40.0f);
      }
      container.addChild(sprite);

      TextSceneObject text = scene.createText(star.name);
      text.translate(0.0f, -24.0f);
      text.setTextSize(16);
      text.translate(-text.getTextWidth() / 2.0f, 0.0f);
      container.addChild(text);

      attachEmpireFleetIcons(scene, container, star);

      synchronized (scene.lock) {
        scene.getRootObject().addChild(container);
      }
      starSceneObjects.put(star.id, container);
    }
  }

  /** Attach the empire labels and fleet counts to the given sprite container for the given star. */
  private void attachEmpireFleetIcons(Scene scene, SceneObject container, Star star) {
    Map<Long, EmpireIconInfo> empires = new TreeMap<>();
    for (Planet planet : star.planets) {
      if (planet.colony == null || planet.colony.empire_id == null) {
        continue;
      }

      Empire empire = EmpireManager.i.getEmpire(planet.colony.empire_id);
      if (empire != null) {
        EmpireIconInfo iconInfo = empires.get(empire.id);
        if (iconInfo == null) {
          iconInfo = new EmpireIconInfo(empire);
          empires.put(empire.id, iconInfo);
        }
        iconInfo.numColonies += 1;
      }
    }

    for (Fleet fleet : star.fleets) {
      if (fleet.empire_id == null || fleet.state == Fleet.FLEET_STATE.MOVING) {
        // Ignore native feets, and moving fleets, which we'll draw them separately.
        continue;
      }

      Empire empire = EmpireManager.i.getEmpire(fleet.empire_id);
      if (empire != null) {
        EmpireIconInfo iconInfo = empires.get(empire.id);
        if (iconInfo == null) {
          iconInfo = new EmpireIconInfo(empire);
          empires.put(empire.id, iconInfo);
        }
        if (fleet.design_id.equals("fighter")) {
          iconInfo.numFighterShips += (int) Math.ceil(fleet.num_ships);
        } else {
          iconInfo.numNonFighterShips += (int) Math.ceil(fleet.num_ships);
        }
      }
    }

    int i = 0;
    for (Map.Entry<Long, EmpireIconInfo> entry : empires.entrySet()) {
      long empireID = entry.getKey();
      EmpireIconInfo iconInfo = entry.getValue();

      Vector2 pt = new Vector2(0, 30.0f);
      pt.rotate(-(float) (Math.PI / 4.0) * (i + 1));

      // Add the empire's icon
      Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
          .shader(scene.getSpriteShader())
          .texture(scene.getTextureManager().loadTextureUrl(
              ImageHelper.getEmpireImageUrlExactDimens(context, iconInfo.empire, 64, 64)))
          .build());
      sprite.translate((float) pt.x + 10.0f, (float) pt.y);
      sprite.setSize(20.0f, 20.0f);
      container.addChild(sprite);

      // Add the counts.
      String text;
      if (iconInfo.numFighterShips == 0 && iconInfo.numNonFighterShips == 0) {
        text = String.format(Locale.ENGLISH, "%d", iconInfo.numColonies);
      } else if (iconInfo.numColonies == 0) {
        text = String.format(Locale.ENGLISH, "[%d, %d]",
            iconInfo.numFighterShips, iconInfo.numNonFighterShips);
      } else {
        text = String.format(Locale.ENGLISH, "%d ● [%d, %d]",
            iconInfo.numColonies, iconInfo.numFighterShips, iconInfo.numNonFighterShips);
      }
      TextSceneObject empireCounts = scene.createText(text);
      float offset = ((empireCounts.getTextWidth() * 0.666f) / 2.0f) + 14.0f;
      empireCounts.translate((float) pt.x + offset, (float) pt.y);
      container.addChild(empireCounts);

      i++;
    }
  }

  private static final class EmpireIconInfo {
    public final Empire empire;
    public int numColonies;
    public int numFighterShips;
    public int numNonFighterShips;

    public EmpireIconInfo(Empire empire) {
      this.empire = empire;
    }
  }

  private void createSectorBackground(long sectorX, long sectorY) {
    SceneObject container = new SceneObject(scene.getDimensionResolver());
    Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
        .shader(scene.getSpriteShader())
        .texture(scene.getTextureManager().loadTexture("stars/starfield.png"))
        .build());
    sprite.setSize(1024.0f, 1024.0f);
    container.addChild(sprite);

    Random rand = new Random(sectorX ^ sectorY * 41378L + 728247L);
    for (int i = 0; i < 20; i++) {
      int x = rand.nextInt(4);
      int y = rand.nextInt(4);
      sprite = scene.createSprite(new SpriteTemplate.Builder()
          .shader(scene.getSpriteShader())
          .texture(scene.getTextureManager().loadTexture("stars/gas.png"))
          .uvTopLeft(new Vector2(0.25f * x, 0.25f * y))
          .uvBottomRight(new Vector2(0.25f * x + 0.25f, 0.25f * y + 0.25f))
          .build());
      sprite.translate((rand.nextFloat() - 0.5f) * 1024.0f, (rand.nextFloat() - 0.5f) * 1024.0f);
      float size = 300.0f + rand.nextFloat() * 200.0f;
      sprite.setSize(size, size);
      container.addChild(sprite);
    }

    container.translate((centerSectorX - sectorX) * 1024.0f, (centerSectorY - sectorY) * 1024.0f);
    addSectorSceneObject(Pair.create(sectorX, sectorY), container);
    synchronized (scene.lock) {
      scene.getRootObject().addChild(container);
    }
  }

  private void addSectorSceneObject(Pair<Long, Long> sectorCoord, SceneObject obj) {
    synchronized (sectorSceneObjects) {
      ArrayList<SceneObject> objects = sectorSceneObjects.get(sectorCoord);
      if (objects == null) {
        objects = new ArrayList<>();
        sectorSceneObjects.put(sectorCoord, objects);
      }
      objects.add(obj);
    }
  }

  /** Remove all objects in the given sector from the scene. */
  private void removeSector(Pair<Long, Long> sectorCoord) {
    ArrayList<SceneObject> objects;
    synchronized (sectorSceneObjects) {
      objects = sectorSceneObjects.remove(sectorCoord);
    }
    if (objects == null) {
      return;
    }

    synchronized (scene.lock) {
      for (SceneObject obj : objects) {
        scene.getRootObject().removeChild(obj);
      }
    }
  }

  private Vector2 getStarUvTopLeft(Star star) {
    switch (star.classification) {
      case BLACKHOLE:
        return new Vector2(0.0f, 0.5f);
      case BLUE:
        return new Vector2(0.25f, 0.5f);
      case NEUTRON:
        return new Vector2(0.0f, 0.0f);
      case ORANGE:
        return new Vector2(0.0f, 0.75f);
      case RED:
        return new Vector2(0.25f, 0.75f);
      case WHITE:
        return new Vector2(0.5f, 0.75f);
      case WORMHOLE:
        return new Vector2(0.0f, 0.0f);
      case YELLOW:
        return new Vector2(0.5f, 0.5f);
      default:
        // Shouldn't happen!
        return new Vector2(0.5f, 0.0f);
    }
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onServerStateEvent(ServerStateEvent event) {
      if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
        onConnected();
      }
    }

    @EventHandler
    public void onStarUpdatedPacket(StarUpdatedPacket pkt) {
      for (Star star : pkt.stars) {
        // Make sure this star is one that we're tracking.
        if (star.sector_x < sectorLeft || star.sector_x > sectorRight
            || star.sector_y < sectorTop || star.sector_y > sectorBottom) {
          continue;
        }

        updateStar(star);
      }
    }
  };

  private final StarfieldGestureDetector.Callback gestureListener =
      new StarfieldGestureDetector.Callback() {
    @Override
    public void onScroll(float dx, float dy) {
      camera.translate(-dx, dy);
    }

    @Override
    public void onFling(float vx, float vy) {
      camera.fling(vx, -vy);
    }

    @Override
    public void onScale(float factor) {
      camera.zoom(factor);
    }

    @Override
    public void onTap(float x, float y) {
      if (tapListener == null) {
        return;
      }
      Star star = null;

      // Work out which star (if any) you tapped on.
      synchronized (scene) {
        float[] outVec = new float[4];
        Vector3 pos = new Vector3();
        Vector3 tap = new Vector3(x, y, 0.0f);
        for (int i = 0; i < scene.getRootObject().getNumChildren(); i++) {
          SceneObject so = scene.getRootObject().getChild(i);
          if (so == null || so.getTapTargetRadius() == null) {
            continue;
          }
          so.project(camera.getViewProjMatrix(), outVec);
          pos.reset(
              (outVec[0] + 1.0f) * 0.5f * camera.getScreenWidth(),
              (-outVec[1] + 1.0f) * 0.5f * camera.getScreenHeight(),
              0.0f);
          if (Vector3.distanceBetween(pos, tap) < so.getTapTargetRadius()) {
            star = (Star) so.getTag();
          }
        }
      }

      tapListener.onStarTapped(star);
    }
  };

  private final Camera.CameraUpdateListener cameraUpdateListener
      = new Camera.CameraUpdateListener() {
    @Override
    public void onCameraTranslate(final float x, final float y, float dx, float dy) {
      App.i.getTaskRunner().runTask(new Runnable() {
        @Override
        public void run() {
          StarfieldManager.this.onCameraTranslate(x, y);
        }
      }, Threads.BACKGROUND);
    }
  };
}
