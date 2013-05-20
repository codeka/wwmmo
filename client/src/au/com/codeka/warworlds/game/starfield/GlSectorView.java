package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

/**
 * This is the base class for StarfieldSurfaceView and TacticalMapView, it contains the common code
 * for scrolling through sectors of stars, etc.
 */
public abstract class GlSectorView extends GLSurfaceView 
                        implements SectorManager.OnSectorListChangedListener {
    private static final Logger log = LoggerFactory.getLogger(GlSectorView.class);
    protected boolean mScrollToCentre = false;

    private GestureDetector mGestureDetector;
    GestureDetector.OnGestureListener mGestureListener;
    private boolean mDisableGestures = false;
    private Context mContext;

    protected int mSectorRadius = 1;
    protected long mSectorX;
    protected long mSectorY;
    protected float mOffsetX;
    protected float mOffsetY;

    protected float mPixelScale;

    public GlSectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (this.isInEditMode()) {
            return;
        }

        setEGLContextClientVersion(2);
        setRenderer(createRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mPixelScale = context.getResources().getDisplayMetrics().density;
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
    }

    protected abstract GLSurfaceView.Renderer createRenderer();

    public float getPixelScale() {
        return mPixelScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (mGestureDetector == null && !mDisableGestures) {
            GestureDetector.OnGestureListener listener = createGestureListener();
            if (listener == null) {
                mDisableGestures = true;
                return true;
            }
            mGestureDetector = new GestureDetector(mContext, listener);
        }

        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    /**
     * If you return non-null from this, we'll set up a gesture detector that calls the
     * methods of the object you return whenever the user gestures on the surface.
     */
    protected GestureDetector.OnGestureListener createGestureListener() {
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SectorManager.getInstance().addSectorListChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SectorManager.getInstance().removeSectorListChangedListener(this);
    }

    @Override
    public void onSectorListChanged() {
        requestRender();
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY) {
        scrollTo(sectorX, sectorY, offsetX, offsetY, false);
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY, boolean centre) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = -offsetX;
        mOffsetY = -offsetY;

        if (centre) {
            mScrollToCentre = true;
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

        requestRender();
    }

    /**
     * Scrolls the view by a relative amount.
     * @param distanceX Number of pixels in the X direction to scroll.
     * @param distanceY Number of pixels in the Y direction to scroll.
     */
    public void scroll(float distanceX, float distanceY) {
        mOffsetX += distanceX;
        mOffsetY += distanceY;

        boolean needUpdate = false;
        while (mOffsetX < -(Sector.SECTOR_SIZE / 2)) {
            mOffsetX += Sector.SECTOR_SIZE;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > (Sector.SECTOR_SIZE / 2)) {
            mOffsetX -= Sector.SECTOR_SIZE;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -(Sector.SECTOR_SIZE / 2)) {
            mOffsetY += Sector.SECTOR_SIZE;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > (Sector.SECTOR_SIZE / 2)) {
            mOffsetY -= Sector.SECTOR_SIZE;
            mSectorY --;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(mSectorX, mSectorY, -mOffsetX, -mOffsetY);
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
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        super.onDraw(canvas);

        if (mScrollToCentre) {
            scroll(getWidth() / 2.0f / getPixelScale(),
                   getHeight() / 2.0f / getPixelScale());
            mScrollToCentre = false;
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            scroll(-(float)(distanceX / getPixelScale()),
                   -(float)(distanceY / getPixelScale()));

            requestRender();
            return false;
        }
    }

    protected static void checkError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            log.error(glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    protected static class Renderer implements GLSurfaceView.Renderer {
        protected float mRatio;
        protected final float[] mViewProjMatrix = new float[16];
        protected final float[] mViewMatrix = new float[16];
        protected final float[] mProjMatrix = new float[16];

        @Override
        public void onDrawFrame(GL10 _) {
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            Matrix.multiplyMM(mViewProjMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        @Override
        public void onSurfaceChanged(GL10 _, int width, int height) {
            GLES20.glViewport(0, 0, width, height);

            mRatio = (float) width / height;
            Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);
        }

        @Override
        public void onSurfaceCreated(GL10 _, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        protected int loadShader(int type, String shaderCode){
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            int shader = GLES20.glCreateShader(type);
            checkError("glCreateShader");

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);
            checkError("glShaderSource");
            GLES20.glCompileShader(shader);
            checkError("glCompileShader");

            return shader;
        }
    }
}
