package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.DrawMode;
import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES20;
import au.com.codeka.TexturedMesh;

/**
 * This entity is added to the currently-selected star and shows the current radius of the star's radar.
 */
public class RadarIndicatorEntity extends Entity {
    private TexturedMesh mMesh;
    private static MyShaderProgram sShaderProgram = new MyShaderProgram();

    public static ShaderProgram getShaderProgram() {
        return sShaderProgram;
    }

    public RadarIndicatorEntity(VertexBufferObjectManager vertexBufferObjectManager) {
        super(0, 0, 1, 1);

        float[] vertexData = new float[TexturedMesh.VERTEX_SIZE * 4];
        vertexData[0 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_X] = 0.0f;
        vertexData[0 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_Y] = 0.0f;
        vertexData[1 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_X] = 0.0f;
        vertexData[1 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_Y] = 1.0f;
        vertexData[1 * TexturedMesh.VERTEX_SIZE + TexturedMesh.TEXTURECOORDINATES_INDEX_V] = 1.0f;
        vertexData[2 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_X] = 1.0f;
        vertexData[2 * TexturedMesh.VERTEX_SIZE + TexturedMesh.TEXTURECOORDINATES_INDEX_U] = 1.0f;
        vertexData[2 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_Y] = 0.0f;
        vertexData[3 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_X] = 1.0f;
        vertexData[3 * TexturedMesh.VERTEX_SIZE + TexturedMesh.TEXTURECOORDINATES_INDEX_U] = 1.0f;
        vertexData[3 * TexturedMesh.VERTEX_SIZE + TexturedMesh.VERTEX_INDEX_Y] = 1.0f;
        vertexData[3 * TexturedMesh.VERTEX_SIZE + TexturedMesh.TEXTURECOORDINATES_INDEX_V] = 1.0f;

        mMesh = new TexturedMesh(0.0f, 0.0f, vertexData, 4, DrawMode.TRIANGLE_STRIP,
                vertexBufferObjectManager);
        mMesh.setShaderProgram(sShaderProgram);
        attachChild(mMesh);
    }

    private static class MyShaderProgram extends ShaderProgram {
        public static int sUniformModelViewPositionMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;

        public static final String VERTEXSHADER =
                "uniform mat4 " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n" +
                "attribute vec4 " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
                "attribute vec2 " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
                "varying vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
                "void main() {\n" +
                "   " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + " = " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
                "   gl_Position = " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + " * " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
                "}";

        public static final String FRAGMENTSHADER =
                "precision mediump float;\n" +
                "varying vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
                "void main() {\n" +
                "   mediump float dist = distance(" + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ", vec2(0.5, 0.5));\n" +
                "   if (dist > 0.5) { dist = 0.0; }\n" +
                "   else if (dist > 0.495) { dist = 0.66; }\n" +
                "   else { dist *= 0.3; }\n" +
                "   gl_FragColor = vec4(0.0, 1.0, 0.0, dist);\n" +
                "}";

        public MyShaderProgram() {
            super(VERTEXSHADER, FRAGMENTSHADER);
        }

        @Override
        protected void link(final GLState pGLState) throws ShaderProgramLinkException {
            GLES20.glBindAttribLocation(mProgramID, ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION);
            GLES20.glBindAttribLocation(mProgramID, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES);

             super.link(pGLState);
 
             sUniformModelViewPositionMatrixLocation = getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
        }  
 
       @Override
       public void bind(final GLState pGLState, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
           GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

           super.bind(pGLState, pVertexBufferObjectAttributes);

           GLES20.glUniformMatrix4fv(sUniformModelViewPositionMatrixLocation, 1, false, pGLState.getModelViewProjectionGLMatrix(), 0);
       }

       @Override
       public void unbind(final GLState pGLState) {
           GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

           super.unbind(pGLState);
       }

    }
}
