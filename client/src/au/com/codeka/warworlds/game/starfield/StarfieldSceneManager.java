package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Canvas;
import android.view.GestureDetector;
import au.com.codeka.common.Pair;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSceneManager extends SectorSceneManager
                                  implements StarManager.StarFetchedHandler,
                                             EmpireManager.EmpireFetchedHandler,
                                             EmpireShieldManager.EmpireShieldUpdatedHandler {
    private static final Logger log = LoggerFactory.getLogger(StarfieldSceneManager.class);
    private ArrayList<OnSelectionChangedListener> mSelectionChangedListeners;
    private Map<String, Empire> mVisibleEmpires;
    private BaseStar mHqStar;

    private BitmapTextureAtlas mStarTextureAtlas;
    private TiledTextureRegion mNeutronStarTextureRegion;
    private TiledTextureRegion mNormalStarTextureRegion;

    public StarfieldSceneManager(StarfieldActivity activity) {
        super(activity);
        log.info("Starfield initializing...");

        mSelectionChangedListeners = new ArrayList<OnSelectionChangedListener>();
        mVisibleEmpires = new TreeMap<String, Empire>();
    }

    @Override
    public void onLoadResources() {
        mStarTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 128, 320,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("stars/");
        mNormalStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mStarTextureAtlas, mActivity,
                "stars_small.png", 0, 0, 4, 10);
        mNeutronStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mStarTextureAtlas, mActivity,
                "stars_small.png", 0, 0, 2, 5);
        mActivity.getTextureManager().loadTexture(mStarTextureAtlas);
    }

    @Override
    protected void onStart() {
        super.onStart();

        StarManager.getInstance().addStarUpdatedListener(null, this);
        EmpireManager.i.addEmpireUpdatedListener(null, this);
        EmpireShieldManager.i.addEmpireShieldUpdatedHandler(this);

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire != null) {
            BaseStar homeStar = myEmpire.getHomeStar();
            int numHqs = BuildManager.getInstance().getTotalBuildingsInEmpire("hq");
            if (numHqs > 0) {
                mHqStar = homeStar;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        StarManager.getInstance().removeStarUpdatedListener(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
        EmpireShieldManager.i.removeEmpireShieldUpdatedHandler(this);
    }

    public void deselectStar() {
    }

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onEmpireShieldUpdated(int empireID) {
        // TODO: invalidate();
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
/*
    protected void fireSelectionChanged(VisibleEntity entity) {
        for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
            if (entity.star != null) {
                listener.onStarSelected(entity.star);
            } else if (entity.fleet != null) {
                listener.onFleetSelected(entity.fleet);
            }
        }
    }
*/
    @Override
    public void onEmpireFetched(Empire empire) {
        // if the player's empire changes, it might mean that the location of their HQ has changed,
        // so we'll want to make sure it's still correct.
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (empire.getKey().equals(myEmpire.getKey())) {
            if (mHqStar != null) {
                mHqStar = empire.getHomeStar();
            }
        }
    }

    public Star getSelectedStar() {
        return null;
    }

    private void placeSelection() {
        
    }

    @Override
    protected void refreshScene(Scene scene) {
        scene.detachChildren();

        final List<Pair<Long, Long>> missingSectors = drawScene(scene);
        if (missingSectors != null) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }
    }

    private List<Pair<Long, Long>> drawScene(Scene scene) {
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

//                StarfieldBackgroundRenderer bgRenderer = SectorManager.getInstance().getBackgroundRenderer(sector);
//                bgRenderer.drawBackground(canvas, sx, sy,
//                        sx+Sector.SECTOR_SIZE, sy+Sector.SECTOR_SIZE);
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
                drawSector(scene, sx, sy, sector);
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
    private void drawSector(Scene scene, int offsetX, int offsetY, Sector sector) {
        for(BaseStar star : sector.getStars()) {
            drawStar(scene, (Star) star, offsetX, offsetY);
        }/*
        for(BaseStar star : sector.getStars()) {
            drawStarName(scene, (Star) star, offsetX, offsetY);
        }
        for (BaseStar star : sector.getStars()) {
            for (BaseFleet fleet : star.getFleets()) {
                if (fleet.getState() == Fleet.State.MOVING) {
                    drawMovingFleet(scene, (Fleet) fleet, (Star) star, offsetX, offsetY);
                }
            }
        }*/
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStar(Scene scene, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        int starID = Integer.parseInt(star.getKey());

        float size = (float)(star.getSize() * star.getStarType().getImageScale() * 2.0f);
        ITextureRegion textureRegion = null;
        if (star.getStarType().getInternalName().equals("neutron")) {
            textureRegion = mNeutronStarTextureRegion.getTextureRegion(2 + (starID & 3));
            //size *= 4.0f;
        } else {
            int ty = 0;
            if (star.getStarType().getInternalName().equals("black-hole")) {
                ty = 0;
            } else if (star.getStarType().getInternalName().equals("blue")) {
                ty = 1;
            } else if (star.getStarType().getInternalName().equals("orange")) {
                ty = 6;
            } else if (star.getStarType().getInternalName().equals("red")) {
                ty = 7;
            } else if (star.getStarType().getInternalName().equals("white")) {
                ty = 8;
            } else if (star.getStarType().getInternalName().equals("yellow")) {
                ty = 9;
            }
            textureRegion = mNormalStarTextureRegion.getTextureRegion((ty * 4) + (starID & 3));
        }

        Sprite sprite = new Sprite(
                (float) x,
                (float) y,
                size, size,
                textureRegion,
                mActivity.getVertexBufferObjectManager());
        scene.attachChild(sprite);
/*
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
*/
    }

    /**
     * Gets an \c Empire given it's key.
     */
    private Empire getEmpire(String empireKey) {
        if (empireKey == null) {
            return EmpireManager.i.getNativeEmpire();
        }

        Empire emp = mVisibleEmpires.get(empireKey);
        if (emp == null) {
            EmpireManager.i.fetchEmpire(empireKey, new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    mVisibleEmpires.put(empire.getKey(), empire);
                    // TODO: redraw()
                }
            });
        }
        return emp;
    }

    private void drawStarIcons(Scene scene, Star star, int x, int y) {
        /*
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

                Bitmap bmp = EmpireShieldManager.i.getShield(mContext, emp);

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
                if (f.getEmpireKey() == null || f.getState() == Fleet.State.MOVING) {
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

        }*/
    }

    /**
     * Draw a moving fleet as a line between the source and destination stars, with an icon
     * representing the current location of the fleet.
     */
    private void drawMovingFleet(Scene scene, Fleet fleet, Star srcStar, int offsetX, int offsetY) {
        /*
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
            Bitmap shield = EmpireShieldManager.i.getShield(mContext, emp);
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
        }*/
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStarName(Canvas canvas, Star star, int x, int y) {/*
        x += star.getOffsetX();
        y += star.getOffsetY();

        final float pixelScale = getPixelScale();

        float width = mStarNamePaint.measureText(star.getName()) / pixelScale;
        x -= (width / 2.0f);
        y += star.getSize() + 10.0f;

        canvas.drawText(star.getName(),
                        x * pixelScale,
                        y * pixelScale,
                        mStarNamePaint);*/
    }

    public void selectStar(String starKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        selectStar(star);
    }

    public void selectStar(Star star) {
        
    }

    public void selectFleet(BaseFleet fleet) {
        
    }

    /**
     * When a star is updated, if it's one of ours, then we'll want to redraw to make sure we
     * have the latest data (e.g. it might've been renamed)
     */
    @Override
    public void onStarFetched(Star s) {
        // TODO: redraw();
    }

    public interface OnSelectionChangedListener {
        public abstract void onStarSelected(Star star);
        public abstract void onFleetSelected(Fleet fleet);
    }
}
