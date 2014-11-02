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

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.Pair;
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
    @Nullable private StarfieldScene mScene;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    protected BaseGlActivity mActivity;
    protected long mSectorX;
    protected long mSectorY;
    protected float mOffsetX;
    protected float mOffsetY;
    private boolean mWasStopped;
    private SceneCreatedHandler mSceneCreatedHandler;
    private boolean needSceneRefresh;
    private boolean isSceneRefreshing;

    public SectorSceneManager(BaseGlActivity activity) {
        mActivity = activity;
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
    }

    protected void onStart() {
        SectorManager.eventBus.register(eventHandler);

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(mActivity, createGestureListener());

            ScaleGestureDetector.OnScaleGestureListener scaleListener = createScaleGestureListener();
            if (scaleListener != null) {
                mScaleGestureDetector = new ScaleGestureDetector(mActivity, scaleListener);
            }
        }

        if (mWasStopped) {
            log.debug("We were stopped, refreshing the scene...");
            queueRefreshScene();
        }
    }

    protected void onStop() {
        SectorManager.eventBus.unregister(eventHandler);
        mWasStopped = true;
    }

    public void setSceneCreatedHandler(SceneCreatedHandler handler) {
        mSceneCreatedHandler = handler;
    }

    public BaseGlActivity getActivity() {
        return mActivity;
    }

    private final Object eventHandler = new Object() {
        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onSectorUpdated(Sector sector) {
            queueRefreshScene();
        }

        @EventHandler(thread = EventHandler.UI_THREAD)
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
        new BackgroundRunner<Scene>() {
            @Override
            protected Scene doInBackground() {
                try {
                    return createScene();
                } catch(Exception e) {
                    // the most common reason for this is when the activity is destroyed before we finish...
                    log.warning("Error while refreshing scene.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(final Scene scene) {
                final Engine engine = mActivity.getEngine();
                if (scene != null && engine != null) {
                  engine.runOnUpdateThread(new Runnable() {
                        @Override
                        public void run() {
                            engine.setScene(scene);
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

    public StarfieldScene createScene() {
        StarfieldScene scene = new StarfieldScene(this, getDesiredSectorRadius());
        scene.setBackground(new Background(0.0f, 0.0f, 0.0f));
        scene.setOnSceneTouchListener(this);

        refreshScene(scene);

        HUD hud = new HUD();
        refreshHud(hud);

        if (mSceneCreatedHandler != null) {
            mSceneCreatedHandler.onSceneCreated(scene);
        }

        if (mScene != null) {
            scene.copySelection(mScene);
        }
        mScene = scene;
        mActivity.getCamera().setHUD(hud);
        return mScene;
    }

    @Nullable
    public StarfieldScene getScene() {
        return mScene;
    }

    protected void updateZoomFactor(float zoomFactor) {
        ((ZoomCamera) mActivity.getCamera()).setZoomFactor(zoomFactor);
    }

    public void scrollTo(BaseStar star) {
        if (star == null)
            return;
        scrollTo(star.getSectorX(), star.getSectorY(), star.getOffsetX(), Sector.SECTOR_SIZE - star.getOffsetY());
    }

    /** Scroll to the given sector (x,y) and offset into the sector. */
    public void scrollTo(final long sectorX, final long sectorY,
                         final float offsetX, final float offsetY) {
        mActivity.getEngine().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                final long dx = mSectorX - sectorX;
                final long dy = mSectorY - sectorY;
                mSectorX = sectorX;
                mSectorY = sectorY;
                mOffsetX = offsetX;
                mOffsetY = offsetY;

                StarfieldScene scene = mScene;
                if (scene == null) {
                    return;
                }
                List<Pair<Long, Long>> missingSectors = null;
                for(long sy = mSectorY - scene.getSectorRadius(); sy <= mSectorY + scene.getSectorRadius(); sy++) {
                    for(long sx = mSectorX - scene.getSectorRadius(); sx <= mSectorX + scene.getSectorRadius(); sx++) {
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

                if (dy != 0 || dx != 0) {
                    scene.callOnChildren(new IEntityParameterCallable() {
                        @Override
                        public void call(IEntity entity) {
                            entity.setPosition(
                                    entity.getX() + (dx * Sector.SECTOR_SIZE),
                                    entity.getY() - (dy * Sector.SECTOR_SIZE));
                        }
                    });
                }

                if (missingSectors != null) {
                    SectorManager.i.refreshSectors(missingSectors, false);
                } else if (dx != 0 || dy != 0) {
                    queueRefreshScene();
                }

                mActivity.getCamera().setCenter(mOffsetX, mOffsetY);
            }
        });
    }

    /**
     * Scrolls the view by a relative amount.
     * @param distanceX Number of pixels in the X direction to scroll.
     * @param distanceY Number of pixels in the Y direction to scroll.
     */
    public void scroll(float distanceX, float distanceY) {
        long newSectorX = mSectorX;
        long newSectorY = mSectorY;
        float newOffsetX = mOffsetX + distanceX;
        float newOffsetY = mOffsetY + distanceY;

        boolean needUpdate = false;
        while (newOffsetX < -Sector.SECTOR_SIZE / 2) {
            newOffsetX += Sector.SECTOR_SIZE;
            newSectorX --;
            needUpdate = true;
        }
        while (newOffsetX > Sector.SECTOR_SIZE / 2) {
            newOffsetX -= Sector.SECTOR_SIZE;
            newSectorX ++;
            needUpdate = true;
        }
        while (newOffsetY < -Sector.SECTOR_SIZE / 2) {
            newOffsetY += Sector.SECTOR_SIZE;
            newSectorY ++;
            needUpdate = true;
        }
        while (newOffsetY > Sector.SECTOR_SIZE / 2) {
            newOffsetY -= Sector.SECTOR_SIZE;
            newSectorY --;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(newSectorX, newSectorY, newOffsetX, newOffsetY);
        } else {
            mOffsetX = newOffsetX;
            mOffsetY = newOffsetY;
            mActivity.getCamera().setCenter(mOffsetX, mOffsetY);
        }
    }

    @Override
    public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
        boolean handled = false;
        if (mScaleGestureDetector != null) {
            if (mScaleGestureDetector.onTouchEvent(touchEvent.getMotionEvent())) {
                handled = true;
            }
        }
        if (mGestureDetector != null) {
            if (mGestureDetector.onTouchEvent(touchEvent.getMotionEvent())) {
                handled = true;
            }
        }

        return handled;
    }

    /** The default gesture listener is just for scrolling around. */
    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            final ZoomCamera zoomCamera = (ZoomCamera) mActivity.getCamera();
            final float zoomFactor = zoomCamera.getZoomFactor();
            scroll( distanceX / zoomFactor,
                   -distanceY / zoomFactor);

            return true;
        }
    }

    /** The default scale gesture listener scales the view. */
    protected class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float mZoomFactor;

        public ScaleGestureListener() {
            mZoomFactor = mActivity.getResources().getDisplayMetrics().density;
            updateZoomFactor(mZoomFactor);
        }

        @Override
        public boolean onScale (ScaleGestureDetector detector) {
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
