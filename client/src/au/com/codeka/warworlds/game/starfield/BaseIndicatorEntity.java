package au.com.codeka.warworlds.game.starfield;

import android.graphics.Color;
import android.opengl.GLES20;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.DrawMode;
import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import au.com.codeka.TexturedMesh;

import static org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants
    .ATTRIBUTE_TEXTURECOORDINATES;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants
    .ATTRIBUTE_TEXTURECOORDINATES_LOCATION;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants.LOCATION_INVALID;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants.UNIFORM_COLOR;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants
    .UNIFORM_MODELVIEWPROJECTIONMATRIX;
import static org.andengine.opengl.shader.constants.ShaderProgramConstants
    .VARYING_TEXTURECOORDINATES;

/**
 * Base class for entities that display a circle around a star (e.g. {@link RadarIndicatorEntity}
 * and {@link WormholeDisruptorIndicatorEntity}).
 */
public class BaseIndicatorEntity extends Entity {
  private final TexturedMesh mesh;

  protected BaseIndicatorEntity(VertexBufferObjectManager vertexBufferObjectManager,
      IndicatorEntityShaderProgram shaderProgram) {
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

    mesh = new TexturedMesh(0.0f, 0.0f, vertexData, 4, DrawMode.TRIANGLE_STRIP,
        vertexBufferObjectManager);
    mesh.setShaderProgram(shaderProgram);
    attachChild(mesh);
  }

  /** {@link ShaderProgram} for the indicator entity. */
  static class IndicatorEntityShaderProgram extends ShaderProgram {
    public static int sUniformModelViewPositionMatrixLocation = LOCATION_INVALID;
    public static int sUniformColorLocation = LOCATION_INVALID;

    private static final String VERTEX_SHADER =
        "uniform mat4 " + UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n"
        + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
        + "attribute vec2 " + ATTRIBUTE_TEXTURECOORDINATES + ";\n"
        + "varying vec2 " + VARYING_TEXTURECOORDINATES + ";\n"
        + "void main() {\n"
        + "  " + VARYING_TEXTURECOORDINATES + " = " + ATTRIBUTE_TEXTURECOORDINATES + ";\n"
        + "  gl_Position = " + UNIFORM_MODELVIEWPROJECTIONMATRIX + " * " + ATTRIBUTE_POSITION + ";\n"
        + "}";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n"
        + "varying vec2 " + VARYING_TEXTURECOORDINATES + ";\n"
        + "uniform vec3 " + UNIFORM_COLOR + ";\n"
        + "void main() {\n"
        + "   lowp float dist = distance(" + VARYING_TEXTURECOORDINATES + ", vec2(0.5, 0.5));\n"
        + "   if (dist > 0.5) { dist = 0.0; }\n"
        + "   else if (dist > 0.495) { dist = 0.66; }\n"
        + "   else { dist *= 0.3; }\n"
        + "   gl_FragColor = vec4(" + UNIFORM_COLOR + ", dist);\n"
        + "}";

    private final float[] color;

    public IndicatorEntityShaderProgram(int color) {
      super(VERTEX_SHADER, FRAGMENT_SHADER);

      this.color = new float[] {
        Color.red(color) / 255.0f,
        Color.green(color) / 255.0f,
        Color.blue(color) / 255.0f
      };
    }

    @Override
    protected void link(final GLState pGLState) throws ShaderProgramLinkException {
      GLES20.glBindAttribLocation(mProgramID,
          ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
          ATTRIBUTE_POSITION);
      GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_TEXTURECOORDINATES_LOCATION,
          ATTRIBUTE_TEXTURECOORDINATES);

      super.link(pGLState);

      sUniformModelViewPositionMatrixLocation = getUniformLocation(UNIFORM_MODELVIEWPROJECTIONMATRIX);
      sUniformColorLocation = getUniformLocation(UNIFORM_COLOR);
    }

    @Override
    public void bind(final GLState pGLState,
        final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
      GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

      super.bind(pGLState, pVertexBufferObjectAttributes);

      GLES20.glUniformMatrix4fv(sUniformModelViewPositionMatrixLocation, 1, false,
          pGLState.getModelViewProjectionGLMatrix(), 0);
      GLES20.glUniform3f(sUniformColorLocation, color[0], color[1], color[2]);
    }

    @Override
    public void unbind(final GLState pGLState) {
      GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

      super.unbind(pGLState);
    }
  }
}
