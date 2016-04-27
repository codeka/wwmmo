package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import au.com.codeka.warworlds.common.Log;

/** A {@link Sprite} is basically a quad + texture. */
// TODO: use instancing or something to make this faster.
public class Sprite {
  private static final Log log = new Log("Sprite");

  private static final String vertexShaderCode = TextUtils.join("\n", new String[] {
      "attribute vec4 aPosition;",
      "attribute vec2 aTexCoord;",
      "varying vec2 vTexCoord;",
      "void main() {",
      "  vTexCoord = aTexCoord;",
      "  gl_Position = aPosition;",
      "}",
  });

  private static final String fragmentShaderCode = TextUtils.join("\n", new String[] {
      "precision mediump float;",
      "uniform sampler2D uTexture;",
      "varying vec2 vTexCoord;",
      "void main() {",
      "  gl_FragColor = texture2D(uTexture, vTexCoord);",
      "}",
  });

  private static final int shaderProgram;

  // number of coordinates per vertex in this array
  private static final int COORDS_PER_VERTEX = 5;
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

  private static final int VERTEX_COUNT = SQUARE_COORDS.length / COORDS_PER_VERTEX;
  private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

  private static int posHandle;
  private static int texCoordHandle;

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

    int[] linkStatus = new int[1];
    GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
    if (GLES20.GL_TRUE != linkStatus[0]) {
      log.error("Could not link program: %s", GLES20.glGetProgramInfoLog(shaderProgram));
    }

    posHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
    texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
  }

  private final FloatBuffer vertexBuffer;
  private final FloatBuffer uvBuffer;
  private final ShortBuffer drawListBuffer;
  private int textureHandle;

  public Sprite() {
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
    drawListBuffer = bb.asShortBuffer();
    drawListBuffer.put(SQUARE_INDICES);
    drawListBuffer.position(0);
  }

  public void loadTexture(Context context, String fileName) {
    final int[] textureHandleBuffer = new int[1];
    GLES20.glGenTextures(1, textureHandleBuffer, 0);
    textureHandle = textureHandleBuffer[0];

    Bitmap bmp = null;
    try {
      InputStream ins = context.getAssets().open(fileName);
      bmp = BitmapFactory.decodeStream(ins);
    } catch (IOException e) {
      return;
    }

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
  }

  public void draw() {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(shaderProgram);

    GLES20.glEnableVertexAttribArray(posHandle);
    GLES20.glEnableVertexAttribArray(texCoordHandle);
    GLES20.glVertexAttribPointer(
        posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glVertexAttribPointer(
        texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

    int handle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
    GLES20.glUniform1i(handle, 0);

    // Draw the triangle
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, SQUARE_INDICES.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(posHandle);
  }
}
