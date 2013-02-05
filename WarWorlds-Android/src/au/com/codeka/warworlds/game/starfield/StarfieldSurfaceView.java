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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.Pair;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends UniverseElementSurfaceView {
    private static final Logger log = LoggerFactory.getLogger(StarfieldSurfaceView.class);
    private Context mContext;
    private ArrayList<OnSelectionChangedListener> mSelectionChangedListeners;
    private Star mSelectedStar;
    private Fleet mSelectedFleet;
    private Paint mStarPaint;
    private SelectionOverlay mSelectionOverlay;
    private Map<String, List<VisibleEntityAttachedOverlay>> mStarAttachedOverlays;
    private Map<String, List<VisibleEntityAttachedOverlay>> mFleetAttachedOverlays;
    private Map<String, Empire> mVisibleEmpires;
    private List<VisibleEntity> mVisibleEntities;
    private boolean mScrollToCentre = false;
    private Handler mHandler;

    private boolean mNeedRedraw = true;
    private Bitmap mBuffer;

    private static Bitmap sFleetMultiBitmap;
    private static final int sRadius = 1;

    private long mSectorX;
    private long mSectorY;
    private float mOffsetX;
    private float mOffsetY;

    private SectorManager.OnSectorListChangedListener mSectorListChangedListener;
    private ImageManager.BitmapGeneratedListener mBitmapGeneratedListener;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        log.info("Starfield initializing...");

        mContext = context;
        mSelectionChangedListeners = new ArrayList<OnSelectionChangedListener>();
        mSelectedStar = null;
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
        mStarAttachedOverlays = new TreeMap<String, List<VisibleEntityAttachedOverlay>>();
        mFleetAttachedOverlays = new TreeMap<String, List<VisibleEntityAttachedOverlay>>();
        mVisibleEmpires = new TreeMap<String, Empire>();
        mVisibleEntities = new ArrayList<VisibleEntity>();
        mHandler = new Handler();

        mSelectionOverlay = new SelectionOverlay();

        mStarPaint = new Paint();
        mStarPaint.setARGB(255, 255, 255, 255);
        mStarPaint.setStyle(Style.STROKE);

        if (sFleetMultiBitmap == null) {
            sFleetMultiBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fleet);
        }

        // whenever the sector list changes (i.e. when we've refreshed from the server),
        // redraw the screen.
        mSectorListChangedListener = new SectorManager.OnSectorListChangedListener() {
            @Override
            public void onSectorListChanged() {
                log.debug("Sector list has changed, redrawing.");

                // make sure we re-select the star we had selected before (if any)
                if (mSelectedStar != null) {
                    Star newSelectedStar = SectorManager.getInstance().findStar(mSelectedStar.getKey());
                    // if it's the same instance, that's fine
                    if (newSelectedStar != mSelectedStar) {
                        selectStar(newSelectedStar);
                    }
                }

                setDirty();
                redraw();
            }
        };

        // whenever a new star bitmap is generated, redraw the screen
        mBitmapGeneratedListener = new ImageManager.BitmapGeneratedListener() {
            @Override
            public void onBitmapGenerated(String key, Bitmap bmp) {
                setDirty();
                redraw();
            }
        };

        // disable the initial scrollTo -- you MUST call scrollTo yourself
        // at some point!
        //scrollTo(0, 0, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SectorManager.getInstance().addSectorListChangedListener(mSectorListChangedListener);
        StarImageManager.getInstance().addBitmapGeneratedListener(mBitmapGeneratedListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SectorManager.getInstance().removeSectorListChangedListener(mSectorListChangedListener);
        StarImageManager.getInstance().removeBitmapGeneratedListener(mBitmapGeneratedListener);
    }

    public void deselectStar() {
        mSelectedStar = null;
        removeOverlay(mSelectionOverlay);
    }

    /**
     * Creates the \c OnGestureListener that'll handle our gestures.
     */
    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    public void addSelectionChangedListener(OnSelectionChangedListener listener) {
        if (!mSelectionChangedListeners.contains(listener)) {
            mSelectionChangedListeners.add(listener);
        }
    }

    public void removeSelectionChangedListener(OnSelectionChangedListener listener) {
        mSelectionChangedListeners.remove(listener);
    }

    protected void fireSelectionChanged(Star star) {
        for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
            listener.onStarSelected(star);
        }
    }

    protected void fireSelectionChanged(Fleet fleet) {
        for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
            listener.onFleetSelected(fleet);
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

    public Star getSelectedStar() {
        return mSelectedStar;
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
            if (mBuffer != null) {
                mOffsetX += mBuffer.getWidth() / 2.0f / getPixelScale();
                mOffsetY += mBuffer.getHeight() / 2.0f / getPixelScale();
            } else {
                mScrollToCentre = true;
            }
        }

        List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();

        for(sectorY = mSectorY - sRadius; sectorY <= mSectorY + sRadius; sectorY++) {
            for(sectorX = mSectorX - sRadius; sectorX <= mSectorX + sRadius; sectorX++) {
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

        setDirty();
        redraw();
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
        while (mOffsetX < -(SectorManager.SECTOR_SIZE / 2)) {
            mOffsetX += SectorManager.SECTOR_SIZE;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > (SectorManager.SECTOR_SIZE / 2)) {
            mOffsetX -= SectorManager.SECTOR_SIZE;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -(SectorManager.SECTOR_SIZE / 2)) {
            mOffsetY += SectorManager.SECTOR_SIZE;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > (SectorManager.SECTOR_SIZE / 2)) {
            mOffsetY -= SectorManager.SECTOR_SIZE;
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
            x += SectorManager.SECTOR_SIZE;
            sectorX --;
        }
        while (x >= SectorManager.SECTOR_SIZE) {
            x -= SectorManager.SECTOR_SIZE;
            sectorX ++;
        }
        while (y < 0) {
            y += SectorManager.SECTOR_SIZE;
            sectorY --;
        }
        while (y >= SectorManager.SECTOR_SIZE) {
            y -= SectorManager.SECTOR_SIZE;
            sectorY ++;
        }

        Sector sector = SectorManager.getInstance().getSector(sectorX, sectorY);
        if (sector == null) {
            // if it's not loaded yet, you can't have tapped on anything...
            return null;
        }

        int minDistance = 0;
        Star closestStar = null;

        for(Star star : sector.getStars()) {
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
            return closestStar;
        }
        return null;
    }

    private List<VisibleEntity> getVisibleEntitiesAt(int viewX, int viewY) {
        ArrayList<VisibleEntity> entities = new ArrayList<VisibleEntity>();

        Point2D tap = new Point2D(viewX, viewY);
        for (VisibleEntity entity : mVisibleEntities) {
            float distance = tap.distanceTo(entity.position);
            if (distance < 48.0f) {
                entities.add(entity);
            }
        }

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
    @SuppressLint("DrawAllocation") // these are definitely bad, but need some time to figure out
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        super.onDraw(canvas);

        if (mBuffer == null || mNeedRedraw) {
            if (mBuffer == null) {
                mBuffer = Bitmap.createBitmap(canvas.getWidth(),
                                              canvas.getHeight(),
                                              Bitmap.Config.ARGB_8888);
            }

            if (mScrollToCentre) {
                mOffsetX += mBuffer.getWidth() / 2.0f / getPixelScale();
                mOffsetY += mBuffer.getHeight() / 2.0f / getPixelScale();
                mScrollToCentre = false;
            }

            DrawState state = new DrawState(this);
            final List<Pair<Long, Long>> missingSectors = drawScene(state);
            mVisibleEntities = state.visibleEntities;
            mNeedRedraw = false;

            if (missingSectors != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SectorManager.getInstance().requestSectors(missingSectors, false, null);
                    }
                });
            }
        }

        canvas.drawBitmap(mBuffer, 0, 0, mStarPaint);
    }

    public void setDirty() {
        mNeedRedraw = true;
    }

    /**
     * Draws the current "scene" to the internal buffer.
     */
    private static List<Pair<Long, Long>> drawScene(DrawState state) {
        long startTime = System.nanoTime();
        SectorManager sm = SectorManager.getInstance();

        List<Pair<Long, Long>> missingSectors = null;

        for(int y = -sRadius; y <= sRadius; y++) {
            for(int x = -sRadius; x <= sRadius; x++) {
                long sX = state.sectorX + x;
                long sY = state.sectorY + y;

                Sector sector = sm.getSector(sX, sY);
                if (sector == null) {
                    if (missingSectors == null) {
                        missingSectors = new ArrayList<Pair<Long, Long>>();
                    }
                    missingSectors.add(new Pair<Long, Long>(sX, sY));
                    log.debug(String.format("Missing sector: %d, %d", sX, sY));
                    continue;
                }

                int sx = (int)((x * SectorManager.SECTOR_SIZE) + state.offsetX);
                int sy = (int)((y * SectorManager.SECTOR_SIZE) + state.offsetY);

                StarfieldBackgroundRenderer bgRenderer = SectorManager.getInstance().getBackgroundRenderer(
                        state.context, sector);
                bgRenderer.drawBackground(state.canvas, sx, sy,
                        sx+SectorManager.SECTOR_SIZE, sy+SectorManager.SECTOR_SIZE);

                drawSector(state, sx, sy, sector);
            }
        }

        long endTime = System.nanoTime();
        double ms = (double)(endTime - startTime) / 1000000.0;
        if (ms > 150.0) {
            // only log if it's > 150ms (which it should never be!)
            log.debug(String.format("Scene re-drawn in %.4fms", ms));
        }

        return missingSectors;
    }

    /**
     * Given a \c Sector, returns the (x, y) coordinates (in view-space) of the origin of this
     * sector.
     */
    private static Point2D getSectorOffset(DrawState state, long sx, long sy) {
        return new Point2D((sx * SectorManager.SECTOR_SIZE) + state.offsetX,
                           (sy * SectorManager.SECTOR_SIZE) + state.offsetY);
    }

    /**
     * Draws a sector, which is a 1024x1024 area of stars.
     */
    private static void drawSector(DrawState state, int offsetX, int offsetY, Sector sector) {
        for(Star star : sector.getStars()) {
            drawStar(state, star, offsetX, offsetY);
        }
        for(Star star : sector.getStars()) {
            drawStarName(state, star, offsetX, offsetY);
        }
        for (Star star : sector.getStars()) {
            for (Fleet fleet : star.getFleets()) {
                if (fleet.getState() == Fleet.State.MOVING) {
                    drawMovingFleet(state, fleet, star, offsetX, offsetY);
                }
            }
        }
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private static void drawStar(DrawState state, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();
        final float pixelScale = state.pixelScale;

        // only draw the star if it's actually visible...
        if (isVisible(state.canvas, (x - 100) * pixelScale, (y - 100) * pixelScale,
                                    (x + 100) * pixelScale, (y + 100) * pixelScale)) {

            float imageScale = (float) star.getStarType().getImageScale();
            int imageSize = (int)(star.getSize() * imageScale * 2);
            Sprite sprite = StarImageManager.getInstance().getSprite(state.context, star, imageSize);
            state.matrix.reset();
            state.matrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
            state.matrix.postScale(40.0f * imageScale * pixelScale / sprite.getWidth(),
                                   40.0f * imageScale * pixelScale / sprite.getHeight());
            state.matrix.postTranslate(x * pixelScale, y * pixelScale);
            state.canvas.save();
            state.canvas.setMatrix(state.matrix);
            sprite.draw(state.canvas);
            state.canvas.restore();

            drawStarIcons(state, star, x, y);

            List<VisibleEntityAttachedOverlay> overlays = state.starAttachedOverlays.get(star.getKey());
            if (overlays != null && !overlays.isEmpty()) {
                int n = overlays.size();
                for (int i = 0; i < n; i++) {
                    VisibleEntityAttachedOverlay sao = overlays.get(i);
                    sao.setCentre(x * pixelScale, y * pixelScale);
                }
            }

            state.visibleEntities.add(new VisibleEntity(new Point2D(x * pixelScale, y * pixelScale), star));
        }
    }

    /**
     * Gets an \c Empire given it's key.
     */
    private static Empire getEmpire(final DrawState state, String empireKey) {
        if (empireKey == null) {
            return EmpireManager.getInstance().getNativeEmpire();
        }

        Empire emp = state.visibleEmpires.get(empireKey);
        if (emp == null) {
            EmpireManager.getInstance().fetchEmpire(empireKey, new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    state.visibleEmpires.put(empire.getKey(), empire);
                    state.requestRedraw();
                }

            });
        }
        return emp;
    }

    private static void drawStarIcons(DrawState state, Star star, int x, int y) {
        List<Colony> colonies = star.getColonies();
        if (colonies != null && !colonies.isEmpty()) {
            Map<String, Integer> colonyEmpires = new TreeMap<String, Integer>();

            for (int i = 0; i < colonies.size(); i++) {
                Colony colony = colonies.get(i);
                if (colony.getEmpireKey() == null) {
                    continue;
                }

                Empire emp = getEmpire(state, colony.getEmpireKey());
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
                Empire emp = state.visibleEmpires.get(empireKey);

                Bitmap bmp = emp.getShield(state.context);

                Point2D pt = new Point2D(0, -25.0f);
                pt.rotate((float)(Math.PI / 4.0) * i);
                pt.add(x, y);

                state.matrix.reset();
                state.matrix.postTranslate(-(bmp.getWidth() / 2.0f), -(bmp.getHeight() / 2.0f));
                state.matrix.postScale(16.0f * state.pixelScale / bmp.getWidth(),
                                       16.0f * state.pixelScale / bmp.getHeight());
                state.matrix.postTranslate(pt.x * state.pixelScale, pt.y * state.pixelScale);
                state.canvas.drawBitmap(bmp, state.matrix, state.starPaint);

                String name;
                if (n.equals(1)) {
                    name = emp.getDisplayName();
                } else {
                    name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), n);
                }

                Rect bounds = new Rect();
                state.starPaint.getTextBounds(name, 0, name.length(), bounds);
                float textHeight = bounds.height();

                state.canvas.drawText(name,
                                      (pt.x + 12) * state.pixelScale,
                                      (pt.y + 8) * state.pixelScale - (textHeight / 2),
                                      state.starPaint);
                i++;
            }
        }

        List<Fleet> fleets = star.getFleets();
        if (fleets != null && !fleets.isEmpty()) {
            Map<String, Integer> empireFleets = new TreeMap<String, Integer>();
            for (int i = 0; i < fleets.size(); i++) {
                Fleet f = fleets.get(i);
                if (f.getState() == Fleet.State.MOVING) {
                    // ignore moving fleets, we'll draw them separately
                    continue;
                }

                Integer n = empireFleets.get(f.getEmpireKey());
                if (n == null) {
                    empireFleets.put(f.getEmpireKey(), f.getNumShips());
                } else {
                    empireFleets.put(f.getEmpireKey(), n + f.getNumShips());
                }
            }

            int i = 0;
            for (String empireKey : empireFleets.keySet()) {
                Integer numShips = empireFleets.get(empireKey);
                Empire emp = getEmpire(state, empireKey);
                if (emp != null) {
                    Point2D pt = new Point2D(0, -25.0f);
                    pt.rotate((float)(Math.PI / 4.0) * -i);
                    pt.add(x, y);

                    state.matrix.reset();
                    state.matrix.postTranslate(-(sFleetMultiBitmap.getWidth() / 2.0f),
                                               -(sFleetMultiBitmap.getHeight() / 2.0f));
                    state.matrix.postScale(16.0f * state.pixelScale / sFleetMultiBitmap.getWidth(),
                                      16.0f * state.pixelScale / sFleetMultiBitmap.getHeight());
                    state.matrix.postTranslate(pt.x * state.pixelScale, pt.y * state.pixelScale);
                    state.canvas.drawBitmap(sFleetMultiBitmap, state.matrix, state.starPaint);

                    String name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), numShips);

                    Rect bounds = new Rect();
                    state.starPaint.getTextBounds(name, 0, name.length(), bounds);
                    float textHeight = bounds.height();
                    float textWidth = bounds.width();

                    state.canvas.drawText(name,
                                          (pt.x - 12) * state.pixelScale - textWidth,
                                          (pt.y + 8) * state.pixelScale - (textHeight / 2),
                                          state.starPaint);
                }

                i++;
            }

        }
    }

    /**
     * Draw a moving fleet as a line between the source and destination stars, with an icon
     * representing the current location of the fleet.
     */
    private static void drawMovingFleet(DrawState state, Fleet fleet, Star srcStar, int offsetX, int offsetY) {
        float pixelScale = state.pixelScale;

        // we'll need to find the destination star
        Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
        if (destStar == null) {
            // the destination star isn't in one of the sectors we have in memory, we'll
            // just ignore this fleet (it's probably flying off the edge of the sector and our
            // little viewport won't see it anyway -- unless you've got a REALLY long-range
            // flight, maybe we can stop that from being possible).
            return;
        }

        Point2D srcPoint = new Point2D(offsetX, offsetY);
        srcPoint.x += srcStar.getOffsetX();
        srcPoint.y += srcStar.getOffsetY();

        Point2D destPoint = getSectorOffset(state, destStar.getSectorX(), destStar.getSectorY());
        destPoint.x += destStar.getOffsetX();
        destPoint.y += destStar.getOffsetY();

        // work out how far along the fleet has moved so we can draw the icon at the correct
        // spot. Also, we'll draw the name of the empire, number of ships etc.
        ShipDesign design = ShipDesignManager.getInstance().getDesign(fleet.getDesignID());
        float distance = srcPoint.distanceTo(destPoint);
        float totalTimeInHours = (distance / 10.0f) / (float) design.getSpeedInParsecPerHour();

        DateTime startTime = fleet.getStateStartTime();
        DateTime now = DateTime.now(DateTimeZone.UTC);
        float timeSoFarInHours = Seconds.secondsBetween(startTime, now).getSeconds() / 3600.0f;

        float fractionComplete = timeSoFarInHours / totalTimeInHours;
        if (fractionComplete > 1.0f) {
            fractionComplete = 1.0f;
        }

        // we don't want to start the fleet over the top of the star, so we'll offset it a bit
        distance -= 40.0f;
        if (distance < 0) {
            distance = 0;
        }

        Point2D direction = new Point2D(destPoint);
        direction.subtract(srcPoint);
        direction.normalize();

        Point2D location = new Point2D(direction);
        location.scale(distance * fractionComplete);
        location.add(srcPoint);

        Sprite fleetSprite = design.getSprite();
        Point2D up = fleetSprite.getUp();

        float angle = Point2D.angleBetween(up, direction);

        direction.scale(20.0f);
        location.add(direction);

        Point2D position = new Point2D(location.x * pixelScale, location.y * pixelScale);

        // check if there's any other fleets nearby and offset this one by a bit so that they
        // don't overlap
        Random rand = new Random(fleet.getKey().hashCode());
        for (int i = 0; i < state.visibleEntities.size(); i++) {
            VisibleEntity existing = state.visibleEntities.get(i);
            if (existing.fleet == null) {
                continue;
            }

            if (existing.position.distanceTo(position) < (15.0f * pixelScale)) {
                // pick a random direction and offset it a bit
                Point2D offset = new Point2D(0, 20.0f * pixelScale);
                offset.rotate(rand.nextFloat() * 2 * (float) Math.PI);
                position.add(offset);
                i = -1; // start looping again...
            }
        }

        // record the fact that this guy is visible
        state.visibleEntities.add(new VisibleEntity(position, fleet));

        // scale zoom and rotate the bitmap all with one matrix
        state.matrix.reset();
        state.matrix.postTranslate(-(fleetSprite.getWidth() / 2.0f),
                                   -(fleetSprite.getHeight() / 2.0f));
        state.matrix.postScale(20.0f * pixelScale / fleetSprite.getWidth(),
                              20.0f * pixelScale / fleetSprite.getWidth());
        state.matrix.postRotate((float) (angle * 180.0 / Math.PI));
        state.matrix.postTranslate(position.x, position.y);
        state.canvas.save();
        state.canvas.setMatrix(state.matrix);
        fleetSprite.draw(state.canvas);
        state.canvas.restore();

        Empire emp = getEmpire(state, fleet.getEmpireKey());
        if (emp != null) {
            Bitmap shield = emp.getShield(state.context);
            if (shield != null) {
                state.matrix.reset();
                state.matrix.postTranslate(-(shield.getWidth() / 2.0f), -(shield.getHeight() / 2.0f));
                state.matrix.postScale(16.0f * pixelScale / shield.getWidth(),
                                       16.0f * pixelScale / shield.getHeight());
                state.matrix.postTranslate(position.x + (20.0f * pixelScale),
                                           position.y);
                state.canvas.drawBitmap(shield, state.matrix, state.starPaint);
            }

            String msg = emp.getDisplayName();
            state.canvas.drawText(msg, position.x + (30.0f * pixelScale),
                    position.y, state.starPaint);

            msg = String.format(Locale.ENGLISH, "%s (%d)", design.getDisplayName(), fleet.getNumShips());
            state.canvas.drawText(msg, position.x + (30.0f * pixelScale),
                    position.y + (10.0f * pixelScale), state.starPaint);
        }

        List<VisibleEntityAttachedOverlay> fleetAttachedOverlays = state.fleetAttachedOverlays.get(fleet.getKey());
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
    private static void drawStarName(DrawState state, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        final float pixelScale = state.pixelScale;

        state.starNamePaint.setARGB(255, 255, 255, 255);
        float width = state.starNamePaint.measureText(star.getName()) / pixelScale;
        x -= (width / 2.0f);
        y += star.getSize() + 10.0f;

        state.canvas.drawText(star.getName(),
                x * pixelScale, y * pixelScale, state.starNamePaint);
    }

    public void selectStar(String starKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        selectStar(star);
    }

    private void selectStar(Star star) {
        if (star != null) {
            log.info("Selecting star: "+star.getKey());
            mSelectedStar = star;
            mSelectedFleet = null;

            mSelectionOverlay.setRadius((star.getSize() + 4) * getPixelScale());
            if (mSelectionOverlay.isVisible()) {
                removeOverlay(mSelectionOverlay);
            }
            addOverlay(mSelectionOverlay, star);

            setDirty();
            redraw();
            fireSelectionChanged(star);
        }
    }

    public void selectFleet(Fleet fleet) {
        if (fleet != null && fleet.getState() == Fleet.State.MOVING) {
            log.info("Selecting fleet: "+fleet.getKey());
            mSelectedStar = null;
            mSelectedFleet = fleet;

            mSelectionOverlay.setRadius(15.0f * getPixelScale());
            if (mSelectionOverlay.isVisible()) {
                removeOverlay(mSelectionOverlay);
            }
            addOverlay(mSelectionOverlay, fleet);

            setDirty();
            redraw();
            fireSelectionChanged(fleet);
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            scroll(-(float)(distanceX / getPixelScale()),
                   -(float)(distanceY / getPixelScale()));

            setDirty();
            redraw();
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
                if (mSelectedStar != null && tappedEntities.get(n).star != null) {
                    if (mSelectedStar.getKey().equals(tappedEntities.get(n).star.getKey())) {
                        break;
                    }
                }
                if (mSelectedFleet != null && tappedEntities.get(n).fleet != null) {
                    if (mSelectedFleet.getKey().equals(tappedEntities.get(n).fleet.getKey())) {
                        break;
                    }
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

    private static class DrawState {
        public Context context;
        public Bitmap bitmap;
        public long sectorX;
        public long sectorY;
        public float offsetX;
        public float offsetY;
        public List<VisibleEntity> visibleEntities;
        public Canvas canvas;
        public float pixelScale;
        public Map<String, List<VisibleEntityAttachedOverlay>> starAttachedOverlays;
        public Map<String, List<VisibleEntityAttachedOverlay>> fleetAttachedOverlays;
        public Matrix matrix;
        public Map<String, Empire> visibleEmpires;
        public Paint starPaint;
        public Paint starNamePaint;

        private StarfieldSurfaceView mStarfieldSurfaceView;

        public DrawState(StarfieldSurfaceView ssv) {
            mStarfieldSurfaceView = ssv;
            context = ssv.mContext;
            bitmap = ssv.mBuffer;
            canvas = new Canvas(bitmap);
            sectorX = ssv.mSectorX;
            sectorY = ssv.mSectorY;
            offsetX = ssv.mOffsetX;
            offsetY = ssv.mOffsetY;
            pixelScale = ssv.getPixelScale();
            visibleEntities = new ArrayList<VisibleEntity>();
            starAttachedOverlays = ssv.mStarAttachedOverlays;
            fleetAttachedOverlays = ssv.mFleetAttachedOverlays;
            matrix = new Matrix();
            visibleEmpires = ssv.mVisibleEmpires;
            starPaint = ssv.mStarPaint;

            starNamePaint = new Paint();
            starNamePaint.setStyle(Style.STROKE);
            starNamePaint.setTextSize(15.0f * pixelScale);
        }

        public void requestRedraw() {
            mStarfieldSurfaceView.setDirty();
            mStarfieldSurfaceView.redraw();
        }
    }

    /**
     * Every time we draw the screen, we track the visible entities to make tracking when you
     * tap on them simpler. This class holds a reference to a single "visible" entity.
     */
    private static class VisibleEntity {
        public Point2D position;
        public Star star;
        public Fleet fleet;

        public VisibleEntity(Point2D position, Star star) {
            this.position = position;
            this.star = star;
        }

        public VisibleEntity(Point2D position, Fleet fleet) {
            this.position = position;
            this.fleet = fleet;
        }
    }

    public interface OnSelectionChangedListener {
        public abstract void onStarSelected(Star star);
        public abstract void onFleetSelected(Fleet fleet);
    }
}
