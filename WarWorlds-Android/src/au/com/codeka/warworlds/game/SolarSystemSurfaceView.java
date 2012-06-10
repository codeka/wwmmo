package au.com.codeka.warworlds.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;

/**
 * \c SurfaceView that displays a solar system. Star in the top-left, planets arrayed around,
 * and representations of the fleets, etc.
 */
public class SolarSystemSurfaceView extends UniverseElementSurfaceView {
    private static Logger log = LoggerFactory.getLogger(SolarSystemSurfaceView.class);
    private Star mStar;
    private PlanetInfo[] mPlanetInfos;
    private PlanetInfo mSelectedPlanet;
    private boolean mPlanetsPlaced;
    private Paint mSunPaint;
    private Paint mPlanetPaint;
    private Paint mSelectedPlanetPaint;
    private Bitmap mColonyIcon;
    private CopyOnWriteArrayList<OnPlanetSelectedListener> mPlanetSelectedListeners;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private boolean mPlanetSelectedFired;
    private Handler mHandler;

    public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mPlanetSelectedListeners = new CopyOnWriteArrayList<OnPlanetSelectedListener>();
        mHandler = new Handler();
        mBackgroundRenderer = new StarfieldBackgroundRenderer(context);

        int[] colours = { Color.YELLOW, Color.YELLOW, 0x00000000 };
        float[] positions = { 0.0f, 0.4f, 1.0f };
        mSunPaint = new Paint();
        mSunPaint.setDither(true);
        RadialGradient gradient = new RadialGradient(0, 0, 150 * getPixelScale(), 
                colours, positions, android.graphics.Shader.TileMode.CLAMP);
        mSunPaint.setShader(gradient);

        mPlanetPaint = new Paint();
        mPlanetPaint.setARGB(255, 255, 255, 255);
        mPlanetPaint.setStyle(Style.STROKE);

        mSelectedPlanetPaint = new Paint();
        mSelectedPlanetPaint.setARGB(255, 255, 255, 255);
        mSelectedPlanetPaint.setStyle(Style.STROKE);

        mColonyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.starfield_colony);
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
            if (planetInfo.planet.getKey().equals(planet.getKey())) {
                Point2D pixels = planetInfo.centre;
                return new Point2D(pixels.getX() / getPixelScale(),
                                    pixels.getY() / getPixelScale());
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
            centre = centre.rotate(angle);
            centre.setY(-1 * centre.getY());
            log.debug("Planet centre: ("+centre.getX()+","+centre.getY()+")");

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
    public void selectPlanet(String planetKey) {
        for(PlanetInfo planetInfo : mPlanetInfos) {
            if (planetInfo.planet.getKey().equals(planetKey)) {
                mSelectedPlanet = planetInfo;

                if (mPlanetsPlaced) {
                    firePlanetSelected(mSelectedPlanet.planet);
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

            mBackgroundRenderer.drawBackground(canvas, 0, 0,
                    canvas.getWidth() / getPixelScale(),
                    canvas.getHeight() / getPixelScale(),
                    mStar.hashCode());

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
        canvas.drawCircle(0, 0, 150 * getPixelScale(), mSunPaint);
    }

    private void drawPlanets(Canvas canvas) {
        for (int i = 0; i < mPlanetInfos.length; i++) {
            canvas.drawCircle(0, 0,
                    mPlanetInfos[i].distanceFromSun, mPlanetPaint);
        }

        for (int i = 0; i < mPlanetInfos.length; i++) {
            final PlanetInfo planetInfo = mPlanetInfos[i];
            int resID = planetInfo.planet.getPlanetType().getMedID();
            if (resID != 0) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), resID);

                canvas.drawBitmap(bm,
                        (float) planetInfo.centre.getX() - (bm.getWidth() / 2.0f),
                        (float) planetInfo.centre.getY() - (bm.getHeight() / 2.0f),
                        mPlanetPaint);
            }

            List<Colony> colonies = mStar.getColonies();
            if (colonies != null && !colonies.isEmpty()) {
                for (Colony colony : colonies) {
                    if (colony.getPlanetKey().equals(mPlanetInfos[i].planet.getKey())) {
                        canvas.drawBitmap(mColonyIcon,
                                (float) (planetInfo.centre.getX() + mColonyIcon.getWidth()),
                                (float) (planetInfo.centre.getY() - mColonyIcon.getHeight()),
                                mPlanetPaint);
                    }
                }
            }
        }

        if (mSelectedPlanet != null) {
            canvas.drawCircle((float) mSelectedPlanet.centre.getX(), 
                    (float) mSelectedPlanet.centre.getY(),
                    40 * getPixelScale(), mSelectedPlanetPaint);
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
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
                    closestPlanet != mSelectedPlanet &&
                    tapLocation.distanceTo(closestPlanet.centre) < 60.0 * getPixelScale()) {
                newSelection = closestPlanet;
            }

            if (newSelection != null) {
                selectPlanet(newSelection.planet.getKey());

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
