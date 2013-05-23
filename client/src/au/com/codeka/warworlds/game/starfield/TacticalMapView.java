package au.com.codeka.warworlds.game.starfield;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.microedition.khronos.opengles.GL10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Pair;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Triangle;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.controlfield.ControlField;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;

public class TacticalMapView extends GlSectorView
                             implements SectorManager.OnSectorListChangedListener {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapView.class);

    private DoubleTapHandler mDoubleTapHandler;
    private Context mContext;

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

        mContext = context;
        mSectorRadius = 2;

        log.info("Tactical map initializing...");
    }

    @Override
    protected GlSectorView.Renderer createRenderer() {
        return new TacticalRenderer();
    }

    public void setDoubleTapHandler(DoubleTapHandler handler) {
        mDoubleTapHandler = handler;
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

    private static class ControlFieldState {
        TreeMap<String, TacticalControlField> controlFields;
        TacticalPointCloud pointCloud;
        TacticalVoronoi voronoi;
        List<Pair<Long, Long>> missingSectors;
    }

    private void refreshControlField() {
        log.info("Refreshing control field...");

        final int sectorRadius = mSectorRadius;
        final long sectorX = mSectorX;
        final long sectorY = mSectorY;
        final float offsetX = mOffsetX;
        final float offsetY = mOffsetY;
        new BackgroundRunner<ControlFieldState>() {
            @Override
            protected ControlFieldState doInBackground() {
                ControlFieldState state = new ControlFieldState();
                SectorManager sm = SectorManager.getInstance();

                ArrayList<Vector2> points = new ArrayList<Vector2>();
                TreeMap<String, List<Vector2>> empirePoints = new TreeMap<String, List<Vector2>>();

                for(int y = -sectorRadius; y <= sectorRadius; y++) {
                    for(int x = -sectorRadius; x <= sectorRadius; x++) {
                        long sX = sectorX + x;
                        long sY = sectorY + y;

                        Sector sector = sm.getSector(sX, sY);
                        if (sector == null) {
                            if (state.missingSectors == null) {
                                state.missingSectors = new ArrayList<Pair<Long, Long>>();
                            }
                            state.missingSectors.add(new Pair<Long, Long>(sX, sY));
                            continue;
                        }

                        int sx = (int)((x * Sector.SECTOR_SIZE) + offsetX);
                        int sy = (int)((y * Sector.SECTOR_SIZE) + offsetY);

                        for (BaseStar star : sector.getStars()) {
                            int starX = sx + star.getOffsetX();
                            int starY = sy + star.getOffsetY();
                            TacticalPointCloudVector2 pt = new TacticalPointCloudVector2(
                                    starX / 512.0, starY / 512.0, star);

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

                state.controlFields = new TreeMap<String, TacticalControlField>();
                state.pointCloud = new TacticalPointCloud(points);
                state.voronoi = new TacticalVoronoi(state.pointCloud);

                for (String empireKey : empirePoints.keySet()) {
                    TacticalControlField cf = new TacticalControlField(mPointCloud, state.voronoi);

                    List<Vector2> pts = empirePoints.get(empireKey);
                    for (Vector2 pt : pts) {
                        cf.addPointToControlField(pt);
                    }

                    state.controlFields.put(empireKey, cf);
                }

                return state;
            }

            @Override
            protected void onComplete(ControlFieldState state) {
                mDragOffsetX = mOffsetX - offsetX;
                mDragOffsetY = mOffsetY - offsetY;

                if (mPointCloud != null) {
                    mPointCloud.close();
                }
                if (mControlFields != null) {
                    for (TacticalControlField cf : mControlFields.values()) {
                        cf.close();
                    }
                }

                mControlFields = state.controlFields;
                mPointCloud = state.pointCloud;

                if (state.missingSectors != null) {
                    SectorManager.getInstance().requestSectors(state.missingSectors, false, null);
                }

                requestRender();
            }
        }.execute();

        log.info("Refresh complete.");
    }

    protected int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        checkError("glCompileShader");
        return shader;
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
            scroll(-(float)(distanceX / getPixelScale()),
                   -(float)(distanceY / getPixelScale()));

            mDragOffsetX -= (float)(distanceX / getPixelScale());
            mDragOffsetY -= (float)(distanceY / getPixelScale());

            requestRender();
            return false;
        }

        /**
         * Double-tapping will take you back to the Starfield view with that particular star
         * in the centre.
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float tapX = (e.getX() - (getWidth() / 2.0f) - mDragOffsetX) / 256.0f;
            float tapY = (e.getY() - (getHeight() / 2.0f) - mDragOffsetY) / 256.0f;
            log.info(String.format("drag = %.2f %.2f", mDragOffsetX, mDragOffsetY));
            log.info(String.format("evnt = %.2f %.2f", e.getX(), e.getY()));

            BaseStar star = mPointCloud.findStarNear(new Vector2(tapX, tapY));
            if (star != null) {
                if (mDoubleTapHandler != null) {
                    mDoubleTapHandler.onDoubleTapped(star);
                }
            }

            return false;
        }
    }

    private class TacticalPointCloud extends PointCloud {
        private FloatBuffer mVertexBuffer;
        private int mVertexShader;
        private int mFragmentShader;
        private int mProgram;

        private final int COORDS_PER_VERTEX = 3;

        // todo: resources..?
        private final String mVertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}";

        private final String mFragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

        float mColour[] = {1.0f, 0.0f, 0.0f, 1.0f};

        int mColourHandle;
        int mMvpHandle;
        int mPositionHandle;

        public TacticalPointCloud(List<Vector2> points) {
            super(new ArrayList<Vector2>(points));
        }

        public void render(float[] viewProjMatrix, float dragOffsetX, float dragOffsetY) {
            if (mVertexBuffer == null) {
                setup();
            }

            GLES20.glUseProgram(mProgram);
            GLES20.glUniform4fv(mColourHandle, 1, mColour, 0);

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    COORDS_PER_VERTEX * 4, mVertexBuffer);

            float[] modelMatrix = new float[16];
            float[] mvpMatrix = new float[16];
            for (Vector2 p : mPoints) {
                float x = (float)p.x - (dragOffsetX / 256.0f);
                float y = (float)p.y - (dragOffsetY / 256.0f);

                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, modelMatrix, 0, x, y, 0.0f);
                Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(mMvpHandle, 1, false, mvpMatrix, 0);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 10);
            }

            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }

        private void setup() {
            float[] fpts = new float[3 * 10];
            fpts[0 + 0] = 0.0f;
            fpts[0 + 1] = 0.0f;
            fpts[0 + 2] = 0.0f;
            for (int n = 0; n < 9; n++) {
                float x = (float) Math.cos(Math.PI / 4.0f * n) * (5.0f / 256.0f);
                float y = (float) Math.sin(Math.PI / 4.0f * n) * (5.0f / 256.0f);
                fpts[(n + 1) * 3 + 0] = x;
                fpts[(n + 1) * 3 + 1] = y;
                fpts[(n + 1) * 3 + 2] = 0.0f;
            }

            ByteBuffer bb = ByteBuffer.allocateDirect(fpts.length * 4);
            bb.order(ByteOrder.nativeOrder());
            mVertexBuffer = bb.asFloatBuffer();
            mVertexBuffer.put(fpts);
            mVertexBuffer.position(0);

            mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
            mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mFragmentShader);
            GLES20.glLinkProgram(mProgram);
            checkError("glLinkProgram");

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mColourHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            mMvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        }

        private void close() {
            GLES20.glDeleteProgram(mProgram);
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
                if (closest == null || thisDistance < distance) {
                    closest = thisPoint;
                    distance = thisDistance;
                }
            }

            if (closest == null) {
                return null;
            }
            return closest.star;
        }
    }

    private class TacticalControlField extends ControlField {
        private FloatBuffer mVertexBuffer;
        private int mVertexShader;
        private int mFragmentShader;
        private int mProgram;

        private final int COORDS_PER_VERTEX = 3;
        private int numVertices;

        private int mColourHandle;
        private int mMvpHandle;
        private int mPositionHandle;

        // todo: resources..?
        private final String mVertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}";

        private final String mFragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

        public TacticalControlField(PointCloud pointCloud, Voronoi voronoi) {
            super(pointCloud, voronoi);

        }

        public void render(float[] mvpMatrix, float[] colour) {
            if (mVertexBuffer == null) {
                setup();
            }

            GLES20.glUseProgram(mProgram);
            GLES20.glUniform4fv(mColourHandle, 1, colour, 0);

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    COORDS_PER_VERTEX * 4, mVertexBuffer);

            GLES20.glUniformMatrix4fv(mMvpHandle, 1, false, mvpMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, numVertices);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }

        private void setup() {
            numVertices = 0;
            for (Vector2 pt : mOwnedPoints) {
                List<Triangle> triangles = mVoronoi.getTrianglesForPoint(pt);
                if (triangles == null) {
                    continue;
                }
                numVertices += 2 + triangles.size();
            }

            float[] points = new float[numVertices * 3];
            int n = 0;
            for (Vector2 pt : mOwnedPoints) {
                List<Triangle> triangles = mVoronoi.getTrianglesForPoint(pt);
                if (triangles == null) {
                    continue;
                }

                points[n++] = (float) pt.x;
                points[n++] = (float) pt.y;
                points[n++] = 0.0f;
                points[n++] = (float) triangles.get(0).centre.x;
                points[n++] = (float) triangles.get(0).centre.y;
                points[n++] = 0.0f;
                for (int i = 1; i < triangles.size(); i++) {
                    points[n++] = (float) triangles.get(i).centre.x;
                    points[n++] = (float) triangles.get(i).centre.y;
                    points[n++] = 0.0f;
                }
                points[n++] = (float) triangles.get(0).centre.x;
                points[n++] = (float) triangles.get(0).centre.y;
                points[n++] = 0.0f;
            }

            ByteBuffer bb = ByteBuffer.allocateDirect(points.length * 4);
            bb.order(ByteOrder.nativeOrder());
            mVertexBuffer = bb.asFloatBuffer();
            mVertexBuffer.put(points);
            mVertexBuffer.position(0);

            mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
            mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mFragmentShader);
            GLES20.glLinkProgram(mProgram);
            checkError("glLinkProgram");

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mColourHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            mMvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        }

        public void close() {
            GLES20.glDeleteProgram(mProgram);
        }
    }

    private class TacticalVoronoi extends Voronoi {
        public TacticalVoronoi(PointCloud pc) {
            super(pc);
        }
    }

    private static class TacticalPointCloudVector2 extends Vector2 {
        public BaseStar star;

        public TacticalPointCloudVector2(double x, double y, BaseStar star) {
            super(x, y);
            this.star = star;
        }
    }

    private class TacticalRenderer extends GlSectorView.Renderer {
        @Override
        public void onDrawFrame(GL10 _) {
            super.onDrawFrame(_);

            if (mControlFields == null) {
                return;
            }

            float dragOffsetX = mDragOffsetX;
            float dragOffsetY = mDragOffsetY;

            float[] modelMatrix = new float[16];
            float[] mvpMatrix = new float[16];

            float x = -dragOffsetX / 256.0f;
            float y = -dragOffsetY / 256.0f;

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, modelMatrix, 0, x, y, 0.0f);
            Matrix.multiplyMM(mvpMatrix, 0, mViewProjMatrix, 0, modelMatrix, 0);

            for (String empireKey : mControlFields.keySet()) {
                Empire empire = EmpireManager.getInstance().getEmpire(mContext, empireKey);
                if (empire == null) {
                    //TODO EmpireManager.getInstance().fetchEmpire(mContext, empireKey, null);
                }
                TacticalControlField cf = mControlFields.get(empireKey);

                float[] colour;
                if (empire == null) {
                    colour = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
                } else {
                    colour = empire.getShieldColorFloats();
                }
                cf.render(mvpMatrix, colour);
            }

            mPointCloud.render(mViewProjMatrix, dragOffsetX, dragOffsetY);
        }
    }

    public interface DoubleTapHandler {
        void onDoubleTapped(BaseStar star);
    }
}
