package au.com.codeka.warworlds.client.starfield;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.opengl.Camera;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.client.opengl.Sprite;
import au.com.codeka.warworlds.client.opengl.SpriteTemplate;
import au.com.codeka.warworlds.client.opengl.TextSceneObject;
import au.com.codeka.warworlds.client.opengl.Vector2;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;

/**
 * {@link StarfieldManager} manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that {@link StarfieldFragment} cares about) and the
 * move-fleet view, etc.
 */
public class StarfieldManager {
  private static final Log log = new Log("StarfieldManager");
  private final Scene scene;
  private final Camera camera;
  private final StarfieldGestureDetector gestureDetector;
  private boolean initialized;

  private long centerSectorX;
  private long centerSectorY;
  private int sectorRadius;

  private final Map<Long, SceneObject> starSceneObjects = new HashMap<>();

  public StarfieldManager(RenderSurfaceView renderSurfaceView) {
    this.scene = renderSurfaceView.createScene();
    this.camera = renderSurfaceView.getCamera();
    gestureDetector = new StarfieldGestureDetector(renderSurfaceView, gestureListener);
    renderSurfaceView.setScene(scene);
  }

  public void create() {
    App.i.getEventBus().register(eventListener);
    if (App.i.getServer().getCurrState().getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      // If we're already connected, then call onConnected now.
      onConnected();
    }

    gestureDetector.create();
  }

  public void destroy() {
    gestureDetector.destroy();

    App.i.getEventBus().unregister(eventListener);
  }

  public void warpTo(Star star) {
    warpTo(star.sector_x, star.sector_y, star.offset_x - 512.0f,star.offset_y - 512.0f);
  }

  public void warpTo(long sectorX, long sectorY, float offsetX, float offsetY) {
    centerSectorX = sectorX;
    centerSectorY = sectorY;
    sectorRadius = 1;

    // Now tell the server we want to watch these new sectors, it'll send us back all the stars we
    // don't have yet.
    App.i.getServer().send(new Packet.Builder()
        .watch_sectors(new WatchSectorsPacket.Builder()
            .top(centerSectorY - sectorRadius)
            .left(centerSectorX - sectorRadius)
            .bottom(centerSectorY + sectorRadius)
            .right(centerSectorX + sectorRadius)
            .build())
        .build());
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

  /** Called when a star is updated, we may need to update the sprite for it. */
  private void updateStar(Star star) {
    SceneObject container = starSceneObjects.get(star.id);
    if (container == null) {
      container = new SceneObject(scene.getDimensionResolver());

      float x = (star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f);
      float y = (star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f);
      container.translate(x, y);

      Vector2 uvTopLeft = getStarUvTopLeft(star);
      Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
          .shader(scene.getSpriteShader())
          .texture(scene.getTextureManager().loadTexture("stars/stars_small.png"))
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
      text.setTextSize(16);
      text.translate(-text.getTextWidth() / 2.0f, -15.0f);
      container.addChild(text);

      synchronized (scene.lock) {
        scene.getRootObject().addChild(container);
      }
      starSceneObjects.put(star.id, container);
    }
    // TODO: update the sprite with label, kind, etc...
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
        if (star.sector_x < centerSectorX - sectorRadius
            || star.sector_x > centerSectorX + sectorRadius
            || star.sector_y < centerSectorY - sectorRadius
            || star.sector_y > centerSectorY + sectorRadius) {
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
  };
}
