package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;

import androidx.collection.LongSparseArray;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.opengl.Camera;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.client.opengl.Sprite;
import au.com.codeka.warworlds.client.opengl.SpriteTemplate;
import au.com.codeka.warworlds.client.opengl.TextSceneObject;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.Vector3;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;
import au.com.codeka.warworlds.common.sim.StarHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link StarfieldManager} manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that {@link StarfieldScreen} cares about) and the
 * move-fleet view, etc.
 */
public class StarfieldManager {
  public interface TapListener {
    void onStarTapped(@Nullable Star star);
    void onFleetTapped(@Nullable Star star, @Nullable Fleet fleet);
  }

  /** Number of milliseconds between updates to moving fleets. */
  private static final long UPDATE_MOVING_FLEETS_TIME_MS = 2000L;

  private static final Log log = new Log("StarfieldManager");
  private final Scene scene;
  private final Camera camera;
  private final StarfieldGestureDetector gestureDetector;
  private final Context context;
  private final ArrayList<TapListener> tapListeners = new ArrayList<>();
  private boolean initialized;

  // The selected star/fleet. If selectedFleet is non-null, selectedStar will be the star that
  // fleet is on.
  @Nullable private Star selectedStar;
  @Nullable private Fleet selectedFleet;

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

  /** A mapping of IDs to {@link SceneObject} representing stars and fleets. */
  private final LongSparseArray<SceneObject> sceneObjects = new LongSparseArray<>();

  /** A mapping of sector x,y coordinates to the list of {@link SceneObject}s for that sector. */
  private final Map<Pair<Long,Long>, ArrayList<SceneObject>> sectorSceneObjects = new HashMap<>();

  /** A mapping of sector x,y coordinates to the {@link BackgroundSceneObject}s for that sector. */
  private final Map<Pair<Long,Long>, BackgroundSceneObject> backgroundSceneObjects = new HashMap<>();

  /**
   * The "selection indicator" which is added to a star or fleet scene object to indicate the
   * current selection. It's never null, but it might not be attached to anything.
   */
  private final SelectionIndicatorSceneObject selectionIndicatorSceneObject;

  public StarfieldManager(RenderSurfaceView renderSurfaceView) {
    this.scene = renderSurfaceView.createScene();
    this.camera = renderSurfaceView.getCamera();
    this.context = renderSurfaceView.getContext();
    gestureDetector = new StarfieldGestureDetector(renderSurfaceView, gestureListener);
    renderSurfaceView.setScene(scene);
    selectionIndicatorSceneObject = new SelectionIndicatorSceneObject(scene.getDimensionResolver());
  }

  public void create() {
    App.i.getEventBus().register(eventListener);
    if (App.i.getServer().getCurrState().getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      // If we're already connected, then call onConnected now.
      onConnected();
    }

    camera.setCameraUpdateListener(cameraUpdateListener);
    gestureDetector.create();

    App.i.getTaskRunner().runTask(
        updateMovingFleetsRunnable,
        Threads.UI,
        UPDATE_MOVING_FLEETS_TIME_MS);
  }

  public void destroy() {
    gestureDetector.destroy();
    camera.setCameraUpdateListener(null);

    App.i.getEventBus().unregister(eventListener);
  }

  public void addTapListener(@Nonnull TapListener tapListener) {
    tapListeners.add(tapListener);
  }

  public void removeTapListener(@Nonnull TapListener tapListener) {
    tapListeners.remove(tapListener);
  }

  /** Gets the selected star (or null if no star is selected). */
  @Nullable
  public Star getSelectedStar() {
    if (selectedFleet != null) {
      // If we have a fleet selected, the selectedStar will be the star for that fleet but from
      // the user's point of view, there's no actual star selected...
      return null;
    }
    return selectedStar;
  }

  /** Gets the selected fleet (or null if not fleet is selected). */
  @Nullable
  public Fleet getSelectedFleet() {
    return selectedFleet;
  }

  /**
   * Gets the {@link SceneObject} that's being used to render the star with the given ID, or if
   * the given star isn't being rendered by us, returns null.
   */
  @Nullable
  public SceneObject getStarSceneObject(long starId) {
    return sceneObjects.get(starId);
  }

  /** Sets the star we have selected to the given value (or unselects if star is null). */
  public void setSelectedStar(@Nullable Star star) {
    if (star != null) {
      selectionIndicatorSceneObject.setSize(60, 60);
      SceneObject sceneObject = sceneObjects.get(star.id);
      sceneObject.addChild(selectionIndicatorSceneObject);
    } else if (selectionIndicatorSceneObject.getParent() != null) {
      selectionIndicatorSceneObject.getParent().removeChild(selectionIndicatorSceneObject);
    }

    selectedFleet = null;
    selectedStar = star;
    for (TapListener tapListener : tapListeners) {
      tapListener.onStarTapped(star);
    }
  }

  /**
   * Sets the fleet we have selected to the given value (or unselects it if fleet is null).
   *
   * @param star The star the fleet is coming from.
   * @param fleet The fleet.
   */
  public void setSelectedFleet(@Nullable Star star, @Nullable Fleet fleet) {
    Threads.checkOnThread(Threads.UI);

    log.debug("setSelectedFleet(%d %s, %d %sx%.1f)",
        star == null ? 0 : star.id,
        star == null ? "?" : star.name,
        fleet == null ? 0 : fleet.id,
        fleet == null ? "?" : fleet.design_type,
        fleet == null ? 0 : fleet.num_ships);
    if (fleet != null) {
      selectionIndicatorSceneObject.setSize(60, 60);
      SceneObject sceneObject = sceneObjects.get(fleet.id);
      if (sceneObject != null) {
        sceneObject.addChild(selectionIndicatorSceneObject);
      } else {
        log.warning("No SceneObject for fleet #%d", fleet.id);
      }
    } else if (selectionIndicatorSceneObject.getParent() != null) {
      selectionIndicatorSceneObject.getParent().removeChild(selectionIndicatorSceneObject);
    }

    selectedStar = star;
    selectedFleet = fleet;
    for (TapListener tapListener : tapListeners) {
      tapListener.onFleetTapped(star, fleet);
    }
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

    log.info("WarpTo: %.2f %.2f", offsetX, offsetY);
    camera.warpTo(
        scene.getDimensionResolver().dp2px(-offsetX),
        scene.getDimensionResolver().dp2px(offsetY));
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
    Empire myEmpire = checkNotNull(EmpireManager.i.getMyEmpire());
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
    App.i.getTaskRunner().runTask(() -> {
      App.i.getServer().send(new Packet.Builder()
          .watch_sectors(new WatchSectorsPacket.Builder()
              .top(top).left(left).bottom(bottom).right(right).build())
          .build());
    }, Threads.BACKGROUND);

    sectorTop = top;
    sectorLeft = left;
    sectorBottom = bottom;
    sectorRight = right;
  }

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

  /** Create a {@link Sprite} to represent the given {@link Star}. */
  public Sprite createStarSprite(Star star) {
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
    return sprite;
  }

  /** Create a {@link Sprite} to represent the given {@link Fleet}. */
  public Sprite createFleetSprite(Fleet fleet) {
    Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
        .shader(scene.getSpriteShader())
        .texture(scene.getTextureManager().loadTexture(getFleetTexture(fleet)))
        .build());
    sprite.setSize(64.0f, 64.0f);
    return sprite;
  }

  /** Called when a star is updated, we may need to update the sprite for it. */
  private void updateStar(Star star) {
    Star oldStar = null;
    SceneObject container = sceneObjects.get(star.id);
    if (container == null) {
      container = new SceneObject(scene.getDimensionResolver());
      container.setClipRadius(80.0f);
      container.setTapTargetRadius(80.0f);
      addSectorSceneObject(Pair.create(star.sector_x, star.sector_y), container);

      float x = (star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f);
      float y = (star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f);
      container.translate(x, -y);
    } else {
      oldStar = ((SceneObjectInfo) container.getTag()).star;

      // Temporarily remove the container, and clear out it's children. We'll re-add them all.
      synchronized (scene.lock) {
        if (container.getParent() != null) {
          scene.getRootObject().removeChild(container);
        }
      }
      container.removeAllChildren();
    }

    // Be sure to update the container's tag with the new star info.
    container.setTag(new SceneObjectInfo(star));

    Sprite sprite = createStarSprite(star);
    container.addChild(sprite);

    TextSceneObject text = scene.createText(star.name);
    text.translate(0.0f, -24.0f);
    text.setTextSize(16);
    text.translate(-text.getTextWidth() / 2.0f, 0.0f);
    container.addChild(text);

    attachEmpireFleetIcons(container, star);

    synchronized (scene.lock) {
      scene.getRootObject().addChild(container);
      sceneObjects.put(star.id, container);
    }

    detachNonMovingFleets(oldStar, star);
    attachMovingFleets(star);
  }

  /** Attach the empire labels and fleet counts to the given sprite container for the given star. */
  private void attachEmpireFleetIcons(SceneObject container, Star star) {
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
        // Ignore native fleets, and moving fleets, which we'll draw them separately.
        continue;
      }

      Empire empire = EmpireManager.i.getEmpire(fleet.empire_id);
      if (empire != null) {
        EmpireIconInfo iconInfo = empires.get(empire.id);
        if (iconInfo == null) {
          iconInfo = new EmpireIconInfo(empire);
          empires.put(empire.id, iconInfo);
        }
        if (fleet.design_type.equals(Design.DesignType.FIGHTER)) {
          iconInfo.numFighterShips += (int) Math.ceil(fleet.num_ships);
        } else {
          iconInfo.numNonFighterShips += (int) Math.ceil(fleet.num_ships);
        }
      }
    }

    int i = 0;
    for (Map.Entry<Long, EmpireIconInfo> entry : empires.entrySet()) {
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

  /** Attach moving fleets to the given sprite container for the given star. */
  private void attachMovingFleets(Star star) {
    for (Fleet fleet : star.fleets) {
      if (fleet.state == Fleet.FLEET_STATE.MOVING) {
        attachMovingFleet(star, fleet);
      }
    }
  }

  private void attachMovingFleet(Star star, Fleet fleet) {
    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (destStar == null) {
      log.warning("Cannot attach moving fleet, destination star is null.");
      return;
    }

    SceneObject container = sceneObjects.get(fleet.id);
    if (container == null) {
      container = new SceneObject(scene.getDimensionResolver());
      container.setClipRadius(80.0f);
      container.setTapTargetRadius(80.0f);
      container.setTag(new SceneObjectInfo(star, fleet, destStar));
      addSectorSceneObject(Pair.create(star.sector_x, star.sector_y), container);

      Vector2 pos = getMovingFleetPosition(star, destStar, fleet);
      container.translate((float) pos.x, (float) -pos.y);
    } else {
      // Temporarily remove the container, and clear out it's children. We'll re-add them all.
      synchronized (scene.lock) {
        scene.getRootObject().removeChild(container);
      }
      container.removeAllChildren();
    }

    Sprite sprite = createFleetSprite(fleet);
    container.addChild(sprite);

    // Rotate the sprite
    Vector2 dir = StarHelper.directionBetween(star, destStar);
    dir.normalize();
    float angle = Vector2.angleBetween(dir, new Vector2(0, -1));
    sprite.setRotation(angle, 0, 0, 1);

    synchronized (scene.lock) {
      scene.getRootObject().addChild(container);
      sceneObjects.put(fleet.id, container);
    }
  }

  /** Detach any non-moving fleets that may have been moving previously. */
  private void detachNonMovingFleets(@Nullable Star oldStar, Star star) {
    // Remove any fleets that are no longer moving.
    for (Fleet fleet : star.fleets) {
      if (fleet.state != Fleet.FLEET_STATE.MOVING) {
        SceneObject sceneObject = sceneObjects.get(fleet.id);
        if (sceneObject != null) {
          detachNonMovingFleet(fleet, sceneObject);
        }
      }
    }

    // Make sure to also do the same for fleets that are no longer on the star.
    if (oldStar != null) {
      for (Fleet oldFleet : oldStar.fleets) {
        SceneObject sceneObject = sceneObjects.get(oldFleet.id);
        if (sceneObject == null) {
          // no need to see if we need to remove it if it doesn't exist...
          continue;
        }
        boolean removed = true;
        for (Fleet fleet : star.fleets) {
          if (fleet.id.equals(oldFleet.id)) {
            removed = false;
            break;
          }
        }
        if (removed) {
          detachNonMovingFleet(oldFleet, sceneObject);
        }
      }
    }
  }

  private void detachNonMovingFleet(Fleet fleet, SceneObject sceneObject) {
    // If you had it selected, we'll need to un-select it.
    if (selectedFleet != null && selectedFleet.id.equals(fleet.id)) {
      App.i.getTaskRunner().runTask(() -> setSelectedFleet(null, null), Threads.UI);
    }

    synchronized (scene.lock) {
      sceneObject.getParent().removeChild(sceneObject);
      sceneObjects.remove(fleet.id);
    }
  }

  /** Get the current position of the given moving fleet. */
  private Vector2 getMovingFleetPosition(Star star, Star destStar, Fleet fleet) {
    Vector2 src = new Vector2(
        (star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f),
        (star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f));
    Vector2 dest = new Vector2(
        (destStar.sector_x - centerSectorX) * 1024.0f + (destStar.offset_x - 512.0f),
        (destStar.sector_y - centerSectorY) * 1024.0f + (destStar.offset_y - 512.0f));

    long totalTime = fleet.eta - fleet.state_start_time;
    long elapsedTime = System.currentTimeMillis() - fleet.state_start_time;
    double timeFraction = (float) elapsedTime / (float) totalTime;

    // Subtract 100, we'll add 50 after because we want the fleet to start offset from the star and
    // finish offset as well.
    double distance = src.distanceTo(dest) - 100.0;
    distance *= timeFraction;
    distance += 50;
    if (distance < 50.0) {
      distance = 50.0;
    }

    dest.subtract(src);
    dest.normalize();
    dest.scale(distance);
    dest.add(src);
    return dest;
  }

  private void createSectorBackground(long sectorX, long sectorY) {
    BackgroundSceneObject backgroundSceneObject =
        new BackgroundSceneObject(scene, sectorX, sectorY);
    backgroundSceneObject.setZoomAmount(camera.getZoomAmount());
    backgroundSceneObject.translate(
        -(centerSectorX - sectorX) * 1024.0f,
        (centerSectorY - sectorY) * 1024.0f);

    Pair<Long, Long> xy = Pair.create(sectorX, sectorY);
    addSectorSceneObject(xy, backgroundSceneObject);
    synchronized (backgroundSceneObjects) {
      backgroundSceneObjects.put(xy, backgroundSceneObject);
    }
    synchronized (scene.lock) {
      scene.getRootObject().addChild(backgroundSceneObject);
    }
  }

  /**
   * Goes through all of the moving fleets and updates their position. This is called every now and
   * then to make sure fleets are moving.
   */
  private final Runnable updateMovingFleetsRunnable = new Runnable() {
    @Override
    public void run() {
      synchronized (scene.lock) {
        for (int i = 0; i < sceneObjects.size(); i++) {
          SceneObjectInfo sceneObjectInfo = (SceneObjectInfo) sceneObjects.valueAt(i).getTag();
          if (sceneObjectInfo != null && sceneObjectInfo.fleet != null) {
            updateMovingFleet(sceneObjects.valueAt(i));
          }
        }
      }

      App.i.getTaskRunner().runTask(
          updateMovingFleetsRunnable,
          Threads.UI,
          UPDATE_MOVING_FLEETS_TIME_MS);
    }
  };

  /**
   * Update the given {@link SceneObject} that represents a moving fleet. Should already be in the
   * scene.lock.
   */
  private void updateMovingFleet(SceneObject fleetSceneObject) {
    SceneObjectInfo sceneObjectInfo = (SceneObjectInfo) fleetSceneObject.getTag();
    Vector2 pos = getMovingFleetPosition(
        sceneObjectInfo.star, sceneObjectInfo.destStar, sceneObjectInfo.fleet);
    fleetSceneObject.setTranslation((float) pos.x, (float) -pos.y);
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
        SceneObjectInfo sceneObjectInfo = (SceneObjectInfo) obj.getTag();
        if (sceneObjectInfo != null) {
          if (sceneObjectInfo.fleet != null) {
            sceneObjects.remove(sceneObjectInfo.fleet.id);
          } else {
            sceneObjects.remove(sceneObjectInfo.star.id);
          }
        }
        scene.getRootObject().removeChild(obj);
      }
    }

    synchronized (backgroundSceneObjects) {
      backgroundSceneObjects.remove(sectorCoord);
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

  private String getFleetTexture(Fleet fleet) {
    switch (fleet.design_type) {
      case COLONY_SHIP:
        return "sprites/colony.png";
      case SCOUT:
        return "sprites/scout.png";
      case FIGHTER:
        return "sprites/fighter.png";
      case TROOP_CARRIER:
        return "sprites/troopcarrier.png";
      case WORMHOLE_GENERATOR:
        return "sprites/wormhole-generator.png";
      case UNKNOWN_DESIGN:
      default:
        // Shouldn't happen, the rest are reserved for buildings.
        return "sprites/hq.png";
    }
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onServerStateEvent(ServerStateEvent event) {
      if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
        onConnected();
      }
    }

    @EventHandler(thread = Threads.BACKGROUND)
    public void onStar(Star star) {
      // Make sure this star is one that we're tracking.
      if (star.sector_x < sectorLeft || star.sector_x > sectorRight
          || star.sector_y < sectorTop || star.sector_y > sectorBottom) {
        return;
      }

      updateStar(star);

      if (selectedFleet != null && selectedStar != null && selectedStar.id.equals(star.id)) {
        // We have a fleet selected and it's one on this star. Make sure we update the selected
        // fleet as well.
        boolean found = false;
        for (Fleet fleet : star.fleets) {
          if (fleet.id.equals(selectedFleet.id)) {
            selectedFleet = fleet;
            selectedStar = star;
            found = true;
          }
        }
        if (!found) {
          // The fleet's been removed from star, it's no longer selected!
          setSelectedFleet(null, null);
        }
      } else if (selectedStar != null && selectedStar.id.equals(star.id)) {
        // The star that we have selected has been updated.
        selectedStar = star;
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
      synchronized (backgroundSceneObjects) {
        for (BackgroundSceneObject backgroundSceneObject : backgroundSceneObjects.values()) {
          backgroundSceneObject.setZoomAmount(camera.getZoomAmount());
        }
      }
    }

    @Override
    public void onTap(float x, float y) {
      SceneObjectInfo selected = null;

      // Work out which object (if any) you tapped on.
      synchronized (scene.lock) {
        SceneObject selectedSceneObject = null;
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
            selectedSceneObject = so;
          }
        }

        if (selectedSceneObject != null) {
          selected = (SceneObjectInfo) selectedSceneObject.getTag();
        }
      }

      if (selectedFleet != null && (selected == null || selected.fleet == null)) {
        setSelectedFleet(null, null);
      } else if (selected != null && selected.fleet != null) {
        setSelectedFleet(selected.star, selected.fleet);
      } else if (selectedStar != null && selected == null) {
        setSelectedStar(null);
      } else if (selected != null) {
        setSelectedStar(selected.star);
      }
    }
  };

  private final Camera.CameraUpdateListener cameraUpdateListener
      = (x, y, dx, dy) -> App.i.getTaskRunner().runTask(
          () -> StarfieldManager.this.onCameraTranslate(x, y),
          Threads.BACKGROUND);

  private static final class EmpireIconInfo {
    final Empire empire;
    int numColonies;
    int numFighterShips;
    int numNonFighterShips;

    EmpireIconInfo(Empire empire) {
      this.empire = empire;
    }
  }

  private static final class SceneObjectInfo {
    final Star star;
    @Nullable final Fleet fleet;
    @Nullable final Star destStar;

    SceneObjectInfo(Star star) {
      this.star = star;
      this.fleet = null;
      this.destStar = null;
    }

    SceneObjectInfo(Star star, Fleet fleet, Star destStar) {
      this.star = star;
      this.fleet = fleet;
      this.destStar = destStar;
    }
  }
}
