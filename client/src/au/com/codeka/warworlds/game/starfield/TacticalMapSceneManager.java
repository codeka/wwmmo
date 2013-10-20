package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import au.com.codeka.common.Pair;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages.Star;
import au.com.codeka.controlfield.ControlField;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;

public class TacticalMapSceneManager extends SectorSceneManager
                                     implements EmpireShieldManager.EmpireShieldUpdatedHandler {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapSceneManager.class);

    private TacticalPointCloud mPointCloud;
    private TacticalVoronoi mVoronoi;
    private TreeMap<String, TacticalControlField> mControlFields;

    private BitmapTextureAtlas mBitmapTextureAtlas;
    private TiledTextureRegion mNeutronTextureRegion;
    private TiledTextureRegion mNormalTextureRegion;

    public TacticalMapSceneManager(TacticalMapActivity activity) {
        super(activity);
        mSectorRadius = 2;

        log.info("Tactical map initializing...");
    }

    @Override
    public void onLoadResources() {
        mBitmapTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 128, 320,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("stars/");
        mNormalTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, mActivity,
                "stars_small.png", 0, 0, 4, 10);
        mNeutronTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, mActivity,
                "stars_small.png", 0, 0, 2, 5);
        mActivity.getTextureManager().loadTexture(mBitmapTextureAtlas);
    }

    @Override
    protected void refreshScene(Scene scene) {
        scene.detachChildren();
        refreshControlField();
        mPointCloud.addToScene(scene);
    }

    public void setDoubleTapHandler(DoubleTapHandler handler) {
        
    }

    @Override
    protected void onStart() {
        super.onStart();
        EmpireShieldManager.i.addEmpireShieldUpdatedHandler(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EmpireShieldManager.i.removeEmpireShieldUpdatedHandler(this);
    }

    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    @Override
    protected ScaleGestureDetector.OnScaleGestureListener createScaleGestureListener() {
        return new ScaleGestureListener();
    }

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onEmpireShieldUpdated(int empireID) {
        //invalidate();
    }
/*
    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (mControlFields == null) {
            refreshControlField();
        }

        canvas.drawColor(Color.BLACK);

        for (String empireKey : mControlFields.keySet()) {
            Empire empire = EmpireManager.i.getEmpire(empireKey);
            if (empire == null) {
                EmpireManager.i.fetchEmpire(empireKey, null);
            }
            TacticalControlField cf = mControlFields.get(empireKey);

            if (empire == null) {
                mInfluencePaint.setARGB(128, 255, 255, 255);
            } else {
                mInfluencePaint.setColor(EmpireShieldManager.i.getShieldColour(empire));
            }
            cf.render(canvas, mInfluencePaint);
        }

        mPointCloud.render(canvas);
    }
*/
    private void refreshControlField() {
        SectorManager sm = SectorManager.getInstance();

        List<Pair<Long, Long>> missingSectors = null;
        ArrayList<Vector2> points = new ArrayList<Vector2>();
        TreeMap<String, List<Vector2>> empirePoints = new TreeMap<String, List<Vector2>>();

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
                    continue;
                }

                int sx = (int)(x * Sector.SECTOR_SIZE);
                int sy = (int)(y * Sector.SECTOR_SIZE);

                for (BaseStar star : sector.getStars()) {
                    int starX = sx + star.getOffsetX();
                    int starY = sy + star.getOffsetY();
                    TacticalPointCloudVector2 pt = new TacticalPointCloudVector2(
                            (float) starX / Sector.SECTOR_SIZE, (float) starY / Sector.SECTOR_SIZE,
                            star);

                    TreeSet<String> doneEmpires = new TreeSet<String>();
                    for (BaseColony c : star.getColonies()) {
                        String empireKey = c.getEmpireKey();
                        if (empireKey == null || empireKey.length() == 0) {
                            continue;
                        }
                        if (doneEmpires.contains(empireKey)) {
                            continue;
                        }
                        doneEmpires.add(empireKey);
                        List<Vector2> thisEmpirePoints = empirePoints.get(empireKey);
                        if (thisEmpirePoints == null) {
                            thisEmpirePoints = new ArrayList<Vector2>();
                            empirePoints.put(empireKey, thisEmpirePoints);
                        }
                        thisEmpirePoints.add(pt);
                    }
                    points.add(pt);
                }
            }
        }

        //mDragOffsetX = 0.0f;
        //mDragOffsetY = 0.0f;

        mControlFields = new TreeMap<String, TacticalControlField>();
        mPointCloud = new TacticalPointCloud(points);
        TacticalVoronoi v = new TacticalVoronoi(mPointCloud);

        for (String empireKey : empirePoints.keySet()) {
            TacticalControlField cf = new TacticalControlField(mPointCloud, v);

            List<Vector2> pts = empirePoints.get(empireKey);
            for (Vector2 pt : pts) {
                cf.addPointToControlField(pt);
            }

            mControlFields.put(empireKey, cf);
        }

        if (missingSectors != null) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }
    }

    protected class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float mZoomFactor = 1.0f;

        @Override
        public boolean onScale (ScaleGestureDetector detector) {
            mZoomFactor *= detector.getScaleFactor();

            ((ZoomCamera) mActivity.getCamera()).setZoomFactor(mZoomFactor);
            return true;
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
            // we move double the distance because our view is scaled by half.
            scroll( (float) distanceX,
                   -(float) distanceY);

            return false;
        }

        /**
         * Double-tapping will take you back to the Starfield view with that particular star
         * in the centre.
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //float tapX = (e.getX() - mDragOffsetX) / 256.0f;
            //float tapY = (e.getY() - mDragOffsetY) / 256.0f;

            //BaseStar star = mPointCloud.findStarNear(new Vector2(tapX, tapY));
            //if (star != null) {
                //if (mDoubleTapHandler != null) {
                //   mDoubleTapHandler.onDoubleTapped(star);
                //}
            //}

            return false;
        }
    }

    private class TacticalPointCloud extends PointCloud {
        public TacticalPointCloud(ArrayList<Vector2> points) {
            super(points);
        }
/*
        public void render(Canvas canvas) {
            for (Vector2 p : mPoints) {
                float x = (float)(p.x * 256.0) + mDragOffsetX;
                float y = (float)(p.y * 256.0) + mDragOffsetY;
                canvas.drawCircle(x, y, 5.0f, mPointPaint);
            }
        }
*/
        public void addToScene(Scene scene) {
            Random rand = new Random(); // TODO: this isn't going to look good when a new sector loads

            for (Vector2 p : mPoints) {
                BaseStar star = ((TacticalPointCloudVector2) p).star;

                float size = (float)(star.getSize() * star.getStarType().getImageScale() * 2.0f);
                ITextureRegion textureRegion = null;
                if (star.getStarType().getInternalName().equals("neutron")) {
                    textureRegion = mNeutronTextureRegion.getTextureRegion(2 + rand.nextInt(4));
                    //size *= 4.0f;
                } else {
                    int y = 0;
                    if (star.getStarType().getInternalName().equals("black-hole")) {
                        y = 0;
                    } else if (star.getStarType().getInternalName().equals("blue")) {
                        y = 1;
                    } else if (star.getStarType().getInternalName().equals("orange")) {
                        y = 6;
                    } else if (star.getStarType().getInternalName().equals("red")) {
                        y = 7;
                    } else if (star.getStarType().getInternalName().equals("white")) {
                        y = 8;
                    } else if (star.getStarType().getInternalName().equals("yellow")) {
                        y = 9;
                    }
                    textureRegion = mNormalTextureRegion.getTextureRegion((y * 4) + rand.nextInt(4));
                }

                

                Sprite sprite = new Sprite(
                        (float)(p.x * Sector.SECTOR_SIZE),
                        (float)(p.y * Sector.SECTOR_SIZE),
                        size, size,
                        textureRegion,
                        mActivity.getVertexBufferObjectManager());
                scene.attachChild(sprite);
            }
        }

        /**
         * Finds the star nearest the given point.
         */
        public BaseStar findStarNear(Vector2 pt) {
            TacticalPointCloudVector2 closest = null;
            double distance = 0.0;

            for (Vector2 v : mPoints) {
                TacticalPointCloudVector2 thisPoint = (TacticalPointCloudVector2) v;
                double thisDistance = thisPoint.distanceTo(pt);
                if (closest == null) {
                    closest = thisPoint;
                    distance = thisDistance;
                } else {
                    if (thisDistance < distance) {
                        closest = thisPoint;
                        distance = thisDistance;
                    }
                }
            }

            if (closest == null) {
                return null;
            }
            return closest.star;
        }
    }

    private class TacticalControlField extends ControlField {
        public TacticalControlField(PointCloud pointCloud, Voronoi voronoi) {
            super(pointCloud, voronoi);
        }
/*
        public void render(Canvas canvas, Paint paint) {
            Path path = new Path();
            for (Vector2 pt : mOwnedPoints) {
                List<Triangle> triangles = mVoronoi.getTrianglesForPoint(pt);
                if (triangles == null) {
                    continue;
                }

                path.moveTo((float) triangles.get(0).centre.x * 256.0f + mDragOffsetX,
                            (float) triangles.get(0).centre.y * 256.0f + mDragOffsetY);
                for (int i = 1; i < triangles.size(); i++) {
                    path.lineTo((float) triangles.get(i).centre.x * 256.0f + mDragOffsetX,
                                (float) triangles.get(i).centre.y * 256.0f + mDragOffsetY);
                }
                path.lineTo((float) triangles.get(0).centre.x * 256.0f + mDragOffsetX,
                            (float) triangles.get(0).centre.y * 256.0f + mDragOffsetY);
            }
            canvas.drawPath(path, paint);
        }
*/
    }

    private class TacticalVoronoi extends Voronoi {
        public TacticalVoronoi(PointCloud pc) {
            super(pc);
        }
/*
        public void renderVoronoi(Canvas canvas, Paint paint) {
            for (Vector2 pt : mPointCloud.getPoints()) {
                List<Triangle> triangles = mPointCloudToTriangles.get(pt);
                if (triangles == null) {
                    // shouldn't happen, but just in case...
                    continue;
                }

                for (int i = 0; i < triangles.size() - 1; i++) {
                    Vector2 p1 = triangles.get(i).centre;
                    Vector2 p2 = triangles.get(i+1).centre;
                    canvas.drawLine((float) p1.x * 256.0f, (float) p1.y * 256.0f,
                                    (float) p2.x * 256.0f, (float) p2.y * 256.0f, paint);
                }
                Vector2 p1 = triangles.get(0).centre;
                Vector2 p2 = triangles.get(triangles.size() - 1).centre;
                canvas.drawLine((float) p1.x * 256.0f, (float) p1.y * 256.0f,
                                (float) p2.x * 256.0f, (float) p2.y * 256.0f, paint);
            }
        }
*/
    }

    private static class TacticalPointCloudVector2 extends Vector2 {
        public BaseStar star;

        public TacticalPointCloudVector2(double x, double y, BaseStar star) {
            super(x, y);
            this.star = star;
        }
    }

    public interface DoubleTapHandler {
        void onDoubleTapped(BaseStar star);
    }
}
