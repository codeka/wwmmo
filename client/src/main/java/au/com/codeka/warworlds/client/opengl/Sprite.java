package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** A {@link Sprite} is basically a quad + texture. */
// TODO: use instancing or something to make this faster.
public class Sprite {
  private static final String vertexShaderCode =
      "attribute vec4 vPosition;" +
          "void main() {" +
          "  gl_Position = vPosition;" +
          "}";

  private static final String fragmentShaderCode =
      "precision mediump float;" +
          "uniform vec4 vColor;" +
          "void main() {" +
          "  gl_FragColor = vColor;" +
          "}";

  private static final int shaderProgram;

  // number of coordinates per vertex in this array
  private static final int COORDS_PER_VERTEX = 3;
  private static final float SQUARE_COORDS[] = {
      -0.5f,  0.5f, 0.0f,   // top left
      -0.5f, -0.5f, 0.0f,   // bottom left
      0.5f, -0.5f, 0.0f,   // bottom right
      0.5f,  0.5f, 0.0f }; // top right

  private static final short SQUARE_INDICES[] = { 0, 1, 2, 0, 2, 3 };

  private static final int VERTEX_COUNT = SQUARE_COORDS.length / COORDS_PER_VERTEX;
  private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

  static {
    shaderProgram = GLES20.glCreateProgram();

    int shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(shader, vertexShaderCode);
    GLES20.glCompileShader(shader);
    GLES20.glAttachShader(shaderProgram, shader);

    shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(shader, fragmentShaderCode);
    GLES20.glCompileShader(shader);
    GLES20.glAttachShader(shaderProgram, shader);

    GLES20.glLinkProgram(shaderProgram);
  }

  private final FloatBuffer vertexBuffer;
  private final ShortBuffer drawListBuffer;

  public Sprite() {
    // initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer bb = ByteBuffer.allocateDirect(SQUARE_COORDS.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(SQUARE_COORDS);
    vertexBuffer.position(0);

    // initialize byte buffer for the draw list
    // (# of coordinate values * 2 bytes per short)
    ByteBuffer dlb = ByteBuffer.allocateDirect(SQUARE_INDICES.length * 2);
    dlb.order(ByteOrder.nativeOrder());
    drawListBuffer = dlb.asShortBuffer();
    drawListBuffer.put(SQUARE_INDICES);
    drawListBuffer.position(0);
  }

  public void draw() {
    final float[] color = {1.0f, 1.0f, 1.0f, 1.0f};

    // Add program to OpenGL ES environment
    GLES20.glUseProgram(shaderProgram);

    int posHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
    GLES20.glEnableVertexAttribArray(posHandle);
    GLES20.glVertexAttribPointer(
        posHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);

    int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
    GLES20.glUniform4fv(colorHandle, 1, color, 0);

    // Draw the triangle
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, SQUARE_INDICES.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(posHandle);
  }
}
