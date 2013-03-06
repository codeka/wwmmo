package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import au.com.codeka.common.Vector2;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

/**
 * \c SurfaceView that displays a solar system. Star in the top-left, planets arrayed around,
 * and representations of the fleets, etc.
 */
public class SolarSystemSurfaceView extends UniverseElementSurfaceView {
    private Context mContext;
    private Star mStar;
    private PlanetInfo[] mPlanetInfos;
    private PlanetInfo mSelectedPlanet;
    private Paint mPlanetPaint;
    private SelectionView mSelectionView;
    private Bitmap mColonyIcon;
    private CopyOnWriteArrayList<OnPlanetSelectedListener> mPlanetSelectedListeners;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private BitmapGeneratedListener mBitmapGeneratedListener;
    private Matrix mMatrix;

    public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mContext = context;
        mPlanetSelectedListeners = new CopyOnWriteArrayList<OnPlanetSelectedListener>();

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
            planetInfo.centre = new Vector2(0, 0);
            planetInfo.distanceFromSun = 0.0f;
            mPlanetInfos[i] = planetInfo;
        }

        placePlanets();
        redraw();
    }

    /**
     * Gets a \c Point2D representing the centre of the given planet, relative to this
     * \c SolarSystemSurfaceView in device pixels.
     */
    public Vector2 getPlanetCentre(Planet planet) {
        if (mPlanetInfos == null) {
            return null;
        }

        for(PlanetInfo planetInfo : mPlanetInfos) {
            if (planetInfo.planet.getIndex() == planet.getIndex()) {
                Vector2 pixels = planetInfo.centre;
                return new Vector2(pixels.x / getPixelScale(),
                                   pixels.y / getPixelScale());
            }
        }

        return null;
    }

    private void placePlanets() {
        int width = getWidth();

        float planetStart = 150 * getPixelScale();
        float distanceBetweenPlanets = width - planetStart;
        distanceBetweenPlanets /= mPlanetInfos.length;

        for (int i = 0; i < mPlanetInfos.length; i++) {
            PlanetInfo planetInfo = mPlanetInfos[i];

            float distanceFromSun = planetStart + (distanceBetweenPlanets * i) + (distanceBetweenPlanets / 2.0f);
            float x = 0;
            float y = -1 * distanceFromSun;

            float angle = (0.5f/(mPlanetInfos.length + 1));
            angle = (float) ((angle*i*Math.PI) + angle*Math.PI);

            Vector2 centre = new Vector2(x, y);
            centre.rotate(angle);
            centre.y *= -1;

            planetInfo.centre = centre;
            planetInfo.distanceFromSun = distanceFromSun;

            List<Colony> colonies = mStar.getColonies();
            if (colonies != null && !colonies.isEmpty()) {
                for (Colony colony : colonies) {
                    if (colony.getPlanetIndex() == mPlanetInfos[i].planet.getIndex()) {
                        planetInfo.hasColony = true;

                        for (Building building : colony.getBuildings()) {
                            if (building.getDesignName().equals("hq")) {
                                planetInfo.hasHQ = true;
                            }
                        }
                    }
                }
            }

            mPlanetInfos[i] = planetInfo;
        }

        updateSelection();
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

                firePlanetSelected(mSelectedPlanet.planet);

                if (mSelectedPlanet != null && mSelectionView != null) {
                    updateSelection();
                } else if (mSelectionView != null) {
                    mSelectionView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void updateSelection() {
        if (mSelectedPlanet != null && mSelectionView != null) {
            mSelectionView.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSelectionView.getLayoutParams();
            params.width = (int) ((((mSelectedPlanet.planet.getSize() - 10.0) / 8.0) + 4.0) * 10.0) + (int) (40 * getPixelScale());
            params.height = params.width;
            params.leftMargin = (int) (getLeft() + mSelectedPlanet.centre.x - (params.width / 2));
            params.topMargin = (int) (getTop() + mSelectedPlanet.centre.y - (params.height / 2));
            mSelectionView.setLayoutParams(params);
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

    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (mPlanetInfos == null) {
            return;
        }
        placePlanets();
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

            drawSun(canvas);
            drawPlanets(canvas);
        }

        drawOverlays(canvas);
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
        canvas.concat(mMatrix);
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
            mMatrix.postTranslate((float) planetInfo.centre.x, (float) planetInfo.centre.y);
            canvas.save();
            canvas.concat(mMatrix);
            sprite.draw(canvas);
            canvas.restore();

            if (planetInfo.hasHQ) {
                Sprite hqSprite = SpriteManager.getInstance().getSprite("building.hq"); // todo: hardcoded?

                mMatrix.reset();
                mMatrix.postTranslate(-(hqSprite.getWidth() / 2.0f), -(hqSprite.getHeight() / 2.0f));
                mMatrix.postScale(30.0f * getPixelScale() / hqSprite.getWidth(),
                                  30.0f * getPixelScale() / hqSprite.getHeight());
                mMatrix.postTranslate((float) planetInfo.centre.x,
                                      (float) planetInfo.centre.y - (30.0f * getPixelScale()));

                canvas.save();
                canvas.concat(mMatrix);
                hqSprite.draw(canvas);
                canvas.restore();
            }

            if (planetInfo.hasColony) {
                canvas.drawBitmap(mColonyIcon,
                        (float) (planetInfo.centre.x + mColonyIcon.getWidth()),
                        (float) (planetInfo.centre.y - mColonyIcon.getHeight()),
                        mPlanetPaint);
            }
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

            Vector2 tapLocation = Vector2.pool.borrow().reset(e.getX(), e.getY());
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

            Vector2.pool.release(tapLocation);
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
        public Vector2 centre;
        public float distanceFromSun;
        public boolean hasColony;
        public boolean hasHQ;
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
