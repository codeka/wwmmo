package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import android.graphics.Rect;
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

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends UniverseElementSurfaceView {
    private Logger log = LoggerFactory.getLogger(StarfieldSurfaceView.class);
    private Context mContext;
    private ArrayList<OnSelectionChangedListener> mSelectionChangedListeners;
    private Star mSelectedStar;
    private Fleet mSelectedFleet;
    private Paint mStarPaint;
    private Paint mStarNamePaint;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private SelectionOverlay mSelectionOverlay;
    private Map<String, List<VisibleEntityAttachedOverlay>> mStarAttachedOverlays;
    private Map<String, List<VisibleEntityAttachedOverlay>> mFleetAttachedOverlays;
    private Map<String, Empire> mVisibleEmpires;
    private ArrayList<VisibleEntity> mVisibleEntities;

    private Bitmap mFleetMultiBitmap;
    private Matrix mMatrix;

    private boolean mNeedRedraw = true;
    private Bitmap mBuffer;

    private int mRadius = 1;

    private long mSectorX;
    private long mSectorY;
    private float mOffsetX;
    private float mOffsetY;

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
        mMatrix = new Matrix();
        mVisibleEntities = new ArrayList<VisibleEntity>();

        mSelectionOverlay = new SelectionOverlay();

        mStarPaint = new Paint();
        mStarPaint.setARGB(255, 255, 255, 255);
        mStarPaint.setStyle(Style.STROKE);

        mBackgroundRenderer = new StarfieldBackgroundRenderer(mContext);
        mFleetMultiBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fleet);

        // whenever the sector list changes (i.e. when we've refreshed from the server),
        // redraw the screen.
        SectorManager.getInstance().addSectorListChangedListener(new SectorManager.OnSectorListChangedListener() {
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
        });

        // whenever a new star bitmap is generated, redraw the screen
        StarImageManager.getInstance().addBitmapGeneratedListener(
                new ImageManager.BitmapGeneratedListener() {
            @Override
            public void onBitmapGenerated(String key, Bitmap bmp) {
                setDirty();
                redraw();
            }
        });

        scrollTo(0, 0, 0, 0);
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
    public void addOverlay(VisibleEntityAttachedOverlay overlay, Star star) {
        addOverlay(overlay);

        List<VisibleEntityAttachedOverlay> starAttachedOverlays = mStarAttachedOverlays.get(star.getKey());
        if (starAttachedOverlays == null) {
            starAttachedOverlays = new ArrayList<VisibleEntityAttachedOverlay>();
            mStarAttachedOverlays.put(star.getKey(), starAttachedOverlays);
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
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = -offsetX;
        mOffsetY = -offsetY;

        List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();

        for(sectorY = mSectorY - mRadius; sectorY <= mSectorY + mRadius; sectorY++) {
            for(sectorX = mSectorX - mRadius; sectorX <= mSectorX + mRadius; sectorX++) {
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
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            // TODO: do something?
            return;
        }

        super.onDraw(canvas);

        if (mBuffer == null || mNeedRedraw) {
            if (mBuffer == null) {
                mBuffer = Bitmap.createBitmap(canvas.getWidth(),
                                              canvas.getHeight(),
                                              Bitmap.Config.ARGB_8888);
            }

            drawScene();
            mNeedRedraw = false;
        }

        canvas.drawBitmap(mBuffer, 0, 0, mStarPaint);
    }

    public void setDirty() {
        mNeedRedraw = true;
    }

    /**
     * Draws the current "scene" to the internal buffer.
     */
    private void drawScene() {
        long startTime = System.nanoTime();
        Canvas canvas = new Canvas(mBuffer);

        SectorManager sm = SectorManager.getInstance();
        canvas.drawColor(Color.BLACK);

        // clear the list of visible entities, we'll be re-creating this
        mVisibleEntities.clear();

        for(int y = -mRadius; y <= mRadius; y++) {
            for(int x = -mRadius; x <= mRadius; x++) {
                long sectorX = mSectorX + x;
                long sectorY = mSectorY + y;

                Sector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    continue; // it might not be loaded yet...
                }

                int sx = (int)((x * SectorManager.SECTOR_SIZE) + mOffsetX);
                int sy = (int)((y * SectorManager.SECTOR_SIZE) + mOffsetY);

                // TODO: seed should be part of the sector (and used for other things too)
                mBackgroundRenderer.drawBackground(canvas, sx, sy,
                        sx+SectorManager.SECTOR_SIZE, sy+SectorManager.SECTOR_SIZE,
                        sectorX ^ sectorY + sectorX);

                drawSector(canvas, sx, sy, sector);
            }
        }

        long endTime = System.nanoTime();
        double ms = (double)(endTime - startTime) / 1000000.0;
        if (ms > 150.0) {
            // only log if it's > 150ms (which it should never be!)
            log.debug(String.format("Scene re-drawn in %.4fms", ms));
        }
    }

    /**
     * Given a \c Sector, returns the (x, y) coordinates (in view-space) of the origin of this
     * sector.
     */
    private Point2D getSectorOffset(long sx, long sy) {
        SectorManager sm = SectorManager.getInstance();

        for(int y = -mRadius; y <= mRadius; y++) {
            for(int x = -mRadius; x <= mRadius; x++) {
                long sectorX = mSectorX + x;
                long sectorY = mSectorY + y;

                Sector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    continue; // it might not be loaded yet...
                }

                if (sector.getX() == sx && sector.getY() == sy) {
                    return new Point2D((x * SectorManager.SECTOR_SIZE) + mOffsetX,
                                       (y * SectorManager.SECTOR_SIZE) + mOffsetY);
                }
            }
        }

        return null;
    }

    /**
     * Draws a sector, which is a 1024x1024 area of stars.
     */
    private void drawSector(Canvas canvas, int offsetX, int offsetY, Sector sector) {
        for(Star star : sector.getStars()) {
            drawStar(canvas, star, offsetX, offsetY);
        }
        for(Star star : sector.getStars()) {
            drawStarName(canvas, star, offsetX, offsetY);
        }
        for (Star star : sector.getStars()) {
            for (Fleet fleet : star.getFleets()) {
                if (fleet.getState() == Fleet.State.MOVING) {
                    drawMovingFleet(canvas, fleet, star, offsetX, offsetY);
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

            int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
            Sprite sprite = StarImageManager.getInstance().getSprite(mContext, star, imageSize);
            mMatrix.reset();
            mMatrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
            mMatrix.postScale(40.0f * pixelScale / sprite.getWidth(),
                              40.0f * pixelScale / sprite.getHeight());
            mMatrix.postTranslate(x * pixelScale, y * pixelScale);
            canvas.save();
            canvas.setMatrix(mMatrix);
            sprite.draw(canvas);
            canvas.restore();

            drawStarIcons(canvas, star, x, y);

            List<VisibleEntityAttachedOverlay> starAttachedOverlays = mStarAttachedOverlays.get(star.getKey());
            if (starAttachedOverlays != null && !starAttachedOverlays.isEmpty()) {
                int n = starAttachedOverlays.size();
                for (int i = 0; i < n; i++) {
                    VisibleEntityAttachedOverlay sao = starAttachedOverlays.get(i);
                    sao.setCentre(x * pixelScale, y * pixelScale);
                }
            }

            mVisibleEntities.add(new VisibleEntity(new Point2D(x * pixelScale, y * pixelScale), star));
        }
    }

    /**
     * Gets an \c Empire given it's key.
     */
    private Empire getEmpire(String empireKey) {
        Empire emp = mVisibleEmpires.get(empireKey);
        if (emp == null) {
            EmpireManager.getInstance().fetchEmpire(empireKey, new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    mVisibleEmpires.put(empire.getKey(), empire);
                    setDirty();
                    redraw();
                }

            });
        }
        return emp;
    }

    private void drawStarIcons(Canvas canvas, Star star, int x, int y) {
        final float pixelScale = getPixelScale();

        List<Colony> colonies = star.getColonies();
        if (colonies != null && !colonies.isEmpty()) {
            Map<String, Integer> colonyEmpires = new TreeMap<String, Integer>();

            for (int i = 0; i < colonies.size(); i++) {
                Colony colony = colonies.get(i);

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

                Point2D pt = new Point2D(0, -25.0f);
                pt.rotate((float)(Math.PI / 4.0) * i);
                pt.add(x, y);

                mMatrix.reset();
                mMatrix.postTranslate(-(bmp.getWidth() / 2.0f), -(bmp.getHeight() / 2.0f));
                mMatrix.postScale(16.0f * pixelScale / bmp.getWidth(),
                                  16.0f * pixelScale / bmp.getHeight());
                mMatrix.postTranslate(pt.x * pixelScale, pt.y * pixelScale);
                canvas.drawBitmap(bmp, mMatrix, mStarPaint);

                String name;
                if (n.equals(1)) {
                    name = emp.getDisplayName();
                } else {
                    name = String.format("%s (%d)", emp.getDisplayName(), n);
                }

                Rect bounds = new Rect();
                mStarPaint.getTextBounds(name, 0, name.length(), bounds);
                float textHeight = bounds.height();

                canvas.drawText(name,
                                (pt.x + 12) * pixelScale,
                                (pt.y + 8) * pixelScale - (textHeight / 2),
                                mStarPaint);
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
                Empire emp = getEmpire(empireKey);
                if (emp != null) {
                    Point2D pt = new Point2D(0, -25.0f);
                    pt.rotate((float)(Math.PI / 4.0) * -i);
                    pt.add(x, y);

                    mMatrix.reset();
                    mMatrix.postTranslate(-(mFleetMultiBitmap.getWidth() / 2.0f),
                                          -(mFleetMultiBitmap.getHeight() / 2.0f));
                    mMatrix.postScale(16.0f * pixelScale / mFleetMultiBitmap.getWidth(),
                                      16.0f * pixelScale / mFleetMultiBitmap.getHeight());
                    mMatrix.postTranslate(pt.x * pixelScale, pt.y * pixelScale);
                    canvas.drawBitmap(mFleetMultiBitmap, mMatrix, mStarPaint);

                    String name = String.format("%s (%d)", emp.getDisplayName(), numShips);

                    Rect bounds = new Rect();
                    mStarPaint.getTextBounds(name, 0, name.length(), bounds);
                    float textHeight = bounds.height();
                    float textWidth = bounds.width();

                    canvas.drawText(name,
                                    (pt.x - 12) * pixelScale - textWidth,
                                    (pt.y + 8) * pixelScale - (textHeight / 2),
                                    mStarPaint);
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

        Point2D srcPoint = new Point2D(offsetX, offsetY);
        srcPoint.x += srcStar.getOffsetX();
        srcPoint.y += srcStar.getOffsetY();

        Point2D destPoint = getSectorOffset(destStar.getSectorX(), destStar.getSectorY());
        destPoint.x += destStar.getOffsetX();
        destPoint.y += destStar.getOffsetY();

        // work out how far along the fleet has moved so we can draw the icon at the correct
        // spot. Also, we'll draw the name of the empire, number of ships etc.
        ShipDesign design = ShipDesignManager.getInstance().getDesign(fleet.getDesignID());
        float distance = srcPoint.distanceTo(destPoint);
        float totalTimeInHours = (float) design.getSpeedInParsecPerHour() / (distance / 10.0f);

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

        // TODO: check that it's actually visible on the screen....

        // record the fact that this guy is visible
        mVisibleEntities.add(new VisibleEntity(new Point2D(location.x * pixelScale, location.y * pixelScale), fleet));

        // scale zoom and rotate the bitmap all with one matrix
        mMatrix.reset();
        mMatrix.postTranslate(-(fleetSprite.getWidth() / 2.0f),
                              -(fleetSprite.getHeight() / 2.0f));
        mMatrix.postScale(20.0f * pixelScale / fleetSprite.getWidth(),
                          20.0f * pixelScale / fleetSprite.getHeight());
        mMatrix.postRotate((float) (angle * 180.0 / Math.PI));
        mMatrix.postTranslate(location.x * pixelScale, (location.y) * pixelScale);
        canvas.save();
        canvas.setMatrix(mMatrix);
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
                mMatrix.postTranslate((location.x + 20.0f) * pixelScale,
                                      location.y * pixelScale);
                canvas.drawBitmap(shield, mMatrix, mStarPaint);
            }

            String msg = emp.getDisplayName();
            canvas.drawText(msg, (location.x + 30.0f) * pixelScale, location.y * pixelScale, mStarPaint);

            msg = String.format("%s (%d)", design.getDisplayName(), fleet.getNumShips());
            canvas.drawText(msg, (location.x + 30.0f) * pixelScale, (location.y + 10.0f) * pixelScale, mStarPaint);
        }

        List<VisibleEntityAttachedOverlay> fleetAttachedOverlays = mFleetAttachedOverlays.get(fleet.getKey());
        if (fleetAttachedOverlays != null && !fleetAttachedOverlays.isEmpty()) {
            int n = fleetAttachedOverlays.size();
            for (int i = 0; i < n; i++) {
                VisibleEntityAttachedOverlay sao = fleetAttachedOverlays.get(i);
                sao.setCentre(location.x * pixelScale, location.y * pixelScale);
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

        if (mStarNamePaint == null) {
            mStarNamePaint = new Paint();
            mStarNamePaint.setStyle(Style.STROKE);
            mStarNamePaint.setTextSize(15.0f * pixelScale);
        }
        mStarNamePaint.setARGB(255, 255, 255, 255);
        float width = mStarNamePaint.measureText(star.getName()) / pixelScale;
        x -= (width / 2.0f);
        y += star.getSize() + 10.0f;

        canvas.drawText(star.getName(),
                x * pixelScale, y * pixelScale, mStarNamePaint);
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

    private void selectFleet(Fleet fleet) {
        if (fleet != null) {
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

    /**
     * An \c Overlay that's "attached" to a "visible entity" (star or fleet). We'll make sure
     * it's recentred whenever the view scrolls around.
     */
    public static abstract class VisibleEntityAttachedOverlay extends UniverseElementSurfaceView.Overlay {
        protected Point2D mCentre;

        public VisibleEntityAttachedOverlay() {
            mCentre = new Point2D();
        }

        public Point2D getCentre() {
            return mCentre;
        }

        public void setCentre(double x, double y) {
            mCentre.x = (float) x;
            mCentre.y = (float) y;
        }
    }

    /**
     * This overlay is used for drawing the selection indicator. It's an animated dotted circle
     * that spins around the selected point.
     */
    private static class SelectionOverlay extends VisibleEntityAttachedOverlay {
        private RotatingCircle mInnerCircle;
        private RotatingCircle mOuterCircle;

        public SelectionOverlay() {
            Paint p = new Paint();
            p.setARGB(255, 255, 255, 255);
            mInnerCircle = new RotatingCircle(p);

            p = new Paint();
            p.setARGB(255, 255, 255, 255);
            mOuterCircle = new RotatingCircle(p);
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);

            mInnerCircle.setCentre(x, y);
            mOuterCircle.setCentre(x, y);
        }

        public void setRadius(double radius) {
            mInnerCircle.setRadius(radius);
            mOuterCircle.setRadius(radius + 4.0);
        }

        @Override
        public void draw(Canvas canvas) {
            mInnerCircle.draw(canvas);
            mOuterCircle.draw(canvas);
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
