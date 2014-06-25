package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
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
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
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
public class SolarSystemSurfaceView extends UniverseElementSurfaceView
                                    implements EmpireShieldManager.ShieldUpdatedHandler {
    private Context mContext;
    private Star mStar;
    private PlanetInfo[] mPlanetInfos;
    private PlanetInfo mSelectedPlanet;
    private Paint mPlanetPaint;
    private SelectionView mSelectionView;
    private CopyOnWriteArrayList<OnPlanetSelectedListener> mPlanetSelectedListeners;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private Matrix mMatrix;

    private Comparator<Building> mBuildingDesignComparator = new Comparator<Building>() {
        @Override
        public int compare(Building lhs, Building rhs) {
            return lhs.getDesignID().compareTo(rhs.getDesignID());
        }
    };

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
    }

    public void setSelectionView(SelectionView selectionView) {
        mSelectionView = selectionView;
        if (mSelectionView != null) {
            mSelectionView.setVisibility(View.GONE);
        }
    }

    public void setStar(Star star) {
        mStar = star;

        BasePlanet[] planets = mStar.getPlanets();
        mPlanetInfos = new PlanetInfo[planets.length];

        for (int i = 0; i < planets.length; i++) {
            PlanetInfo planetInfo = new PlanetInfo();
            planetInfo.planet = (Planet) planets[i];
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

            List<BaseColony> colonies = mStar.getColonies();
            if (colonies != null && !colonies.isEmpty()) {
                for (BaseColony colony : colonies) {
                    if (colony.getPlanetIndex() == mPlanetInfos[i].planet.getIndex()) {
                        planetInfo.colony = (Colony) colony;
                        planetInfo.buildings = new ArrayList<Building>();

                        for (BaseBuilding building : colony.getBuildings()) {
                            BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING, building.getDesignID());
                            if (design.showInSolarSystem()) {
                                planetInfo.buildings.add((Building) building);
                            }
                        }

                        if (!planetInfo.buildings.isEmpty()) {
                            Collections.sort(planetInfo.buildings, mBuildingDesignComparator);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EmpireShieldManager.i.addShieldUpdatedHandler(this);
        ImageManager.eventBus.register(mEventHandler);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ImageManager.eventBus.unregister(mEventHandler);
        if (mBackgroundRenderer != null) {
            mBackgroundRenderer.close();
            mBackgroundRenderer = null;
        }
        EmpireShieldManager.i.removeShieldUpdatedHandler(this);
    }

    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (mPlanetInfos == null) {
            return;
        }
        placePlanets();
    }

    /** Called when an empire's shield is updated, we'll have to refresh the view. */
    @Override
    public void onShieldUpdated(int empireID) {
        invalidate();
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
                mBackgroundRenderer = new StarfieldBackgroundRenderer(new long[] {mStar.getKey().hashCode()});
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
        Sprite sprite = StarImageManager.getInstance().getSprite(mStar, imageSize, false);

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

            Sprite sprite = pim.getSprite(planetInfo.planet);
            mMatrix.reset();
            mMatrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
            mMatrix.postScale(100.0f * getPixelScale() / sprite.getWidth(),
                              100.0f * getPixelScale() / sprite.getHeight());
            mMatrix.postTranslate((float) planetInfo.centre.x, (float) planetInfo.centre.y);
            canvas.save();
            canvas.concat(mMatrix);
            sprite.draw(canvas);
            canvas.restore();

            if (planetInfo.buildings != null) {
                int j = 0;
                float angleOffset = (float)(Math.PI / 4.0) * (planetInfo.buildings.size() - 1) / 2.0f;
                for (Building building : planetInfo.buildings) {
                    BuildingDesign design = building.getDesign();
                    Sprite buildingSprite = SpriteManager.i.getSprite(design.getSpriteName());
    
                    Vector2 pt = Vector2.pool.borrow().reset(0, -30.0f);
                    pt.rotate(angleOffset - (float)(Math.PI / 4.0) * j);
    
    
                    mMatrix.reset();
                    mMatrix.postTranslate(-(buildingSprite.getWidth() / 2.0f), -(buildingSprite.getHeight() / 2.0f));
                    mMatrix.postScale(20.0f * getPixelScale() / buildingSprite.getWidth(),
                                      20.0f * getPixelScale() / buildingSprite.getHeight());
                    mMatrix.postTranslate((float) (planetInfo.centre.x + (pt.x * getPixelScale())),
                                          (float) (planetInfo.centre.y + (pt.y * getPixelScale())));
    
                    canvas.save();
                    canvas.concat(mMatrix);
                    buildingSprite.draw(canvas);
                    canvas.restore();
    
                    j++;
                }
            }

            if (planetInfo.colony != null) {
                Empire empire = EmpireManager.i.getEmpire(planetInfo.colony.getEmpireKey());
                if (empire != null) {
                    Bitmap shield = EmpireShieldManager.i.getShield(mContext, empire);
                    if (shield != null) {
                        mMatrix.reset();
                        mMatrix.postTranslate(-shield.getWidth() / 2.0f, -shield.getHeight() / 2.0f);
                        mMatrix.postScale(20.0f * getPixelScale() / shield.getWidth(),
                                          20.0f * getPixelScale() / shield.getHeight());
                        mMatrix.postTranslate((float) planetInfo.centre.x,
                                              (float) planetInfo.centre.y + (30.0f * getPixelScale()));
                        canvas.drawBitmap(shield, mMatrix, mPlanetPaint);
                    }
                }
            }
        }
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
            redraw();
        }
    };

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
        public Colony colony;
        public ArrayList<Building> buildings;
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
