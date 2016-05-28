package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;
import android.text.TextUtils;

import au.com.codeka.warworlds.common.Log;

/**
 * {@link SpriteShader} manages the shader program used to render a sprite.
 */
public class SpriteShader {
  private static final Log log = new Log("Sprite");

  private final String vertexShaderCode = TextUtils.join("\n", new String[] {
      "uniform mat4 uMvpMatrix;",
      "attribute vec4 aPosition;",
      "attribute vec2 aTexCoord;",
      "varying vec2 vTexCoord;",
      "void main() {",
      "  vTexCoord = aTexCoord;",
      "  gl_Position = uMvpMatrix * aPosition;",
      "}",
  });

  private final String fragmentShaderCode = TextUtils.join("\n", new String[] {
      "precision mediump float;",
      "uniform sampler2D uTexture;",
      "varying vec2 vTexCoord;",
      "void main() {",
      "  gl_FragColor = texture2D(uTexture, vTexCoord);",
      "}",
  });

  private int shaderProgram;
  private int posHandle;
  private int texCoordHandle;
  private int mvpMatrixHandle;
  private int texSamplerHandle;

  public SpriteShader() {
  }

  private void ensureCreated() {
    if (shaderProgram != 0) {
      return;
    }
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
    mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMvpMatrix");
    texSamplerHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
  }

  public void begin() {
    ensureCreated();
    GLES20.glUseProgram(shaderProgram);

    GLES20.glEnableVertexAttribArray(posHandle);
    GLES20.glEnableVertexAttribArray(texCoordHandle);
    GLES20.glUniform1i(texSamplerHandle , 0);
  }

  public void end() {
    GLES20.glDisableVertexAttribArray(posHandle);
    GLES20.glDisableVertexAttribArray(texCoordHandle);
  }

  public int getPositionHandle() {
    return posHandle;
  }

  public int getTexCoordHandle() {
    return texCoordHandle;
  }

  public int getMvpMatrixHandle() {
    return mvpMatrixHandle;
  }
}
