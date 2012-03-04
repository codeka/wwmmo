package au.com.codeka.warworlds.game;

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
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.R;
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
    private CopyOnWriteArrayList<OnPlanetSelectedListener> mPlanetSelectedListeners;

    public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mPlanetSelectedListeners = new CopyOnWriteArrayList<OnPlanetSelectedListener>();
    }

    public void setStar(Star star) {
        mStar = star;

        Planet[] planets = mStar.getPlanets();
        mPlanetInfos = new PlanetInfo[planets.length];

        for (int i = 0; i < planets.length; i++) {
            double x = 0; double y = -1 * ((50*i) + 250);

            double angle = (0.5/(planets.length + 1));
            angle = (angle*i*Math.PI)+ angle*Math.PI;

            Point2D centre = new Point2D(x, y);
            centre = centre.rotate(angle);
            centre.setY(-1 * centre.getY());
            log.info("Planet centre: ("+centre.getX()+","+centre.getY()+")");

            PlanetInfo planetInfo = new PlanetInfo();
            planetInfo.planet = planets[i];
            planetInfo.centre = centre;
            mPlanetInfos[i] = planetInfo;
        }
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
        drawBackground(canvas);

        if (isInEditMode()) {
            // TODO: do something?
            return;
        }
        log.info("onDraw() called...");

        if (mStar != null) {
            drawSun(canvas);
            drawPlanets(canvas);
        }
    }

    Bitmap mBackground;
    Paint mBackgroundPaint;
    private void drawBackground(Canvas canvas) {
        if (mBackground == null) {
            mBackground = BitmapFactory.decodeResource(getResources(), R.drawable.starfield);
        }
        if (mBackgroundPaint == null) {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setARGB(255, 255, 255, 255);
            mBackgroundPaint.setStyle(Style.STROKE);
        }

        canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
    }

    private Paint mSunPaint;
    private void drawSun(Canvas canvas) {
        int[] colours = { Color.YELLOW, Color.YELLOW, 0x00000000 };
        float[] positions = { 0.0f, 0.4f, 1.0f };

        if (mSunPaint == null) {
            mSunPaint = new Paint();
            mSunPaint.setDither(true);
            RadialGradient gradient = new RadialGradient(0, 0, 200, 
                    colours, positions, android.graphics.Shader.TileMode.CLAMP);
            mSunPaint.setShader(gradient);
        }

        canvas.drawCircle(0, 0, 200, mSunPaint);
    }

    Paint mPlanetPaint;
    private void drawPlanets(Canvas canvas) {
        if (mPlanetPaint == null) {
            mPlanetPaint = new Paint();
            mPlanetPaint.setARGB(255, 255, 255, 255);
            mPlanetPaint.setStyle(Style.STROKE);
        }

        for (int i = 0; i < mPlanetInfos.length; i++) {
            canvas.drawCircle(0, 0, (50*i) + 250, mPlanetPaint);
        }

        for (int i = 0; i < mPlanetInfos.length; i++) {
            final PlanetInfo planetInfo = mPlanetInfos[i];
            int resID = planetInfo.planet.getPlanetType().getMedID();
            if (resID != 0) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), resID);

                canvas.drawBitmap(bm, (float) (planetInfo.centre.getX() - (bm.getWidth()/2)),
                        (float) (planetInfo.centre.getY() - (bm.getHeight()/2)), mPlanetPaint);
            }
        }

        if (mSelectedPlanet != null) {
            Paint p2 = new Paint();
            p2.setARGB(255, 255, 255, 255);
            p2.setStyle(Style.STROKE);
            canvas.drawCircle((float) mSelectedPlanet.centre.getX(), 
                    (float) mSelectedPlanet.centre.getY(), 60, p2);
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            boolean newPlanet = false;

            Point2D tapLocation = new Point2D(e.getX(), e.getY());
            for (PlanetInfo planetInfo : mPlanetInfos) {
                if (tapLocation.distanceTo(planetInfo.centre) < 80.0) {
                    if (mSelectedPlanet != planetInfo) {
                        newPlanet = true;
                        mSelectedPlanet = planetInfo;
                    }
                }
            }
            
            if (newPlanet) {
                firePlanetSelected(mSelectedPlanet.planet);
                redraw();
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
