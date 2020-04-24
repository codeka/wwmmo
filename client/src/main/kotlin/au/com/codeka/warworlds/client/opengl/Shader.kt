package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import au.com.codeka.warworlds.common.Log

/**
 * Base class for shaders.
 */
abstract class Shader {
  private var program = 0
  var mvpMatrixHandle = 0
    private set

  fun begin() {
    ensureCreated()
    GLES20.glUseProgram(program)
    onBegin()
  }

  fun end() {
    onEnd()
  }

  /** Called when the shader is compiled. This is a good time to save attribute locations, etc.  */
  protected open fun onCompile() {}

  /** Called when we begin, the program will already be bound at this point.  */
  protected open fun onBegin() {}

  /** Called when we end. The program wills till be bound.  */
  protected open fun onEnd() {}
  protected abstract val vertexShaderCode: String?
  protected abstract val fragmentShaderCode: String?
  protected fun getAttributeLocation(name: String?): Int {
    return GLES20.glGetAttribLocation(program, name)
  }

  protected fun getUniformLocation(name: String?): Int {
    return GLES20.glGetUniformLocation(program, name)
  }

  private fun ensureCreated() {
    if (program != 0) {
      return
    }
    program = GLES20.glCreateProgram()
    var shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
    GLES20.glShaderSource(shader, vertexShaderCode)
    GLES20.glCompileShader(shader)
    GLES20.glAttachShader(program, shader)
    shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
    GLES20.glShaderSource(shader, fragmentShaderCode)
    GLES20.glCompileShader(shader)
    GLES20.glAttachShader(program, shader)
    GLES20.glLinkProgram(program)
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (GLES20.GL_TRUE != linkStatus[0]) {
      log.error("Could not link program: %s", GLES20.glGetProgramInfoLog(program))
    }
    mvpMatrixHandle = getUniformLocation("uMvpMatrix")
    onCompile()
  }

  companion object {
    private val log = Log("Shader")
  }
}