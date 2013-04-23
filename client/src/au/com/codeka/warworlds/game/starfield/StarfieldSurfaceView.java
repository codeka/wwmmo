package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
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
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import au.com.codeka.common.Pair;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends SectorView
                                  implements StarManager.StarFetchedHandler,
                                             SectorManager.OnSectorListChangedListener,
                                             EmpireManager.EmpireFetchedHandler {
    private static final Logger log = LoggerFactory.getLogger(StarfieldSurfaceView.class);
    private Context mContext;
    private ArrayList<OnSelectionChangedListener> mSelectionChangedListeners;
    private VisibleEntity mSelectedEntity;
    private SelectionView mSelectionView;
    private Map<String, List<VisibleEntityAttachedOverlay>> mStarAttachedOverlays;
    private Map<String, List<VisibleEntityAttachedOverlay>> mFleetAttachedOverlays;
    private Map<String, Empire> mVisibleEmpires;
    private List<VisibleEntity> mVisibleEntities;
    private Matrix mMatrix;
    private Paint mStarPaint;
    private Paint mStarNamePaint;
    private boolean mIsScrolling;
    private Handler mHandler;
    private BaseStar mHqStar;
    private Sprite mHqSprite;
    private HqDirectionOverlay mHqOverlay;

    private static Bitmap sFleetMultiBitmap;

    private ImageManager.BitmapGeneratedListener mBitmapGeneratedListener;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        log.info("Starfield initializing...");

        mContext = context;
        mSelectionChangedListeners = new ArrayList<OnSelectionChangedListener>();
        mSelectedEntity = null;
        mStarAttachedOverlays = new TreeMap<String, List<VisibleEntityAttachedOverlay>>();
        mFleetAttachedOverlays = new TreeMap<String, List<VisibleEntityAttachedOverlay>>();
        mVisibleEmpires = new TreeMap<String, Empire>();
        mVisibleEntities = new ArrayList<VisibleEntity>();
        mHandler = new Handler();

        mMatrix = new Matrix();
        mStarPaint = new Paint();
        mStarPaint.setColor(Color.WHITE);
        mStarPaint.setStyle(Style.STROKE);

        mStarNamePaint = new Paint();
        mStarNamePaint.setColor(Color.WHITE);
        mStarNamePaint.setStyle(Style.STROKE);

        if (sFleetMultiBitmap == null) {
            sFleetMultiBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fleet);
        }

        // whenever a new star bitmap is generated, redraw the screen
        mBitmapGeneratedListener = new ImageManager.BitmapGeneratedListener() {
            @Override
            public void onBitmapGenerated(String key, Bitmap bmp) {
                redraw();
            }
        };

        // disable the initial scrollTo -- you MUST call scrollTo yourself at some point!
        //scrollTo(0, 0, 0, 0);
    }

    public void setSelectionView(SelectionView selectionView) {
        mSelectionView = selectionView;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.isInEditMode()) {
            return;
        }

        SectorManager.getInstance().addSectorListChangedListener(this);
        StarImageManager.getInstance().addBitmapGeneratedListener(mBitmapGeneratedListener);
        StarManager.getInstance().addStarUpdatedListener(null, this);
        EmpireManager.getInstance().addEmpireUpdatedListener(null, this);

        MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
        if (myEmpire != null) {
            BaseStar homeStar = myEmpire.getHomeStar();
            int numHqs = BuildManager.getInstance().getTotalBuildingsInEmpire("hq");
            if (numHqs > 0) {
                mHqStar = homeStar;
            }
        }

        mHqOverlay = new HqDirectionOverlay();
        addOverlay(mHqOverlay);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.isInEditMode()) {
            return;
        }

        SectorManager.getInstance().removeSectorListChangedListener(this);
        StarImageManager.getInstance().removeBitmapGeneratedListener(mBitmapGeneratedListener);
        StarManager.getInstance().removeStarUpdatedListener(this);
        EmpireManager.getInstance().removeEmpireUpdatedListener(this);

        removeOverlay(mHqOverlay);
    }

    public void deselectStar() {
        mSelectedEntity = null;
        if (mSelectionView != null) {
            mSelectionView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSectorListChanged() {
        log.debug("Sector list has changed, redrawing.");
        super.onSectorListChanged();

        // make sure we re-select the entity we had selected before (if any)
        if (mSelectedEntity != null) {
            if (mSelectedEntity.star != null) {
                Star newSelectedStar = SectorManager.getInstance().findStar(mSelectedEntity.star.getKey());
                // if it's the same instance, that's fine
                if (newSelectedStar != mSelectedEntity.star) {
                   selectStar(newSelectedStar);
                }
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
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP && mIsScrolling) {
            mIsScrolling = false;
            placeSelection();
        }
        return true;
    }

    public void addSelectionChangedListener(OnSelectionChangedListener listener) {
        if (!mSelectionChangedListeners.contains(listener)) {
            mSelectionChangedListeners.add(listener);
        }
    }

    public void removeSelectionChangedListener(OnSelectionChangedListener listener) {
        mSelectionChangedListeners.remove(listener);
    }

    protected void fireSelectionChanged(VisibleEntity entity) {
        for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
            if (entity.star != null) {
                listener.onStarSelected(entity.star);
            } else if (entity.fleet != null) {
                listener.onFleetSelected(entity.fleet);
            }
        }
    }

    @Override
    public void addOverlay(Overlay overlay) {
        super.addOverlay(overlay);
    }

    /**
     * Adds the given \c VisibleEntityAttachedOverlay and attaches it to the given star.
     */
    public void addOverlay(VisibleEntityAttachedOverlay overlay, StarSummary starSummary) {
        addOverlay(overlay);

        List<VisibleEntityAttachedOverlay> starAttachedOverlays = mStarAttachedOverlays.get(starSummary.getKey());
        if (starAttachedOverlays == null) {
            starAttachedOverlays = new ArrayList<VisibleEntityAttachedOverlay>();
            mStarAttachedOverlays.put(starSummary.getKey(), starAttachedOverlays);
        }
        starAttachedOverlays.add(overlay);
    }

    /**
     * Adds the given \c StarAttachedOverlay and attaches it to the given fleet.
     */
    public void addOverlay(VisibleEntityAttachedOverlay overlay, Fleet fleet) {
        addOverlay(overlay);

        List<VisibleEntityAttachedOverlay> fleetAttachedOverlays = mFleetAttachedOverlays.get(fleet.getKey());
        if (fleetAttachedOverlays == null) {
            fleetAttachedOverlays = new ArrayList<VisibleEntityAttachedOverlay>();
            mFleetAttachedOverlays.put(fleet.getKey(), fleetAttachedOverlays);
        }
        fleetAttachedOverlays.add(overlay);
    }

    @Override
    public void removeOverlay(Overlay overlay) {
        super.removeOverlay(overlay);

        // we have to go through all out entity-attached overlays and make sure this one is gone
        for (List<VisibleEntityAttachedOverlay> overlays : mStarAttachedOverlays.values()) {
            overlays.remove(overlay);
        }
        for (List<VisibleEntityAttachedOverlay> overlays : mFleetAttachedOverlays.values()) {
            overlays.remove(overlay);
        }
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        // if the player's empire changes, it might mean that the location of their HQ has changed,
        // so we'll want to make sure it's still correct.
        MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
        if (empire.getKey().equals(myEmpire.getKey())) {
            mHqStar = empire.getHomeStar();
        }
    }

    public Star getSelectedStar() {
        if (mSelectedEntity == null) {
            return null;
        }
        return mSelectedEntity.star;
    }

    /**
     * Gets the \c Star that's closest to the centre of the screen.
     */
    public Star findStarInCentre() {
        Star centreStar = null;
        double distance = 0;

        Vector2 centre = Vector2.pool.borrow().reset(getWidth() / 2.0, getHeight() / 2.0);

        for (VisibleEntity entity : mVisibleEntities) {
            if (entity.star != null) {
                double thisDistance = entity.position.distanceTo(centre);
                if (centreStar == null || thisDistance < distance) {
                    centreStar = entity.star;
                    distance = thisDistance;
                }
            }
        }

        Vector2.pool.release(centre);
        return centreStar;
    }

    private void placeSelection() {
        if (mSelectionView == null || mSelectedEntity == null) {
            return;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mSelectionView.getLayoutParams();
        lp.leftMargin = (int) mSelectedEntity.position.x + getLeft() - (mSelectionView.getWidth() / 2);
        lp.topMargin = (int) mSelectedEntity.position.y + getTop() - (mSelectionView.getHeight() / 2);
        mSelectionView.setLayoutParams(lp);
        mSelectionView.setVisibility(View.VISIBLE);
    }

    private List<VisibleEntity> getVisibleEntitiesAt(int viewX, int viewY) {
        ArrayList<VisibleEntity> entities = new ArrayList<VisibleEntity>();

        Vector2 tap = Vector2.pool.borrow().reset(viewX, viewY);
        for (VisibleEntity entity : mVisibleEntities) {
            double distance = tap.distanceTo(entity.position);
            if (distance < 48.0) {
                entities.add(entity);
            }
        }
        Vector2.pool.release(tap);

        // we'll sort them by kind/ID so that they're already returned in the same order. This
        // way, we can cycle through entities when they're close and you tap multiple times.
        Collections.sort(entities, new Comparator<VisibleEntity>() {
            @Override
            public int compare(VisibleEntity lhs, VisibleEntity rhs) {
                if (lhs.star != null && rhs.star == null) {
                    return -1;
                } else if (lhs.fleet != null && rhs.fleet == null) {
                    return 1;
                } else if (lhs.star != null) {
                    return lhs.star.getKey().compareTo(rhs.star.getKey());
                } else {
                    return lhs.fleet.getKey().compareTo(rhs.fleet.getKey());
                }
            }
        });

        return entities;
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
        super.onDraw(canvas);

        mStarNamePaint.setTextSize(15.0f * getPixelScale());

        mHqOverlay.setEnabled(true);
        mVisibleEntities.clear();
        final List<Pair<Long, Long>> missingSectors = drawScene(canvas);

        drawOverlays(canvas);

        if (missingSectors != null) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }
    }

    private List<Pair<Long, Long>> drawScene(Canvas canvas) {
        SectorManager sm = SectorManager.getInstance();

        List<Pair<Long, Long>> missingSectors = null;

        for(int y = -mSectorRadius; y <= mSectorRadius; y++) {
            for(int x = -mSectorRadius; x <= mSectorRadius; x++) {
                long sX = mSectorX + x;
                long sY = mSectorY + y;

                Sector sector = sm.getSector(sX, sY);
                if (sector == null) {
                    if (missingSectors == null) {
                        missingSectors = new ArrayList<Pair<Long, Long>>();
                    }
                    missingSectors.add(new Pair<Long, Long>(sX, sY));
                    log.debug(String.format("Missing sector: %d, %d", sX, sY));
                    continue;
                }

                int sx = (int)((x * Sector.SECTOR_SIZE) + mOffsetX);
                int sy = (int)((y * Sector.SECTOR_SIZE) + mOffsetY);

                StarfieldBackgroundRenderer bgRenderer = SectorManager.getInstance().getBackgroundRenderer(
                        mContext, sector);
                bgRenderer.drawBackground(canvas, sx, sy,
                        sx+Sector.SECTOR_SIZE, sy+Sector.SECTOR_SIZE);
            }
        }

        for (int y = -mSectorRadius; y <= mSectorRadius; y++) {
            for(int x = -mSectorRadius; x <= mSectorRadius; x++) {
                long sX = mSectorX + x;
                long sY = mSectorY + y;
                Sector sector = sm.getSector(sX, sY);
                if (sector == null) {
                    continue;
                }

                int sx = (int)((x * Sector.SECTOR_SIZE) + mOffsetX);
                int sy = (int)((y * Sector.SECTOR_SIZE) + mOffsetY);
                drawSector(canvas, sx, sy, sector);
            }
        }

        return missingSectors;
    }

    /**
     * Given a \c Sector, returns the (x, y) coordinates (in view-space) of the origin of this
     * sector.
     */
    private Vector2 getSectorOffset(long sx, long sy) {
        sx -= mSectorX;
        sy -= mSectorY;
        return new Vector2((sx * Sector.SECTOR_SIZE) + mOffsetX,
                           (sy * Sector.SECTOR_SIZE) + mOffsetY);
    }

    /**
     * Draws a sector, which is a 1024x1024 area of stars.
     */
    private void drawSector(Canvas canvas, int offsetX, int offsetY, Sector sector) {
        for(BaseStar star : sector.getStars()) {
            drawStar(canvas, (Star) star, offsetX, offsetY);
        }
        for(BaseStar star : sector.getStars()) {
            drawStarName(canvas, (Star) star, offsetX, offsetY);
        }
        for (BaseStar star : sector.getStars()) {
            for (BaseFleet fleet : star.getFleets()) {
                if (fleet.getState() == Fleet.State.MOVING) {
                    drawMovingFleet(canvas, (Fleet) fleet, (Star) star, offsetX, offsetY);
                }
            }
        }
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStar(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();
        final float pixelScale = getPixelScale();

        // only draw the star if it's actually visible...
        if (isVisible(canvas, (x - 100) * pixelScale, (y - 100) * pixelScale,
                              (x + 100) * pixelScale, (y + 100) * pixelScale)) {

            float imageScale = (float) star.getStarType().getImageScale();
            int imageSize = (int)(star.getSize() * imageScale * 2);
            Sprite sprite = StarImageManager.getInstance().getSprite(mContext, star, imageSize);
            mMatrix.reset();
            mMatrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
            mMatrix.postScale(40.0f * imageScale * pixelScale / sprite.getWidth(),
                              40.0f * imageScale * pixelScale / sprite.getHeight());
            mMatrix.postTranslate(x * pixelScale, y * pixelScale);
            canvas.save();
            canvas.concat(mMatrix);
            sprite.draw(canvas);
            canvas.restore();

            drawStarIcons(canvas, star, x, y);
            if (mHqStar != null && star.getKey().equals(mHqStar.getKey())) {
                if (mHqSprite == null) {
                    mHqSprite = SpriteManager.i.getSprite("building.hq");
                }

                mMatrix.reset();
                mMatrix.postTranslate(-(mHqSprite.getWidth() / 2.0f), -(mHqSprite.getHeight() / 2.0f));
                mMatrix.postScale(40.0f * imageScale * pixelScale / mHqSprite.getWidth(),
                                  40.0f * imageScale * pixelScale / mHqSprite.getHeight());
                mMatrix.postTranslate(x * pixelScale, (y * pixelScale) + sprite.getHeight());
                canvas.save();
                canvas.concat(mMatrix);
                mHqSprite.draw(canvas);
                canvas.restore();

                mHqOverlay.setEnabled(false);
            }

            List<VisibleEntityAttachedOverlay> overlays = mStarAttachedOverlays.get(star.getKey());
            if (overlays != null && !overlays.isEmpty()) {
                int n = overlays.size();
                for (int i = 0; i < n; i++) {
                    VisibleEntityAttachedOverlay sao = overlays.get(i);
                    sao.setCentre(x * pixelScale, y * pixelScale);
                }
            }

            VisibleEntity ve = new VisibleEntity(new Vector2(x * pixelScale, y * pixelScale), star);
            if (mSelectedEntity != null && mSelectedEntity.star != null && mSelectedEntity.star.getKey().equals(star.getKey())) {
                mSelectedEntity = ve;
            }
            mVisibleEntities.add(ve);
        }
    }

    /**
     * Gets an \c Empire given it's key.
     */
    private Empire getEmpire(String empireKey) {
        if (empireKey == null) {
            return EmpireManager.getInstance().getNativeEmpire();
        }

        Empire emp = mVisibleEmpires.get(empireKey);
        if (emp == null) {
            EmpireManager.getInstance().fetchEmpire(mContext, empireKey, new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    mVisibleEmpires.put(empire.getKey(), empire);
                    redraw();
                }
            });
        }
        return emp;
    }

    private void drawStarIcons(Canvas canvas, Star star, int x, int y) {
        final float pixelScale = getPixelScale();

        List<BaseColony> colonies = star.getColonies();
        if (colonies != null && !colonies.isEmpty()) {
            Map<String, Integer> colonyEmpires = new TreeMap<String, Integer>();

            for (int i = 0; i < colonies.size(); i++) {
                BaseColony colony = colonies.get(i);
                if (colony.getEmpireKey() == null) {
                    continue;
                }

                Empire emp = getEmpire(colony.getEmpireKey());
                if (emp != null) {
                    Integer n = colonyEmpires.get(emp.getKey());
                    if (n == null) {
                        n = 1;
                        colonyEmpires.put(emp.getKey(), n);
                    } else {
                        colonyEmpires.put(emp.getKey(), n+1);
                    }
                }
            }

            int i = 1;
            for (String empireKey : colonyEmpires.keySet()) {
                Integer n = colonyEmpires.get(empireKey);
                Empire emp = mVisibleEmpires.get(empireKey);

                Bitmap bmp = emp.getShield(mContext);

                Vector2 pt = Vector2.pool.borrow().reset(0, -25.0f);
                pt.rotate((float)(Math.PI / 4.0) * i);
                pt.add(x, y);

                mMatrix.reset();
                mMatrix.postTranslate(-(bmp.getWidth() / 2.0f), -(bmp.getHeight() / 2.0f));
                mMatrix.postScale(16.0f * pixelScale / bmp.getWidth(),
                                  16.0f * pixelScale / bmp.getHeight());
                mMatrix.postTranslate((float) pt.x * pixelScale, (float) pt.y * pixelScale);
                canvas.drawBitmap(bmp, mMatrix, mStarPaint);

                String name;
                if (n.equals(1)) {
                    name = emp.getDisplayName();
                } else {
                    name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), n);
                }

                Rect bounds = new Rect();
                mStarPaint.getTextBounds(name, 0, name.length(), bounds);
                float textHeight = bounds.height();

                canvas.drawText(name,
                                (float) (pt.x + 12) * pixelScale,
                                (float) (pt.y + 8) * pixelScale - (textHeight / 2),
                                mStarPaint);
                Vector2.pool.release(pt); pt = null;
                i++;
            }
        }

        List<BaseFleet> fleets = star.getFleets();
        if (fleets != null && !fleets.isEmpty()) {
            Map<String, Integer> empireFleets = new TreeMap<String, Integer>();
            for (int i = 0; i < fleets.size(); i++) {
                BaseFleet f = fleets.get(i);
                if (f.getState() == Fleet.State.MOVING) {
                    // ignore moving fleets, we'll draw them separately
                    continue;
                }

                Integer n = empireFleets.get(f.getEmpireKey());
                if (n == null) {
                    empireFleets.put(f.getEmpireKey(), (int) Math.ceil(f.getNumShips()));
                } else {
                    empireFleets.put(f.getEmpireKey(), n + (int) Math.ceil(f.getNumShips()));
                }
            }

            int i = 0;
            for (String empireKey : empireFleets.keySet()) {
                Integer numShips = empireFleets.get(empireKey);
                Empire emp = getEmpire(empireKey);
                if (emp != null) {
                    Vector2 pt = Vector2.pool.borrow().reset(0, -25.0f);
                    pt.rotate((float)(Math.PI / 4.0) * -i);
                    pt.add(x, y);

                    mMatrix.reset();
                    mMatrix.postTranslate(-(sFleetMultiBitmap.getWidth() / 2.0f),
                                          -(sFleetMultiBitmap.getHeight() / 2.0f));
                    mMatrix.postScale(16.0f * pixelScale / sFleetMultiBitmap.getWidth(),
                                      16.0f * pixelScale / sFleetMultiBitmap.getHeight());
                    mMatrix.postTranslate((float) pt.x * pixelScale, (float) pt.y * pixelScale);
                    canvas.drawBitmap(sFleetMultiBitmap, mMatrix, mStarPaint);

                    String name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), numShips);

                    Rect bounds = new Rect();
                    mStarPaint.getTextBounds(name, 0, name.length(), bounds);
                    float textHeight = bounds.height();
                    float textWidth = bounds.width();

                    canvas.drawText(name,
                                    (float) (pt.x - 12) * pixelScale - textWidth,
                                    (float) (pt.y + 8) * pixelScale - (textHeight / 2),
                                    mStarPaint);
                    Vector2.pool.release(pt); pt = null;
                }

                i++;
            }

        }
    }

    /**
     * Draw a moving fleet as a line between the source and destination stars, with an icon
     * representing the current location of the fleet.
     */
    private void drawMovingFleet(Canvas canvas, Fleet fleet, Star srcStar, int offsetX, int offsetY) {
        float pixelScale = getPixelScale();

        // we'll need to find the destination star
        Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
        if (destStar == null) {
            // the destination star isn't in one of the sectors we have in memory, we'll
            // just ignore this fleet (it's probably flying off the edge of the sector and our
            // little viewport won't see it anyway -- unless you've got a REALLY long-range
            // flight, maybe we can stop that from being possible).
            return;
        }

        Vector2 srcPoint = Vector2.pool.borrow().reset(offsetX, offsetY);
        srcPoint.x += srcStar.getOffsetX();
        srcPoint.y += srcStar.getOffsetY();

        Vector2 destPoint = getSectorOffset(destStar.getSectorX(), destStar.getSectorY());
        destPoint.x += destStar.getOffsetX();
        destPoint.y += destStar.getOffsetY();

        // work out how far along the fleet has moved so we can draw the icon at the correct
        // spot. Also, we'll draw the name of the empire, number of ships etc.
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        double distance = srcPoint.distanceTo(destPoint);
        double totalTimeInHours = (distance / 10.0) / design.getSpeedInParsecPerHour();

        DateTime startTime = fleet.getStateStartTime();
        DateTime now = DateTime.now(DateTimeZone.UTC);
        float timeSoFarInHours = Seconds.secondsBetween(startTime, now).getSeconds() / 3600.0f;

        double fractionComplete = timeSoFarInHours / totalTimeInHours;
        if (fractionComplete > 1.0) {
            fractionComplete = 1.0;
        }

        // we don't want to start the fleet over the top of the star, so we'll offset it a bit
        distance -= 40.0f;
        if (distance < 0) {
            distance = 0;
        }

        Vector2 direction = Vector2.pool.borrow().reset(destPoint);
        direction.subtract(srcPoint);
        direction.normalize();

        Vector2 location = Vector2.pool.borrow().reset(direction);
        location.scale(distance * fractionComplete);
        location.add(srcPoint);
        Vector2.pool.release(srcPoint); srcPoint = null;

        Sprite fleetSprite = SpriteManager.i.getSprite(design.getSpriteName());
        Vector2 up = fleetSprite.getUp();

        float angle = Vector2.angleBetween(up, direction);

        direction.scale(20.0f);
        location.add(direction);
        Vector2.pool.release(direction); direction = null;

        Vector2 position = new Vector2(location.x * pixelScale, location.y * pixelScale);
        Vector2.pool.release(location); location = null;

        // check if there's any other fleets nearby and offset this one by a bit so that they
        // don't overlap
        Random rand = new Random(fleet.getKey().hashCode());
        for (int i = 0; i < mVisibleEntities.size(); i++) {
            VisibleEntity existing = mVisibleEntities.get(i);
            if (existing.fleet == null) {
                continue;
            }

            if (existing.position.distanceTo(position) < (15.0f * pixelScale)) {
                // pick a random direction and offset it a bit
                Vector2 offset = Vector2.pool.borrow().reset(0, 20.0 * pixelScale);
                offset.rotate(rand.nextFloat() * 2 * (float) Math.PI);
                position.add(offset);
                Vector2.pool.release(offset);
                i = -1; // start looping again...
            }
        }

        // record the fact that this guy is visible
        VisibleEntity ve = new VisibleEntity(position, fleet);
        if (mSelectedEntity != null && mSelectedEntity.fleet != null && mSelectedEntity.fleet.getKey().equals(fleet.getKey())) {
            mSelectedEntity = ve;
        }
        mVisibleEntities.add(ve);

        // scale zoom and rotate the bitmap all with one matrix
        mMatrix.reset();
        mMatrix.postTranslate(-(fleetSprite.getWidth() / 2.0f),
                              -(fleetSprite.getHeight() / 2.0f));
        mMatrix.postScale(20.0f * pixelScale / fleetSprite.getWidth(),
                          20.0f * pixelScale / fleetSprite.getWidth());
        mMatrix.postRotate((float) (angle * 180.0 / Math.PI));
        mMatrix.postTranslate((float) position.x, (float) position.y);
        canvas.save();
        canvas.concat(mMatrix);
        fleetSprite.draw(canvas);
        canvas.restore();

        Empire emp = getEmpire(fleet.getEmpireKey());
        if (emp != null) {
            Bitmap shield = emp.getShield(mContext);
            if (shield != null) {
                mMatrix.reset();
                mMatrix.postTranslate(-(shield.getWidth() / 2.0f), -(shield.getHeight() / 2.0f));
                mMatrix.postScale(16.0f * pixelScale / shield.getWidth(),
                                  16.0f * pixelScale / shield.getHeight());
                mMatrix.postTranslate((float) position.x + (20.0f * pixelScale),
                                      (float) position.y);
                canvas.drawBitmap(shield, mMatrix, mStarPaint);
            }

            String msg = emp.getDisplayName();
            canvas.drawText(msg, (float) position.x + (30.0f * pixelScale),
                            (float) position.y, mStarPaint);

            msg = String.format(Locale.ENGLISH, "%s (%d)", design.getDisplayName(), (int) Math.ceil(fleet.getNumShips()));
            canvas.drawText(msg, (float) position.x + (30.0f * pixelScale),
                            (float) position.y + (10.0f * pixelScale), mStarPaint);
        }

        List<VisibleEntityAttachedOverlay> fleetAttachedOverlays = mFleetAttachedOverlays.get(fleet.getKey());
        if (fleetAttachedOverlays != null && !fleetAttachedOverlays.isEmpty()) {
            int n = fleetAttachedOverlays.size();
            for (int i = 0; i < n; i++) {
                VisibleEntityAttachedOverlay sao = fleetAttachedOverlays.get(i);
                sao.setCentre(position.x, position.y);
            }
        }
    }

    /**
     * Determines whether any part of the given \c Rect will be visible on the given canvas,
     * not looking at the clip rect, just looking at the bounds of the canvas.
     */
    private static boolean isVisible(Canvas canvas, float left, float top, float right, float bottom) {
        if (right < 0 || bottom < 0) {
            return false;
        }
        if (left > canvas.getWidth() || top > canvas.getHeight()) {
            return false;
        }
        return true;
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStarName(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        final float pixelScale = getPixelScale();

        float width = mStarNamePaint.measureText(star.getName()) / pixelScale;
        x -= (width / 2.0f);
        y += star.getSize() + 10.0f;

        canvas.drawText(star.getName(),
                        x * pixelScale,
                        y * pixelScale,
                        mStarNamePaint);
    }

    public void selectStar(String starKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        selectStar(star);
    }

    private void selectStar(Star star) {
        if (star != null) {
            log.info("Selecting star: "+star.getKey());
            mSelectedEntity = null;
            for (VisibleEntity entity : mVisibleEntities) {
                if (entity.star != null && entity.star.getKey().equals(star.getKey())) {
                    selectEntity(entity);
                    break;
                }
            }
        }
    }

    public void selectFleet(BaseFleet fleet) {
        if (fleet != null && fleet.getState() == Fleet.State.MOVING) {
            log.info("Selecting fleet: "+fleet.getKey());
            mSelectedEntity = null;
            for (VisibleEntity entity : mVisibleEntities) {
                if (entity.fleet != null && entity.fleet.getKey().equals(fleet.getKey())) {
                    selectEntity(entity);
                    break;
                }
            }
        }
    }

    /**
     * Selected a fleet or star.
     */
    public void selectEntity(VisibleEntity entity) {
        if (entity.fleet != null) {
            if (entity.fleet.getState() != Fleet.State.MOVING) {
                return;
            }
        }

        mSelectedEntity = entity;
        if (mSelectionView != null && mSelectedEntity != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mSelectionView.getLayoutParams();
            lp.width = 40;
            lp.height = 40;
            if (mSelectedEntity.star != null) {
                lp.width = (int)(mSelectedEntity.star.getSize() * 2 * getPixelScale());
                lp.height = (int)(mSelectedEntity.star.getSize() * 2 * getPixelScale());
            }
            mSelectionView.setLayoutParams(lp);
            mSelectionView.setVisibility(View.VISIBLE);
        }

        redraw();
        fireSelectionChanged(entity);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                placeSelection();
            }
        });
    }

    /**
     * When a star is updated, if it's one of ours, then we'll want to redraw to make sure we
     * have the latest data (e.g. it might've been renamed)
     */
    @Override
    public void onStarFetched(Star s) {
        boolean needRedraw = false;
        for (VisibleEntity entity : mVisibleEntities) {
            if (entity.star != null && entity.star.getKey().equals(s.getKey())) {
                needRedraw = true;
            }
        }
        if (needRedraw) {
            redraw();
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends SectorView.GestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            super.onScroll(e1, e2, distanceX, distanceY);

            if (mSelectionView != null) {
                mSelectionView.setVisibility(View.GONE);
            }
            mIsScrolling = true;

            return false;
        }

        /**
         * We need to check whether you tapped on a star, and if so, we'll display some
         * basic info about the star on the status pane. Actually, we'll just fire the
         * "StarSelect" event and let the \c StarfieldActivity do that.
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int tapX = (int) e.getX();
            int tapY = (int) e.getY();

            List<VisibleEntity> tappedEntities = getVisibleEntitiesAt(tapX, tapY);
            if (tappedEntities.size() == 0) {
                return false;
            }

            int n;
            for (n = 0; n < tappedEntities.size(); n++) {
                if (mSelectedEntity != null && mSelectedEntity == tappedEntities.get(n)) {
                    break;
                }
            }

            VisibleEntity nextEntity = tappedEntities.get(0);
            if (n < tappedEntities.size() - 1) {
                nextEntity = tappedEntities.get(n + 1);
            }

            if (nextEntity.star != null) {
                selectStar(nextEntity.star);
            } else {
                selectFleet(nextEntity.fleet);
            }

            // play the 'click' sound effect
            playSoundEffect(android.view.SoundEffectConstants.CLICK);

            return false;
        }
    }

    /**
     * Every time we draw the screen, we track the visible entities to make tracking when you
     * tap on them simpler. This class holds a reference to a single "visible" entity.
     */
    private static class VisibleEntity {
        public Vector2 position;
        public Star star;
        public Fleet fleet;

        public VisibleEntity(Vector2 position, Star star) {
            this.position = position;
            this.star = star;
        }

        public VisibleEntity(Vector2 position, Fleet fleet) {
            this.position = position;
            this.fleet = fleet;
        }
    }

    private class HqDirectionOverlay extends Overlay {
        private Paint mPaint;
        private boolean mEnabled;

        public HqDirectionOverlay() {
            mPaint = new Paint();
            mPaint.setStyle(Style.FILL);
            mPaint.setARGB(255, 0, 255, 0);
        }

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        @Override
        public void draw(Canvas canvas) {
            if (!mEnabled || mHqStar == null) {
                return;
            }

            Vector2 centre = Vector2.pool.borrow().reset(getWidth() / 2.0, getHeight() / 2.0);

            // we want to trace a line from the centre of the screen to the HQ star
            long sectorX = mHqStar.getSectorX() - mSectorX;
            long sectorY = mHqStar.getSectorY() - mSectorY;
            Vector2 starDirection = Vector2.pool.borrow().reset((double) sectorX, (double) sectorY);
            starDirection.scale(Sector.SECTOR_SIZE);
            starDirection.add(mHqStar.getOffsetX() + mOffsetX, mHqStar.getOffsetY() + mOffsetY);
            starDirection.scale(getPixelScale());

            // normalize the starDirection so it's actually the DIRECTION from the centre
            // to the star
            starDirection.add((float) -centre.x, (float) -centre.y);
            starDirection.normalize();

            // OK, so imagine a line from centre in the direction of starDirection. We want to
            // find the point at which it crosses the edge of the screen (i.e. when X=0 or WIDTH,
            // or when Y=0 or HEIGHT).
            // First, convert to y=mx+b form (though b is always zero)
            double m = starDirection.y / starDirection.x;

            // We know if Y is +ve then it will cross the bottom edge, if it's -ve then it'll cross
            // the top edge. Find out the X-coordinate at which that happens
            double y = (starDirection.y > 0 ? 1.0 : -1.0) * (getHeight() / 2.0);
            double x = y / m;
            if ((x + centre.x) < 0 || (x + centre.x) > getWidth()) {
                // if X is out the range (0..WIDTH) then it must hit the vertical edge before it
                // hits the horizontal edge
                x = (starDirection.x > 0 ? 1.0 : -1.0) * (getWidth() / 2.0);
                y = x * m;
            }
            Vector2 edge = Vector2.pool.borrow().reset(x, y);
            edge.add(centre);

            Vector2 pt = Vector2.pool.borrow().reset(edge.x - (starDirection.x * 25.0),
                                                     edge.y - (starDirection.y * 25.0));

            // rotate 90 degrees...
            starDirection.reset(starDirection.y, -starDirection.x);
            Vector2 pt1 = Vector2.pool.borrow().reset(pt.x + (starDirection.x * 10.0),
                                                      pt.y + (starDirection.y * 10.0));
            Vector2 pt2 = Vector2.pool.borrow().reset(pt.x - (starDirection.x * 10.0),
                                                      pt.y - (starDirection.y * 10.0));

            Path p = new Path();
            p.moveTo((float) edge.x, (float) edge.y);
            p.lineTo((float) pt1.x, (float) pt1.y);
            p.lineTo((float) pt2.x, (float) pt2.y);
            p.close();
            canvas.drawPath(p, mPaint);

            Vector2.pool.release(centre);
            Vector2.pool.release(pt);
            Vector2.pool.release(pt1);
            Vector2.pool.release(pt2);
            Vector2.pool.release(starDirection);
            Vector2.pool.release(edge);
        }
    }

    public interface OnSelectionChangedListener {
        public abstract void onStarSelected(Star star);
        public abstract void onFleetSelected(Fleet fleet);
    }
}
