package au.com.codeka.warworlds.opengl;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A {@link SceneObject} that represents a piece of text.
 */
public class TextSceneObject extends SceneObject {
  private final DimensionResolver dimensionResolver;
  private final SpriteShader shader;
  private final TextTexture textTexture;

  private String text;
  private float textWidth;
  private boolean dirty;
  private FloatBuffer positionBuffer;
  private FloatBuffer texCoordBuffer;
  private ShortBuffer indexBuffer;

  public TextSceneObject(
      DimensionResolver dimensionResolver,
      SpriteShader shader,
      TextTexture textTexture,
      String text) {
    super(dimensionResolver);
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.shader = Preconditions.checkNotNull(shader);
    this.textTexture = Preconditions.checkNotNull(textTexture);
    this.text = text;
    this.dirty = true;
  }

  public void setTextSize(float dp) {
    float px = dimensionResolver.dp2px(dp);
    float scale = px / textTexture.getTextHeight();
    Matrix.scaleM(matrix, 0, scale, scale, 1.0f);
  }

  public String getText() {
    return text;
  }

  public float getTextWidth() {
    if (dirty) {
      // If it's dirty, we'll have to measure the text ourselves... we can't create the buffers
      // since that must happen on the GL thread.
      textWidth = measureText();
    }

    return dimensionResolver.px2dp(textWidth);
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    //TODO Threads.checkOnThread(Threads.GL);

    if (dirty) {
      createBuffers();
    }

    shader.begin();
    textTexture.bind();
    GLES20.glVertexAttribPointer(
        shader.getPositionHandle(), 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
    GLES20.glVertexAttribPointer(
        shader.getTexCoordHandle(), 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
    GLES20.glUniformMatrix4fv(shader.getMvpMatrixHandle(), 1, false, mvpMatrix, 0);

    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, indexBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    shader.end();
  }

  private float measureText() {
    float width = 0.0f;
    for (int i = 0; i < text.length(); i++) {
      Rect bounds = textTexture.getCharBounds(text.charAt(i));
      width += bounds.width();
    }
    return width;
  }

  private void createBuffers() {
    // # of chars * 4 verts per char * 3 components per vert
    float[] positions = new float[text.length() * 4 * 3];

    // # of chars * 4 verts per char * 2 components per vert
    float[] uvs = new float[text.length() * 4 * 2];

    // # of chars * 2 triangles * 3 verts per triangle
    short[] indices = new short[text.length() * 2 * 3];

    float offsetX = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      Rect bounds = textTexture.getCharBounds(ch);
      positions[(i * 12)] = offsetX;
      positions[(i * 12) + 1] = -bounds.height() / 2.0f;
      positions[(i * 12) + 2] = 0.0f;
      positions[(i * 12) + 3] = offsetX + bounds.width();
      positions[(i * 12) + 4] = -bounds.height() / 2.0f;
      positions[(i * 12) + 5] = 0.0f;
      positions[(i * 12) + 6] = offsetX;
      positions[(i * 12) + 7] = bounds.height() / 2.0f;
      positions[(i * 12) + 8] = 0.0f;
      positions[(i * 12) + 9] = offsetX + bounds.width();
      positions[(i * 12) + 10] = bounds.height() / 2.0f;
      positions[(i * 12) + 11] = 0.0f;
      offsetX += bounds.width();

      uvs[(i * 8)] = (float) bounds.left / textTexture.getWidth();
      uvs[(i * 8) + 1] = (float) bounds.bottom / textTexture.getHeight();
      uvs[(i * 8) + 2] = (float) bounds.right / textTexture.getWidth();
      uvs[(i * 8) + 3] = (float) bounds.bottom / textTexture.getHeight();
      uvs[(i * 8) + 4] = (float) bounds.left / textTexture.getWidth();
      uvs[(i * 8) + 5] = (float) bounds.top / textTexture.getHeight();
      uvs[(i * 8) + 6] = (float) bounds.right / textTexture.getWidth();
      uvs[(i * 8) + 7] = (float) bounds.top / textTexture.getHeight();

      indices[(i * 6)] = (short) (i * 4);
      indices[(i * 6) + 1] = (short) ((i * 4) + 1);
      indices[(i * 6) + 2] = (short) ((i * 4) + 2);
      indices[(i * 6) + 3] = (short) ((i * 4) + 1);
      indices[(i * 6) + 4] = (short) ((i * 4) + 3);
      indices[(i * 6) + 5] = (short) ((i * 4) + 2);
    }
    textWidth = offsetX;

    ByteBuffer bb = ByteBuffer.allocateDirect(positions.length * 4);
    bb.order(ByteOrder.nativeOrder());
    positionBuffer = bb.asFloatBuffer();
    positionBuffer.put(positions);
    positionBuffer.position(0);

    bb = ByteBuffer.allocateDirect(uvs.length * 4);
    bb.order(ByteOrder.nativeOrder());
    texCoordBuffer = bb.asFloatBuffer();
    texCoordBuffer.put(uvs);
    texCoordBuffer.position(0);

    bb = ByteBuffer.allocateDirect(indices.length * 2);
    bb.order(ByteOrder.nativeOrder());
    indexBuffer = bb.asShortBuffer();
    indexBuffer.put(indices);
    indexBuffer.position(0);

    dirty = false;
  }
}
