package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.andengine.engine.Engine;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.IEntity;
import org.andengine.entity.IEntityParameterCallable;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;

import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.Pair;
import au.com.codeka.common.Tuple;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;

/**
 * This is the base class for StarfieldSurfaceView and TacticalMapView, it contains the common code
 * for scrolling through sectors of stars, etc.
 */
public abstract class SectorSceneManager implements IOnSceneTouchListener {
  private static final Log log = new Log("SectorSceneManager");
  @Nullable
  private StarfieldScene scene;
  private GestureDetector gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;
  protected BaseGlActivity activity;
  protected long sectorX;
  protected long sectorY;
  protected float offsetX;
  protected float offsetY;
  private boolean wasStopped;
  private SceneCreatedHandler sceneCreatedHandler;
  private boolean needSceneRefresh;
  private boolean isSceneRefreshing;

  public SectorSceneManager(BaseGlActivity activity) {
    this.activity = activity;
    sectorX = sectorY = 0;
    offsetX = offsetY = 0;
  }

  protected void onStart() {
    SectorManager.eventBus.register(eventHandler);

    if (gestureDetector == null) {
      gestureDetector = new GestureDetector(activity, createGestureListener());

      ScaleGestureDetector.OnScaleGestureListener scaleListener = createScaleGestureListener();
      if (scaleListener != null) {
        scaleGestureDetector = new ScaleGestureDetector(activity, scaleListener);
      }
    }

    if (wasStopped) {
      log.debug("We were stopped, refreshing the scene...");
      queueRefreshScene();
    }
  }

  protected void onStop() {
    SectorManager.eventBus.unregister(eventHandler);
    wasStopped = true;
  }


  public void setSceneCreatedHandler(SceneCreatedHandler handler) {
    sceneCreatedHandler = handler;
  }

  public BaseGlActivity getActivity() {
    return activity;
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onSectorUpdated(Sector sector) {
      queueRefreshScene();
    }

    @EventHandler
    public void onSectorListUpdated(SectorManager.SectorListChangedEvent event) {
      queueRefreshScene();
    }
  };

  public void queueRefreshScene() {
    if (isSceneRefreshing) {
      // if a refresh is already running, don't do another one until the first has completed
      needSceneRefresh = true;
      return;
    }

    needSceneRefresh = false;
    isSceneRefreshing = true;
    new BackgroundRunner<Tuple<StarfieldScene, HUD>>() {
      @Override
      protected Tuple<StarfieldScene, HUD> doInBackground() {
        try {
          return createScene();
        } catch (Exception e) {
          // the most common reason for this is when the activity is destroyed before we finish...
          log.warning("Error while refreshing scene.", e);
          return null;
        }
      }

      @Override
      protected void onComplete(Tuple<StarfieldScene, HUD> tuple) {
        final Engine engine = activity.getEngine();
        final StarfieldScene scene = tuple == null ? null : tuple.one;
        final HUD hud = tuple == null ? null : tuple.two;
        if (scene != null && engine != null) {
          engine.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
              log.info("Setting scene: scene=[%d, %d] us=[%d, %d]", scene.getSectorX(),
                  scene.getSectorY(), sectorX, sectorY);
              if (sectorX != scene.getSectorX() || sectorY != scene.getSectorY()) {
                // if you've panned the map while the scene was being created, then
                // we'll have to update everything in the scene with the new
                // offsets.
                offsetChildren(scene, (scene.getSectorX() - sectorX) * Sector.SECTOR_SIZE,
                    (sectorY - scene.getSectorY()) * Sector.SECTOR_SIZE);
              }
              engine.setScene(scene);

              if (sceneCreatedHandler != null) {
                sceneCreatedHandler.onSceneCreated(scene);
              }

              StarfieldScene oldScene = SectorSceneManager.this.scene;
              SectorSceneManager.this.scene = scene;

              if (oldScene != null) {
                scene.copySelection(oldScene);
                oldScene.disposeScene();
              }

              activity.getCamera().setHUD(hud);
            }
          });
        }

        // We're done refreshing. but if we needed another one, do it now
        isSceneRefreshing = false;
        if (needSceneRefresh) {
          queueRefreshScene();
        }
      }
    }.execute();
  }

  public abstract void onLoadResources();

  protected abstract void refreshHud(HUD hud);

  protected abstract void refreshScene(StarfieldScene scene);

  protected abstract int getDesiredSectorRadius();

  protected GestureDetector.OnGestureListener createGestureListener() {
    return new GestureListener();
  }

  protected ScaleGestureDetector.OnScaleGestureListener createScaleGestureListener() {
    return new ScaleGestureListener();
  }

  private Tuple<StarfieldScene, HUD> createScene() {
    StarfieldScene scene = new StarfieldScene(this, sectorX, sectorY, getDesiredSectorRadius());
    scene.setBackground(new Background(0.0f, 0.0f, 0.0f));
    scene.setOnSceneTouchListener(this);

    long startTime = SystemClock.elapsedRealtime();
    refreshScene(scene);
    long endTime = SystemClock.elapsedRealtime();
    log.info("Scene re-drawn in %d ms [%d top-level entities]", (endTime - startTime),
        scene.getChildCount());

    HUD hud = new HUD();
    refreshHud(hud);

    return new Tuple<>(scene, hud);
  }

  @Nullable
  public StarfieldScene getScene() {
    return scene;
  }

  protected void updateZoomFactor(float zoomFactor) {
    ((ZoomCamera) activity.getCamera()).setZoomFactor(zoomFactor);
  }

  public void scrollTo(BaseStar star) {
    if (star == null) {
      return;
    }
    scrollTo(star.getSectorX(), star.getSectorY(), star.getOffsetX(),
        Sector.SECTOR_SIZE - star.getOffsetY());
  }

  /**
   * Scroll to the given sector (x,y) and offset into the sector.
   */
  public void scrollTo(final long sectorX, final long sectorY, final float offsetX,
      final float offsetY) {
    if (activity == null || activity.getEngine() == null) {
      // TODO: if this happens, we should try again after the engine is created...
      return;
    }

    activity.getEngine().runOnUpdateThread(new Runnable() {
      @Override
      public void run() {
        final long dx = SectorSceneManager.this.sectorX - sectorX;
        final long dy = SectorSceneManager.this.sectorY - sectorY;
        SectorSceneManager.this.sectorX = sectorX;
        SectorSceneManager.this.sectorY = sectorY;
        SectorSceneManager.this.offsetX = offsetX;
        SectorSceneManager.this.offsetY = offsetY;

        StarfieldScene scene = SectorSceneManager.this.scene;
        int sectorRadius = 1;
        if (scene != null) {
          sectorRadius = scene.getSectorRadius();
        }
        List<Pair<Long, Long>> missingSectors = null;
        for (long sy = SectorSceneManager.this.sectorY
            - sectorRadius; sy <= SectorSceneManager.this.sectorY + sectorRadius; sy++) {
          for (long sx = SectorSceneManager.this.sectorX
              - sectorRadius; sx <= SectorSceneManager.this.sectorX + sectorRadius; sx++) {
            Pair<Long, Long> key = new Pair<Long, Long>(sx, sy);
            Sector s = SectorManager.i.getSector(sx, sy);
            if (s == null) {
              if (missingSectors == null) {
                missingSectors = new ArrayList<Pair<Long, Long>>();
              }
              missingSectors.add(key);
            }
          }
        }

        if (scene != null && (dy != 0 || dx != 0)) {
          offsetChildren(scene, dx * Sector.SECTOR_SIZE, -dy * Sector.SECTOR_SIZE);
        }

        if (missingSectors != null) {
          SectorManager.i.refreshSectors(missingSectors, false);
        } else if (dx != 0 || dy != 0) {
          queueRefreshScene();
        }

        activity.getCamera().setCenter(SectorSceneManager.this.offsetX,
            SectorSceneManager.this.offsetY);
      }
    });
  }

  private void offsetChildren(StarfieldScene scene, final float dx, final float dy) {
    scene.callOnChildren(new IEntityParameterCallable() {
      @Override
      public void call(IEntity entity) {
        entity.setPosition(entity.getX() + dx, entity.getY() + dy);
      }
    });
  }

  /**
   * Scrolls the view by a relative amount.
   *
   * @param distanceX Number of pixels in the X direction to scroll.
   * @param distanceY Number of pixels in the Y direction to scroll.
   */
  public void scroll(float distanceX, float distanceY) {
    long newSectorX = sectorX;
    long newSectorY = sectorY;
    float newOffsetX = offsetX + distanceX;
    float newOffsetY = offsetY + distanceY;

    boolean needUpdate = false;
    while (newOffsetX < -Sector.SECTOR_SIZE / 2) {
      newOffsetX += Sector.SECTOR_SIZE;
      newSectorX--;
      needUpdate = true;
    }
    while (newOffsetX > Sector.SECTOR_SIZE / 2) {
      newOffsetX -= Sector.SECTOR_SIZE;
      newSectorX++;
      needUpdate = true;
    }
    while (newOffsetY < -Sector.SECTOR_SIZE / 2) {
      newOffsetY += Sector.SECTOR_SIZE;
      newSectorY++;
      needUpdate = true;
    }
    while (newOffsetY > Sector.SECTOR_SIZE / 2) {
      newOffsetY -= Sector.SECTOR_SIZE;
      newSectorY--;
      needUpdate = true;
    }

    if (needUpdate) {
      scrollTo(newSectorX, newSectorY, newOffsetX, newOffsetY);
    } else {
      offsetX = newOffsetX;
      offsetY = newOffsetY;
      activity.getCamera().setCenter(offsetX, offsetY);
    }
  }

  @Override
  public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
    boolean handled = false;
    if (scaleGestureDetector != null) {
      if (scaleGestureDetector.onTouchEvent(touchEvent.getMotionEvent())) {
        handled = true;
      }
    }
    if (gestureDetector != null) {
      if (gestureDetector.onTouchEvent(touchEvent.getMotionEvent())) {
        handled = true;
      }
    }

    return handled;
  }

  /**
   * The default gesture listener is just for scrolling around.
   */
  protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      final ZoomCamera zoomCamera = (ZoomCamera) activity.getCamera();
      final float zoomFactor = zoomCamera.getZoomFactor();
      scroll(distanceX / zoomFactor, -distanceY / zoomFactor);

      return true;
    }
  }

  /**
   * The default scale gesture listener scales the view.
   */
  protected class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private float mZoomFactor;

    public ScaleGestureListener() {
      mZoomFactor = activity.getResources().getDisplayMetrics().density;
      updateZoomFactor(mZoomFactor);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      mZoomFactor *= detector.getScaleFactor();
      if (mZoomFactor < 0.333f) {
        mZoomFactor = 0.333f;
      }
      if (mZoomFactor > 2.5f) {
        mZoomFactor = 2.5f;
      }

      updateZoomFactor(mZoomFactor);
      return true;
    }
  }

  public interface SceneCreatedHandler {
    void onSceneCreated(Scene scene);
  }
}
