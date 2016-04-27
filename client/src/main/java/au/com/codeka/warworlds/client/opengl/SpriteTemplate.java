package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A {@link SpriteTemplate} describes a sprite's characteristics (texture, bounds within the texture
 * and so on). You would use it to add an actual {@link Sprite} to a {@link Scene}.
 */
public class SpriteTemplate {
  private static final float SQUARE_COORDS[] = {
      -0.5f,  0.5f, 0.0f, // top left
      -0.5f, -0.5f, 0.0f, // bottom left
       0.5f, -0.5f, 0.0f, // bottom right
       0.5f,  0.5f, 0.0f, // top right
  };
  private static final float SQUARE_UVS[] = {
      0.0f, 1.0f, // top left
      0.0f, 0.0f, // bottom left
      1.0f, 0.0f, // bottom right
      1.0f, 1.0f, // top right
  };

  private static final short SQUARE_INDICES[] = { 0, 1, 2, 0, 2, 3 };

  private final FloatBuffer vertexBuffer;
  private final FloatBuffer uvBuffer;
  private final ShortBuffer indexBuffer;
  private final SpriteShader shader;
  private final TextureBitmap texture;

  public SpriteTemplate(SpriteShader shader, TextureBitmap texture) {
    this.shader = shader;
    this.texture = texture;

    // initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer bb = ByteBuffer.allocateDirect(SQUARE_COORDS.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(SQUARE_COORDS);
    vertexBuffer.position(0);

    bb = ByteBuffer.allocateDirect(SQUARE_UVS.length * 4);
    bb.order(ByteOrder.nativeOrder());
    uvBuffer = bb.asFloatBuffer();
    uvBuffer.put(SQUARE_UVS);
    uvBuffer.position(0);

    // initialize byte buffer for the draw list
    // (# of coordinate values * 2 bytes per short)
    bb = ByteBuffer.allocateDirect(SQUARE_INDICES.length * 2);
    bb.order(ByteOrder.nativeOrder());
    indexBuffer = bb.asShortBuffer();
    indexBuffer.put(SQUARE_INDICES);
    indexBuffer.position(0);
  }

  public void draw() {
    shader.begin();
    texture.bind();
    GLES20.glVertexAttribPointer(
        shader.getPositionHandle(), 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glVertexAttribPointer(
        shader.getTexCoordHandle(), 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, SQUARE_INDICES.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    shader.end();
  }
}
