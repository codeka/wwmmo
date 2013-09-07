package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.common.Pair;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Triangle;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Sector;
import au.com.codeka.common.model.Star;
import au.com.codeka.controlfield.ControlField;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.SectorManager;

public class TacticalMapView extends SectorView
                             implements SectorManager.OnSectorListChangedListener {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapView.class);

    private Paint mPointPaint;
    private Paint mInfluencePaint;
    private DoubleTapHandler mDoubleTapHandler;

    private float mDragOffsetX;
    private float mDragOffsetY;

    TacticalPointCloud mPointCloud;
    TacticalVoronoi mVoronoi;
    TreeMap<String, TacticalControlField> mControlFields;

    public TacticalMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mSectorRadius = 2;

        log.info("Tactical map initializing...");
        mPointPaint = new Paint();
        mPointPaint.setARGB(255, 255, 0, 0);
        mPointPaint.setStyle(Style.FILL);

        mInfluencePaint = new Paint();
        mInfluencePaint.setStyle(Style.FILL);
    }

    public void setDoubleTapHandler(DoubleTapHandler handler) {
        mDoubleTapHandler = handler;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SectorManager.i.addSectorListChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SectorManager.i.removeSectorListChangedListener(this);
    }

    @Override
    public void onSectorListChanged() {
        super.onSectorListChanged();

        refreshControlField();
    }

    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mScrollToCentre) {
            scroll(getWidth() /*/ getPixelScale()*/,
                   getHeight() /*/ getPixelScale()*/);
            mScrollToCentre = false;
        }

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
                mInfluencePaint.setColor(EmpireHelper.getShieldColor(empire));
            }
            cf.render(canvas, mInfluencePaint);
        }

        mPointCloud.render(canvas);
    }

    private void refreshControlField() {
        SectorManager sm = SectorManager.i;

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

                int sx = (int)((x * Model.SECTOR_SIZE) + mOffsetX);
                int sy = (int)((y * Model.SECTOR_SIZE) + mOffsetY);

                for (Star star : sector.stars) {
                    int starX = sx + star.offset_x;
                    int starY = sy + star.offset_y;
                    TacticalPointCloudVector2 pt = new TacticalPointCloudVector2(
                            starX / 512.0, starY / 512.0, star);

                    TreeSet<String> doneEmpires = new TreeSet<String>();
                    for (Colony c : star.colonies) {
                        String empireKey = c.empire_key;
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

        mDragOffsetX = 0.0f;
        mDragOffsetY = 0.0f;

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
            SectorManager.i.requestSectors(missingSectors, false, null);
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
            scroll(-(float)(distanceX * 2.0 / getPixelScale()),
                   -(float)(distanceY * 2.0 / getPixelScale()));

            mDragOffsetX += -(float)(distanceX * 2.0 / getPixelScale());
            mDragOffsetY += -(float)(distanceY * 2.0 / getPixelScale());

            redraw();
            return false;
        }

        /**
         * Double-tapping will take you back to the Starfield view with that particular star
         * in the centre.
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float tapX = (e.getX() - mDragOffsetX) / 256.0f;
            float tapY = (e.getY() - mDragOffsetY) / 256.0f;

            Star star = mPointCloud.findStarNear(new Vector2(tapX, tapY));
            if (star != null) {
                if (mDoubleTapHandler != null) {
                   mDoubleTapHandler.onDoubleTapped(star);
                }
            }

            return false;
        }
    }

    private class TacticalPointCloud extends PointCloud {
        public TacticalPointCloud(ArrayList<Vector2> points) {
            super(points);
        }

        public void render(Canvas canvas) {
            for (Vector2 p : mPoints) {
                float x = (float)(p.x * 256.0) + mDragOffsetX;
                float y = (float)(p.y * 256.0) + mDragOffsetY;
                canvas.drawCircle(x, y, 5.0f, mPointPaint);
            }
        }

        /**
         * Finds the star nearest the given point.
         */
        public Star findStarNear(Vector2 pt) {
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
        public Star star;

        public TacticalPointCloudVector2(double x, double y, Star star) {
            super(x, y);
            this.star = star;
        }
    }

    public interface DoubleTapHandler {
        void onDoubleTapped(Star star);
    }
}
