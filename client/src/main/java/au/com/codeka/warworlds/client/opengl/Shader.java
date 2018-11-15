package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;

import au.com.codeka.warworlds.common.Log;

/**
 * Base class for shaders.
 */
public abstract class Shader {
  private static final Log log = new Log("Shader");
  private int program;
  private int mvpMatrixHandle;

  public void begin() {
    ensureCreated();
    GLES20.glUseProgram(program);

    onBegin();
  }

  public void end() {
    onEnd();
  }

  public int getMvpMatrixHandle() {
    return mvpMatrixHandle;
  }

  /** Called when the shader is compiled. This is a good time to save attribute locations, etc. */
  protected void onCompile() {
  }

  /** Called when we begin, the program will already be bound at this point. */
  protected void onBegin() {
  }

  /** Called when we end. The program wills till be bound. */
  protected void onEnd() {
  }

  protected abstract String getVertexShaderCode();
  protected abstract String getFragmentShaderCode();

  protected int getAttributeLocation(String name) {
    return GLES20.glGetAttribLocation(program, name);
  }

  protected int getUniformLocation(String name) {
    return GLES20.glGetUniformLocation(program, name);
  }

  private void ensureCreated() {
    if (program != 0) {
      return;
    }
    program = GLES20.glCreateProgram();

    int shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(shader, getVertexShaderCode());
    GLES20.glCompileShader(shader);
    GLES20.glAttachShader(program, shader);

    shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(shader, getFragmentShaderCode());
    GLES20.glCompileShader(shader);
    GLES20.glAttachShader(program, shader);

    GLES20.glLinkProgram(program);

    int[] linkStatus = new int[1];
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
    if (GLES20.GL_TRUE != linkStatus[0]) {
      log.error("Could not link program: %s", GLES20.glGetProgramInfoLog(program));
    }

    mvpMatrixHandle = getUniformLocation("uMvpMatrix");
    onCompile();
  }

}
