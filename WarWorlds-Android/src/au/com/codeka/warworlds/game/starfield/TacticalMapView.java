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
import au.com.codeka.Pair;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Triangle;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.controlfield.ControlField;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

public class TacticalMapView extends SectorView {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapView.class);

    private Paint mPointPaint;
    private Context mContext;

    public TacticalMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mContext = context;
        mSectorRadius = 2;

        log.info("Tactical map initializing...");
        mPointPaint = new Paint();
        mPointPaint.setARGB(255, 255, 0, 0);
        mPointPaint.setStyle(Style.STROKE);
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

        canvas.drawColor(Color.BLACK);
        List<Pair<Long, Long>> missingSectors = drawScene(canvas);

        if (missingSectors != null) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }
    }

    private List<Pair<Long, Long>> drawScene(Canvas canvas) {
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

                int sx = (int)((x * SectorManager.SECTOR_SIZE) + mOffsetX);
                int sy = (int)((y * SectorManager.SECTOR_SIZE) + mOffsetY);

                for (Star star : sector.getStars()) {
                    int starX = sx + star.getOffsetX();
                    int starY = sy + star.getOffsetY();
                    Vector2 pt = Vector2.pool.borrow().reset(starX / 512.0, starY / 512.0);

                    TreeSet<String> doneEmpires = new TreeSet<String>();
                    for (Colony c : star.getColonies()) {
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

        TacticalPointCloud pc = new TacticalPointCloud(points);
        TacticalVoronoi v = new TacticalVoronoi(pc);
        TacticalControlField cf = new TacticalControlField(pc, v);

        for (String empireKey : empirePoints.keySet()) {
            Empire empire = EmpireManager.getInstance().getEmpire(mContext, empireKey);
            if (empire == null) {
                EmpireManager.getInstance().fetchEmpire(mContext, empireKey, null);
            }

            List<Vector2> pts = empirePoints.get(empireKey);
            for (Vector2 pt : pts) {
                cf.addPointToControlField(pt);
            }

            Paint paint = new Paint();
            paint.setStyle(Style.FILL);
            if (empire == null) {
                paint.setARGB(128, 255, 255, 255);
            } else {
                paint.setColor(empire.getShieldColor());
            }
            cf.render(canvas, paint);
            cf.clear();
        }

        pc.render(canvas);

        return missingSectors;
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

            redraw();
            return false;
        }
    }

    private class TacticalPointCloud extends PointCloud {
        public TacticalPointCloud(ArrayList<Vector2> points) {
            super(points);
        }

        public void render(Canvas canvas) {
            for (Vector2 p : mPoints) {
                float x = (float)(p.x * 256.0);
                float y = (float)(p.y * 256.0);
                canvas.drawCircle(x, y, 5.0f, mPointPaint);
            }
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

                path.moveTo((float) triangles.get(0).centre.x * 256.0f,
                            (float) triangles.get(0).centre.y * 256.0f);
                for (int i = 1; i < triangles.size(); i++) {
                    path.lineTo((float) triangles.get(i).centre.x * 256.0f,
                                (float) triangles.get(i).centre.y * 256.0f);
                }
                path.lineTo((float) triangles.get(0).centre.x * 256.0f,
                            (float) triangles.get(0).centre.y * 256.0f);
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
}
