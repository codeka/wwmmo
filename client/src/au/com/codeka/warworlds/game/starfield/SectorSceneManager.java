package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import org.andengine.engine.camera.ZoomCamera;
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
    protected float mCameraX;
    protected float mCameraY;

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

    @Override
    public void onSectorListChanged() {
        mActivity.getEngine().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                refreshScene(mScene);
                mCameraX = 0;
                mCameraY = 0;
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
        mCameraX = 0;
        mCameraY = 0;

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

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY, boolean centre) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = offsetX;
        mOffsetY = offsetY;

        if (centre) {
            mActivity.getEngine().runOnUpdateThread(new Runnable() {
                @Override
                public void run() {
                    scroll(mActivity.getCamera().getWidth() / 2.0f,
                           mActivity.getCamera().getHeight() / 2.0f);
                }
            });
        }

        List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();

        for(sectorY = mSectorY - mSectorRadius; sectorY <= mSectorY + mSectorRadius; sectorY++) {
            for(sectorX = mSectorX - mSectorRadius; sectorX <= mSectorX + mSectorRadius; sectorX++) {
                Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
                Sector s = SectorManager.getInstance().getSector(sectorX, sectorY);
                if (s == null) {
                    missingSectors.add(key);
                }
            }
        }

        if (!missingSectors.isEmpty()) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }

        updateCamera();
    }

    private void updateCamera() {
        mActivity.getCamera().setCenter(mCameraX, mCameraY);
    }

    /**
     * Scrolls the view by a relative amount.
     * @param distanceX Number of pixels in the X direction to scroll.
     * @param distanceY Number of pixels in the Y direction to scroll.
     */
    public void scroll(float distanceX, float distanceY) {
        mOffsetX += distanceX;
        mOffsetY += distanceY;
        mCameraX += distanceX;
        mCameraY += distanceY;

        boolean needUpdate = false;
        while (mOffsetX < -(Sector.SECTOR_SIZE / 2)) {
            mOffsetX += Sector.SECTOR_SIZE;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetX > (Sector.SECTOR_SIZE / 2)) {
            mOffsetX -= Sector.SECTOR_SIZE;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetY < -(Sector.SECTOR_SIZE / 2)) {
            mOffsetY += Sector.SECTOR_SIZE;
            mSectorY --;
            needUpdate = true;
        }
        while (mOffsetY > (Sector.SECTOR_SIZE / 2)) {
            mOffsetY -= Sector.SECTOR_SIZE;
            mSectorY ++;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(mSectorX, mSectorY, mOffsetX, mOffsetY);
        } else {
            updateCamera();
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
        if (mGestureDetector == null) {
            return false;
        }

        if (mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(touchEvent.getMotionEvent());
        }
        return mGestureDetector.onTouchEvent(touchEvent.getMotionEvent());
    }

    /** The default gesture listener is just for scrolling around. */
    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            scroll( (float)(distanceX),
                   -(float)(distanceY));

            return false;
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
