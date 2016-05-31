package au.com.codeka.warworlds.client.starfield;

import android.opengl.GLES20;
import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.client.opengl.Shader;
import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.Log;

/**
 * Base class for {@link SceneObject}s that we want to use to "indicate" something (radar radius,
 * selection, etc). Basically it draws a solid circle and a bit of shading in the middle.
 */
public class BaseIndicatorSceneObject extends SceneObject {
  private static final Log log = new Log("BISO");
  private final IndicatorShader shader;
  private final FloatBuffer positionBuffer;
  private final FloatBuffer texCoordBuffer;
  private final ShortBuffer indexBuffer;

  private static final float SQUARE_COORDS[] = {
      -0.5f,  0.5f, 0.0f, // top left
      -0.5f, -0.5f, 0.0f, // bottom left
      0.5f, -0.5f, 0.0f, // bottom right
      0.5f,  0.5f, 0.0f, // top right
  };
  private static final short SQUARE_INDICES[] = { 0, 1, 2, 0, 2, 3 };

  public BaseIndicatorSceneObject(DimensionResolver dimensionResolver, Colour colour) {
    super(dimensionResolver);
    this.shader = new IndicatorShader(colour);

    // initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer bb = ByteBuffer.allocateDirect(SQUARE_COORDS.length * 4);
    bb.order(ByteOrder.nativeOrder());
    positionBuffer = bb.asFloatBuffer();
    positionBuffer.put(SQUARE_COORDS);
    positionBuffer.position(0);

    float[] uvs = new float[] {
        0.0f, 0.0f, // top left
        0.0f, 1.0f, // bottom left
        1.0f, 1.0f, // bottom right
        1.0f, 0.0f, // top right
    };
    bb = ByteBuffer.allocateDirect(uvs.length * 4);
    bb.order(ByteOrder.nativeOrder());
    texCoordBuffer = bb.asFloatBuffer();
    texCoordBuffer.put(uvs);
    texCoordBuffer.position(0);

    // initialize byte buffer for the draw list
    // (# of coordinate values * 2 bytes per short)
    bb = ByteBuffer.allocateDirect(SQUARE_INDICES.length * 2);
    bb.order(ByteOrder.nativeOrder());
    indexBuffer = bb.asShortBuffer();
    indexBuffer.put(SQUARE_INDICES);
    indexBuffer.position(0);
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    Threads.checkOnThread(Threads.GL);

    shader.begin();
    GLES20.glVertexAttribPointer(
        shader.getPosHandle(), 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
    GLES20.glVertexAttribPointer(
        shader.getTexCoordHandle(), 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
    GLES20.glUniformMatrix4fv(shader.getMvpMatrixHandle(), 1, false, mvpMatrix, 0);

    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, SQUARE_INDICES.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    shader.end();
  }

  /** {@link Shader} for the indicator entity. */
  private static class IndicatorShader extends Shader {
    private final float[] color;

    private int posHandle;
    private int texCoordHandle;
    private int colourHandle;

    public IndicatorShader(Colour colour) {
      this.color = new float[] {
          (float) colour.r,
          (float) colour.g,
          (float) colour.b
      };
    }

    public int getPosHandle() {
      return posHandle;
    }

    public int getTexCoordHandle() {
      return texCoordHandle;
    }

    @Override
    protected String getVertexShaderCode() {
      return TextUtils.join("\n", new String[]{
          "uniform mat4 uMvpMatrix;",
          "attribute vec4 aPosition;",
          "attribute vec2 aTexCoord;",
          "varying vec2 vTexCoord;",
          "void main() {",
          "  vTexCoord = aTexCoord;",
          "  gl_Position = uMvpMatrix * aPosition;",
          "}"});
    }

    @Override
    protected String getFragmentShaderCode() {
      log.info("HERE 1");
      return TextUtils.join("\n", new String[]{
          "precision mediump float;",
          "varying vec2 vTexCoord;",
          "uniform vec3 uColour;",
          "void main() {",
          "   float dist = distance(vTexCoord, vec2(0.5, 0.5));",
          "   if (dist > 0.5) { dist = 0.0; }",
          "   else if (dist > 0.495) { dist = 0.66; }",
          "   else { dist *= 0.3; }",
          "   gl_FragColor = vec4(uColour, dist);",
          "}"});
    }

    @Override
    protected void onCompile() {
      posHandle = getAttributeLocation("aPosition");
      texCoordHandle = getAttributeLocation("aTexCoord");
      colourHandle = getUniformLocation("uColour");
    }

    @Override
    protected void onBegin() {
      GLES20.glEnableVertexAttribArray(posHandle);
      GLES20.glEnableVertexAttribArray(texCoordHandle);
      GLES20.glUniform3fv(colourHandle, 1, color, 0);
    }

    @Override
    protected void onEnd() {
      GLES20.glDisableVertexAttribArray(posHandle);
      GLES20.glDisableVertexAttribArray(texCoordHandle);
    }
  }
}
