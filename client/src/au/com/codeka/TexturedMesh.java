package au.com.codeka;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.shape.Shape;
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.DrawType;
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject;
import org.andengine.opengl.vbo.IVertexBufferObject;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttribute;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder;

import android.opengl.GLES20;

/** A @see Mesh that includes texture coordinates as well (useful for coupling with a fragment shader). */
public class TexturedMesh extends Shape {
    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = 1;
    public static final int COLOR_INDEX = 2;
    public static final int TEXTURECOORDINATES_INDEX_U = 3;
    public static final int TEXTURECOORDINATES_INDEX_V = 4;

    public static final int VERTEX_SIZE = 5;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(3)
        .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
        .add(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
        .add(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES, 2, GLES20.GL_FLOAT, false)
        .build();

    protected final ITexturedMeshVertexBufferObject mMeshVertexBufferObject;
    private int mVertexCountToDraw;
    private int mDrawMode;
    protected ITextureRegion mTextureRegion;

    /**
     * Uses a default {@link HighPerformanceTexturedMeshVertexBufferObject} in {@link DrawType#STATIC} with the {@link VertexBufferObjectAttribute}s: {@link Mesh#VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT}.
     */
    public TexturedMesh(final float pX, final float pY, final float[] pBufferData, final int pVertexCount, final DrawMode pDrawMode, final VertexBufferObjectManager pVertexBufferObjectManager) {
        this(pX, pY, pBufferData, pVertexCount, pDrawMode, null, pVertexBufferObjectManager, DrawType.STATIC);
    }

    /**
     * Uses a default {@link HighPerformanceTexturedMeshVertexBufferObject} in {@link DrawType#STATIC} with the {@link VertexBufferObjectAttribute}s: {@link Mesh#VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT}.
     */
    public TexturedMesh(final float pX, final float pY, final float[] pBufferData, final int pVertexCount, final DrawMode pDrawMode, final ITextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager) {
        this(pX, pY, pBufferData, pVertexCount, pDrawMode, pTextureRegion, pVertexBufferObjectManager, DrawType.STATIC);
    }

    /**
     * Uses a default {@link HighPerformanceTexturedMeshVertexBufferObject} with the {@link VertexBufferObjectAttribute}s: {@link Mesh#VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT}.
     */
    public TexturedMesh(final float pX, final float pY, final float[] pBufferData, final int pVertexCount, final DrawMode pDrawMode, final VertexBufferObjectManager pVertexBufferObjectManager, final DrawType pDrawType) {
        this(pX, pY, pBufferData, pVertexCount, pDrawMode, null, pVertexBufferObjectManager, pDrawType);
    }

    /**
     * Uses a default {@link HighPerformanceTexturedMeshVertexBufferObject} with the {@link VertexBufferObjectAttribute}s: {@link Mesh#VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT}.
     */
    public TexturedMesh(final float pX, final float pY, final float[] pBufferData, final int pVertexCount, final DrawMode pDrawMode, final ITextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager, final DrawType pDrawType) {
        this(pX, pY, pVertexCount, pDrawMode, pTextureRegion, new HighPerformanceTexturedMeshVertexBufferObject(pVertexBufferObjectManager, pBufferData, pVertexCount, pDrawType, true, TexturedMesh.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT));
    }

    /**
     * Uses a default {@link HighPerformanceTexturedMeshVertexBufferObject} with the {@link VertexBufferObjectAttribute}s: {@link Mesh#VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT}.
     */
    public TexturedMesh(final float pX, final float pY, final float[] pVertexX, final float[] pVertexY, final DrawMode pDrawMode, final ITextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager, final DrawType pDrawType) {
        this(pX, pY, pVertexX.length, pDrawMode, pTextureRegion, new HighPerformanceTexturedMeshVertexBufferObject(pVertexBufferObjectManager, buildVertexList(pVertexX, pVertexY), pVertexX.length, pDrawType, true, TexturedMesh.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT));
    }

    public TexturedMesh(final float pX, final float pY, final int pVertexCount, final DrawMode pDrawMode, final ITextureRegion pTextureRegion, final ITexturedMeshVertexBufferObject pMeshVertexBufferObject) {
        super(pX, pY, PositionColorTextureCoordinatesShaderProgram.getInstance());

        mDrawMode = pDrawMode.getDrawMode();
        mTextureRegion = pTextureRegion;
        mMeshVertexBufferObject = pMeshVertexBufferObject;
        mVertexCountToDraw = pVertexCount;

        if( pTextureRegion != null) {
            setBlendingEnabled(true);
            initBlendFunction(pTextureRegion);
            onUpdateTextureCoordinates();
        }

        onUpdateVertices();
        onUpdateColor();
        mMeshVertexBufferObject.setDirtyOnHardware();
        setBlendingEnabled(true);
    }

    public float[] getBufferData() {
        return mMeshVertexBufferObject.getBufferData();
    }

    public ITextureRegion getTextureRegion() {
        return mTextureRegion;
    }

    @Override
    public ITexturedMeshVertexBufferObject getVertexBufferObject() {
        return mMeshVertexBufferObject;
    }

    @Override
    public void reset() {
        super.reset();
        if (getTextureRegion() != null) {
            initBlendFunction(getTextureRegion().getTexture());
        }
    }

    @Override
    protected void preDraw(final GLState pGLState, final Camera pCamera) {
        super.preDraw(pGLState, pCamera);

        if (mTextureRegion != null) {
            mTextureRegion.getTexture().bind(pGLState);
        }
        mMeshVertexBufferObject.bind(pGLState, mShaderProgram);
    }

    @Override
    protected void draw(final GLState pGLState, final Camera pCamera) {
        mMeshVertexBufferObject.draw(mDrawMode, mVertexCountToDraw);
    }

    @Override
    protected void postDraw(final GLState pGLState, final Camera pCamera) {
        mMeshVertexBufferObject.unbind(pGLState, mShaderProgram);
        super.postDraw(pGLState, pCamera);
    }

    @Override
    protected void onUpdateColor() {
        mMeshVertexBufferObject.onUpdateColor(this);
    }

    @Override
    protected void onUpdateVertices() {
        mMeshVertexBufferObject.onUpdateVertices(this);
    }

    protected void onUpdateTextureCoordinates() {
        mMeshVertexBufferObject.onUpdateTextureCoordinates(this);
    }

    // ===========================================================
    // Methods
    // ===========================================================
    protected static float[] buildVertexList(float[] pVertexX, float[] pVertexY)
    {
        assert( pVertexX.length == pVertexY.length );

        float[] bufferData = new float[TexturedMesh.VERTEX_SIZE * pVertexX.length];
        updateVertexList(pVertexX, pVertexY, bufferData);
        return bufferData;
    }

    protected static void updateVertexList(float[] pVertexX, float[] pVertexY, float[] pBufferData) {
        for( int i = 0; i < pVertexX.length; i++) {
            pBufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.VERTEX_INDEX_X] = pVertexX[i];
            pBufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.VERTEX_INDEX_Y] = pVertexY[i];
        }
    }

    public static interface ITexturedMeshVertexBufferObject extends IVertexBufferObject {
        public float[] getBufferData();
        public void onUpdateColor(final TexturedMesh pMesh);
        public void onUpdateVertices(final TexturedMesh pMesh);
        public void onUpdateTextureCoordinates(final TexturedMesh pMesh);
    }

    public static class HighPerformanceTexturedMeshVertexBufferObject extends HighPerformanceVertexBufferObject
                                                                      implements ITexturedMeshVertexBufferObject {
        private final int mVertexCount;

        public HighPerformanceTexturedMeshVertexBufferObject(final VertexBufferObjectManager pVertexBufferObjectManager,
                                                             final float[] pBufferData, final int pVertexCount,
                                                             final DrawType pDrawType, final boolean pAutoDispose,
                                                             final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
            super(pVertexBufferObjectManager, pBufferData, pDrawType, pAutoDispose, pVertexBufferObjectAttributes);
            mVertexCount = pVertexCount;
        }

        @Override
        public void onUpdateColor(final TexturedMesh pMesh) {
            final float[] bufferData = mBufferData;
            final float packedColor = pMesh.getColor().getABGRPackedFloat();

            for(int i = 0; i < mVertexCount; i++) {
                bufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.COLOR_INDEX] = packedColor;
            }

            setDirtyOnHardware();
        }

        @Override
        public void onUpdateVertices(final TexturedMesh pMesh) {
            // Since the buffer data is managed from the caller, we just mark the buffer data as dirty.
            setDirtyOnHardware();
        }

        @Override
        public void onUpdateTextureCoordinates(final TexturedMesh pMesh) {
            final float[] bufferData = mBufferData;

            final ITextureRegion textureRegion = pMesh.getTextureRegion();
            if (textureRegion == null) {
                return;
            }
            float textureWidth = textureRegion.getWidth();
            float textureHeight = textureRegion.getHeight();

            float x0 = 0;
            float y0 = 0;
            for(int i = 0; i < mVertexCount; i++) {
                float x = bufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.VERTEX_INDEX_X];
                float y = bufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.VERTEX_INDEX_Y];

                float u = (x - x0) / textureWidth + 0.5f;
                float v = ((y - y0) / -textureHeight) + 0.5f;

                bufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.TEXTURECOORDINATES_INDEX_U] = u;
                bufferData[(i * TexturedMesh.VERTEX_SIZE) + TexturedMesh.TEXTURECOORDINATES_INDEX_V] = v;
            }

            setDirtyOnHardware();
        }
    }
}