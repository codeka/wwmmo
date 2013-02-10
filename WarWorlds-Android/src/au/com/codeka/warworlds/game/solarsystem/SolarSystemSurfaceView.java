package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

/**
 * \c SurfaceView that displays a solar system. Star in the top-left, planets arrayed around,
 * and representations of the fleets, etc.
 */
public class SolarSystemSurfaceView extends UniverseElementSurfaceView {
    private static Logger log = LoggerFactory.getLogger(SolarSystemSurfaceView.class);
    private Context mContext;
    private Star mStar;
    private PlanetInfo[] mPlanetInfos;
    private PlanetInfo mSelectedPlanet;
    private boolean mPlanetsPlaced;
    private Paint mPlanetPaint;
    private SelectionView mSelectionView;
    private Bitmap mColonyIcon;
    private CopyOnWriteArrayList<OnPlanetSelectedListener> mPlanetSelectedListeners;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private boolean mPlanetSelectedFired;
    private Handler mHandler;
    private BitmapGeneratedListener mBitmapGeneratedListener;
    private Matrix mMatrix;

    public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mContext = context;
        mPlanetSelectedListeners = new CopyOnWriteArrayList<OnPlanetSelectedListener>();
        mHandler = new Handler();

        mPlanetPaint = new Paint();
        mPlanetPaint.setARGB(255, 255, 255, 255);
        mPlanetPaint.setStyle(Style.STROKE);
        mMatrix = new Matrix();

        mColonyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.starfield_colony);

        mBitmapGeneratedListener = new BitmapGeneratedListener();
        PlanetImageManager.getInstance().addBitmapGeneratedListener(mBitmapGeneratedListener);
        StarImageManager.getInstance().addBitmapGeneratedListener(mBitmapGeneratedListener);
    }

    public void setSelectionView(SelectionView selectionView) {
        mSelectionView = selectionView;
        if (mSelectionView != null) {
            mSelectionView.setVisibility(View.GONE);
        }
    }

    public void setStar(Star star) {
        mStar = star;

        Planet[] planets = mStar.getPlanets();
        mPlanetInfos = new PlanetInfo[planets.length];

        for (int i = 0; i < planets.length; i++) {
            PlanetInfo planetInfo = new PlanetInfo();
            planetInfo.planet = planets[i];
            planetInfo.centre = new Point2D(0, 0);
            planetInfo.distanceFromSun = 0.0f;
            mPlanetInfos[i] = planetInfo;
        }

        mPlanetsPlaced = false;
    }

    /**
     * Gets a \c Point2D representing the centre of the given planet, relative to this
     * \c SolarSystemSurfaceView in device pixels.
     */
    public Point2D getPlanetCentre(Planet planet) {
        if (!mPlanetsPlaced) {
            return null;
        }

        for(PlanetInfo planetInfo : mPlanetInfos) {
            if (planetInfo.planet.getIndex() == planet.getIndex()) {
                Point2D pixels = planetInfo.centre;
                return new Point2D(pixels.x / getPixelScale(),
                                   pixels.y / getPixelScale());
            }
        }

        return null;
    }

    private void placePlanets(Canvas canvas) {
        if (mPlanetsPlaced) {
            return;
        }

        float planetStart = 150 * getPixelScale();
        float distanceBetweenPlanets = canvas.getWidth() - planetStart;
        distanceBetweenPlanets /= mPlanetInfos.length;

        for (int i = 0; i < mPlanetInfos.length; i++) {
            PlanetInfo planetInfo = mPlanetInfos[i];

            float distanceFromSun = planetStart + (distanceBetweenPlanets * i) + (distanceBetweenPlanets / 2.0f);
            float x = 0;
            float y = -1 * distanceFromSun;

            float angle = (0.5f/(mPlanetInfos.length + 1));
            angle = (float) ((angle*i*Math.PI) + angle*Math.PI);

            Point2D centre = new Point2D(x, y);
            centre.rotate(angle);
            centre.y *= -1;
            log.debug("Planet centre: ("+centre.x+","+centre.y+")");

            planetInfo.centre = centre;
            planetInfo.distanceFromSun = distanceFromSun;
            mPlanetInfos[i] = planetInfo;
        }

        mPlanetsPlaced = true;
    }

    public void addPlanetSelectedListener(OnPlanetSelectedListener listener) {
        if (!mPlanetSelectedListeners.contains(listener)) {
            mPlanetSelectedListeners.add(listener);
        }
    }

    public void removePlanetSelectedListener(OnPlanetSelectedListener listener) {
        mPlanetSelectedListeners.remove(listener);
    }

    protected void firePlanetSelected(Planet planet) {
        for(OnPlanetSelectedListener listener : mPlanetSelectedListeners) {
            listener.onPlanetSelected(planet);
        }
    }

    public Planet getSelectedPlanet() {
        return mSelectedPlanet.planet;
    }
    public void selectPlanet(int planetIndex) {
        for(PlanetInfo planetInfo : mPlanetInfos) {
            if (planetInfo.planet.getIndex() == planetIndex) {
                mSelectedPlanet = planetInfo;

                if (mPlanetsPlaced) {
                    firePlanetSelected(mSelectedPlanet.planet);
                } else {
                    mPlanetSelectedFired = false;
                }

                if (mPlanetsPlaced && mSelectedPlanet != null && mSelectionView != null) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSelectionView.getLayoutParams();
                    params.leftMargin = (int) (getLeft() + mSelectedPlanet.centre.x - (mSelectionView.getWidth() / 2));
                    params.topMargin = (int) (getTop() + mSelectedPlanet.centre.y - (mSelectionView.getHeight() / 2));
                    mSelectionView.setLayoutParams(params);
                    mSelectionView.setVisibility(View.VISIBLE);
                } else if (mSelectionView != null) {
                    mSelectionView.setVisibility(View.GONE);
                }

                redraw();
            }
        }
    }

    /**
     * Creates the \c OnGestureListener that'll handle our gestures.
     */
    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    @Override
    public void onDetachedFromWindow() {
        PlanetImageManager.getInstance().removeBitmapGeneratedListener(mBitmapGeneratedListener);
    }

    /**
     * Draws the actual starfield to the given \c Canvas. This will be called in
     * a background thread, so we can't do anything UI-specific, except drawing
     * to the canvas.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (mStar != null) {
            canvas.drawColor(Color.BLACK);

            if (mBackgroundRenderer == null) {
                mBackgroundRenderer = new StarfieldBackgroundRenderer(mContext,
                        new long[] {mStar.getKey().hashCode()});
            }
            mBackgroundRenderer.drawBackground(canvas, 0, 0,
                    canvas.getWidth() / getPixelScale(),
                    canvas.getHeight() / getPixelScale());

            // make sure the planets are in the correct position
            placePlanets(canvas);

            drawSun(canvas);
            drawPlanets(canvas);
        }

        if (!mPlanetSelectedFired && mSelectedPlanet != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    firePlanetSelected(mSelectedPlanet.planet);
                }
            });
            mPlanetSelectedFired = true;
        }
    }

    private void drawSun(Canvas canvas) {
        float pixelScale = getPixelScale();

        int imageSize = (int)(300.0f * pixelScale);
        Sprite sprite = StarImageManager.getInstance().getSprite(mContext, mStar, imageSize);

        mMatrix.reset();
        mMatrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
        mMatrix.postScale(300.0f * pixelScale / sprite.getWidth(),
                          300.0f * pixelScale / sprite.getHeight());
        canvas.save();
        canvas.setMatrix(mMatrix);
        sprite.draw(canvas);
        canvas.restore();
    }

    private void drawPlanets(Canvas canvas) {
        for (int i = 0; i < mPlanetInfos.length; i++) {
            canvas.drawCircle(0, 0,
                              mPlanetInfos[i].distanceFromSun, mPlanetPaint);
        }

        PlanetImageManager pim = PlanetImageManager.getInstance();

        for (int i = 0; i < mPlanetInfos.length; i++) {
            final PlanetInfo planetInfo = mPlanetInfos[i];

            Sprite sprite = pim.getSprite(mContext, planetInfo.planet);
            mMatrix.reset();
            mMatrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
            mMatrix.postScale(100.0f * getPixelScale() / sprite.getWidth(),
                              100.0f * getPixelScale() / sprite.getHeight());
            mMatrix.postTranslate(planetInfo.centre.x, planetInfo.centre.y);
            canvas.save();
            canvas.setMatrix(mMatrix);
            sprite.draw(canvas);
            canvas.restore();

            List<Colony> colonies = mStar.getColonies();
            if (colonies != null && !colonies.isEmpty()) {
                for (Colony colony : colonies) {
                    if (colony.getPlanetIndex() == mPlanetInfos[i].planet.getIndex()) {
                        canvas.drawBitmap(mColonyIcon,
                                (float) (planetInfo.centre.x + mColonyIcon.getWidth()),
                                (float) (planetInfo.centre.y - mColonyIcon.getHeight()),
                                mPlanetPaint);
                    }
                }
            }
        }

        if (mSelectedPlanet != null && mSelectionView != null) {

//            mSelectionOverlay.setCentre(mSelectedPlanet.centre.x, mSelectedPlanet.centre.y);
//            mSelectionOverlay.setRadius(35.0 * getPixelScale());
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mPlanetInfos == null) {
                return false;
            }

            Point2D tapLocation = new Point2D(e.getX(), e.getY());
            PlanetInfo closestPlanet = null;
            for (PlanetInfo planetInfo : mPlanetInfos) {
                if (closestPlanet == null) {
                    closestPlanet = planetInfo;
                } else {
                    double distanceToClosest = tapLocation.distanceTo(closestPlanet.centre);
                    double distanceToThis = tapLocation.distanceTo(planetInfo.centre);
                    if (distanceToThis < distanceToClosest) {
                        closestPlanet = planetInfo;
                    }
                }
            }

            PlanetInfo newSelection = null;
            if (closestPlanet != null &&
                    tapLocation.distanceTo(closestPlanet.centre) < 60.0 * getPixelScale()) {
                newSelection = closestPlanet;
            }

            if (newSelection != null) {
                selectPlanet(newSelection.planet.getIndex());

                // play the 'click' sound effect
                playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }

            return false;
        }
    }

    /**
     * This class contains info about the planets we need to render and interact with.
     * @author dean@codeka.com.au
     *
     */
    private static class PlanetInfo {
        public Planet planet;
        public Point2D centre;
        public float distanceFromSun;
    }

    /**
     * Our implementation of \c PlanetImageManager.BitmapGeneratedListener just causes us to
     * redraw the screen (with the new bitmap).
     */
    private class BitmapGeneratedListener implements ImageManager.BitmapGeneratedListener {
        @Override
        public void onBitmapGenerated(String planetKey, Bitmap bmp) {
            redraw();
        }
    }

    /**
     * This interface should be implemented when you want to listen for "planet selected"
     * events -- that is, when the user selects a new planet (by tapping on it).
     */
    public interface OnPlanetSelectedListener {
        /**
         * This is called when the user selects (by tapping it) a planet. By definition, only
         * one planet can be selected at a time.
         */
        public abstract void onPlanetSelected(Planet planet);
    }
}
