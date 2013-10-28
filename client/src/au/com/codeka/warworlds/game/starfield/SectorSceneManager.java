package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.IEntity;
import org.andengine.entity.IEntityParameterCallable;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

/**
 * This is the base class for StarfieldSurfaceView and TacticalMapView, it contains the common code
 * for scrolling through sectors of stars, etc.
 */
public abstract class SectorSceneManager implements SectorManager.OnSectorListChangedListener,
                                                    IOnSceneTouchListener {
    private static final Logger log = LoggerFactory.getLogger(SectorSceneManager.class);
    private Scene mScene;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    protected BaseGlActivity mActivity;
    protected int mSectorRadius = 1;
    protected long mSectorX;
    protected long mSectorY;
    protected float mOffsetX;
    protected float mOffsetY;

    public SectorSceneManager(BaseGlActivity activity) {
        mActivity = activity;
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
    }

    protected void onStart() {
        SectorManager.getInstance().addSectorListChangedListener(this);

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(mActivity, createGestureListener());

            ScaleGestureDetector.OnScaleGestureListener scaleListener = createScaleGestureListener();
            if (scaleListener != null) {
                mScaleGestureDetector = new ScaleGestureDetector(mActivity, scaleListener);
            }
        }
    }

    protected void onStop() {
        SectorManager.getInstance().removeSectorListChangedListener(this);
    }

    public BaseGlActivity getActivity() {
        return mActivity;
    }

    @Override
    public void onSectorListChanged() {
        final Scene scene = createScene();

        mActivity.getEngine().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getEngine().setScene(scene);
            }
        });
    }

    public abstract void onLoadResources();
    protected abstract void refreshScene(Scene scene);

    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    protected ScaleGestureDetector.OnScaleGestureListener createScaleGestureListener() {
        return new ScaleGestureListener();
    }

    public Scene createScene() {
        mScene = new Scene();
        mScene.setBackground(new Background(0.0f, 0.0f, 0.0f));
        mScene.setOnSceneTouchListener(this);

        refreshScene(mScene);

        return mScene;
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY) {
        scrollTo(sectorX, sectorY, offsetX, offsetY, false);
    }

    protected void updateZoomFactor(float zoomFactor) {
        ((ZoomCamera) mActivity.getCamera()).setZoomFactor(zoomFactor);
    }

    /** Scroll to the given sector (x,y) and offset into the sector. */
    public void scrollTo(final long sectorX, final long sectorY,
                         final float offsetX, final float offsetY,
                         final boolean centre) {
        mActivity.getEngine().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                final long dy = mSectorY - sectorY;
                final long dx = mSectorX - sectorX;
                if (dy != 0 || dx != 0) {
                    mScene.callOnChildren(new IEntityParameterCallable() {
                        @Override
                        public void call(IEntity entity) {
                            entity.setPosition(
                                    entity.getX() + (dx * Sector.SECTOR_SIZE),
                                    entity.getY() + (dy * Sector.SECTOR_SIZE));
                        }
                    });
                }

                mSectorX = sectorX;
                mSectorY = sectorY;
                mOffsetX = offsetX;
                mOffsetY = offsetY;

                List<Pair<Long, Long>> missingSectors = null;
                for(long sy = mSectorY - mSectorRadius; sy <= mSectorY + mSectorRadius; sy++) {
                    for(long sx = mSectorX - mSectorRadius; sx <= mSectorX + mSectorRadius; sx++) {
                        Pair<Long, Long> key = new Pair<Long, Long>(sx, sy);
                        Sector s = SectorManager.getInstance().getSector(sx, sy);
                        if (s == null) {
                            if (missingSectors == null) {
                                missingSectors = new ArrayList<Pair<Long, Long>>();
                            }
                            missingSectors.add(key);
                        }
                    }
                }
                if (missingSectors != null) {
                    SectorManager.getInstance().requestSectors(missingSectors, false, null);
                }

                mActivity.getCamera().setCenter(mOffsetX, mOffsetY);
                if (centre) {
                    scroll(mActivity.getCamera().getWidth() / 2.0f,
                           mActivity.getCamera().getHeight() / 2.0f);
                }
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
        while (newOffsetX < -(Sector.SECTOR_SIZE / 2)) {
            newOffsetX += Sector.SECTOR_SIZE;
            newSectorX --;
            needUpdate = true;
        }
        while (newOffsetX > (Sector.SECTOR_SIZE / 2)) {
            newOffsetX -= Sector.SECTOR_SIZE;
            newSectorX ++;
            needUpdate = true;
        }
        while (newOffsetY < -(Sector.SECTOR_SIZE / 2)) {
            newOffsetY += Sector.SECTOR_SIZE;
            newSectorY --;
            needUpdate = true;
        }
        while (newOffsetY > (Sector.SECTOR_SIZE / 2)) {
            newOffsetY -= Sector.SECTOR_SIZE;
            newSectorY ++;
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

    /**
     * Gets the \c Star that's closest to the given (x,y), based on the current sector
     * centre and offsets.
     */
    public Star getStarAt(int viewX, int viewY) {
        // first, work out which sector your actually inside of. If (mOffsetX, mOffsetY) is (0,0)
        // then (x,y) corresponds exactly to the offset into (mSectorX, mSectorY). Otherwise, we
        // have to adjust (x,y) by the offset so that it works out like that.
        int x = viewX - (int) mOffsetX;
        int y = viewY - (int) mOffsetY;

        long sectorX = mSectorX;
        long sectorY = mSectorY;
        while (x < 0) {
            x += Sector.SECTOR_SIZE;
            sectorX --;
        }
        while (x >= Sector.SECTOR_SIZE) {
            x -= Sector.SECTOR_SIZE;
            sectorX ++;
        }
        while (y < 0) {
            y += Sector.SECTOR_SIZE;
            sectorY --;
        }
        while (y >= Sector.SECTOR_SIZE) {
            y -= Sector.SECTOR_SIZE;
            sectorY ++;
        }

        Sector sector = SectorManager.getInstance().getSector(sectorX, sectorY);
        if (sector == null) {
            // if it's not loaded yet, you can't have tapped on anything...
            return null;
        }

        int minDistance = 0;
        BaseStar closestStar = null;

        for(BaseStar star : sector.getStars()) {
            int starX = star.getOffsetX();
            int starY = star.getOffsetY();

            int distance = (starX - x)*(starX - x) + (starY - y)*(starY - y);
            if (closestStar == null || distance < minDistance) {
                closestStar = star;
                minDistance = distance;
            }
        }

        // only return it if you tapped within a 48 pixel radius
        if (Math.sqrt(minDistance) <= 48) {
            return (Star) closestStar;
        }
        return null;
    }

    @Override
    public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
        boolean returnValue = false;
        if (mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(touchEvent.getMotionEvent());
        }
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(touchEvent.getMotionEvent());
        }

        return true;
    }

    /** The default gesture listener is just for scrolling around. */
    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            scroll( (float)(distanceX),
                   -(float)(distanceY));

            return true;
        }
    }

    /** The default scale gesture listener scales the view. */
    protected class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float mZoomFactor = 1.0f;

        @Override
        public boolean onScale (ScaleGestureDetector detector) {
            mZoomFactor *= detector.getScaleFactor();

            updateZoomFactor(mZoomFactor);
            return true;
        }
    }
}
