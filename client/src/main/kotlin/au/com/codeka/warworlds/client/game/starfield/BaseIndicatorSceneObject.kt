package au.com.codeka.warworlds.client.game.starfield

import android.opengl.GLES20
import android.text.TextUtils
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.concurrency.Threads.Companion.checkOnThread
import au.com.codeka.warworlds.client.opengl.DimensionResolver
import au.com.codeka.warworlds.client.opengl.SceneObject
import au.com.codeka.warworlds.client.opengl.Shader
import au.com.codeka.warworlds.common.Colour
import au.com.codeka.warworlds.common.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Base class for [SceneObject]s that we want to use to "indicate" something (radar radius,
 * selection, etc). Basically it draws a solid circle and a bit of shading in the middle.
 */
open class BaseIndicatorSceneObject(
    dimensionResolver: DimensionResolver, debugName: String, colour: Colour, lineThickness: Float)
    : SceneObject(dimensionResolver, debugName) {
  private val shader: IndicatorShader
  private val positionBuffer: FloatBuffer
  private val texCoordBuffer: FloatBuffer
  private val indexBuffer: ShortBuffer
  override fun drawImpl(mvpMatrix: FloatArray?) {
    checkOnThread(Threads.GL)
    shader.begin()
    GLES20.glVertexAttribPointer(
        shader.posHandle, 3, GLES20.GL_FLOAT, false, 0, positionBuffer)
    GLES20.glVertexAttribPointer(
        shader.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
    GLES20.glUniformMatrix4fv(shader.mvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, SQUARE_INDICES.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    shader.end()
  }

  /** [Shader] for the indicator entity.  */
  private class IndicatorShader(colour: Colour, private val lineThickness: Float) : Shader() {
    private val color: FloatArray
    var posHandle = 0
      private set
    var texCoordHandle = 0
      private set
    private var colourHandle = 0
    private var lineThicknessHandle = 0

    override val vertexShaderCode: String
      protected get() = TextUtils.join("\n", arrayOf(
          "uniform mat4 uMvpMatrix;",
          "attribute vec4 aPosition;",
          "attribute vec2 aTexCoord;",
          "varying vec2 vTexCoord;",
          "void main() {",
          "  vTexCoord = aTexCoord;",
          "  gl_Position = uMvpMatrix * aPosition;",
          "}"))

    override val fragmentShaderCode: String
      protected get() = TextUtils.join("\n", arrayOf(
          "precision mediump float;",
          "varying vec2 vTexCoord;",
          "uniform vec3 uColour;",
          "uniform float uLineThickness;",
          "void main() {",
          "   float dist = distance(vTexCoord, vec2(0.5, 0.5));",
          "   if (dist > 0.5) {",
          "     dist = 0.0;",
          "   } else if (dist > 0.5 - (uLineThickness * 0.005)) {",
          "     dist = 0.66;",
          "   } else {",
          "     dist *= 0.3;",
          "   }",
          "   gl_FragColor = vec4(uColour, dist);",
          "}"))

    override fun onCompile() {
      posHandle = getAttributeLocation("aPosition")
      texCoordHandle = getAttributeLocation("aTexCoord")
      colourHandle = getUniformLocation("uColour")
      lineThicknessHandle = getUniformLocation("uLineThickness")
    }

    override fun onBegin() {
      GLES20.glEnableVertexAttribArray(posHandle)
      GLES20.glEnableVertexAttribArray(texCoordHandle)
      GLES20.glUniform3fv(colourHandle, 1, color, 0)
      GLES20.glUniform1f(lineThicknessHandle, lineThickness)
    }

    override fun onEnd() {
      GLES20.glDisableVertexAttribArray(posHandle)
      GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    init {
      color = floatArrayOf(
          colour.r.toFloat(),
          colour.g.toFloat(),
          colour.b.toFloat()
      )
    }
  }

  companion object {
    private val log = Log("BISO")
    private val SQUARE_COORDS = floatArrayOf(
        -0.5f, 0.5f, 0.0f,  // top left
        -0.5f, -0.5f, 0.0f,  // bottom left
        0.5f, -0.5f, 0.0f,  // bottom right
        0.5f, 0.5f, 0.0f)
    private val SQUARE_INDICES = shortArrayOf(0, 1, 2, 0, 2, 3)
  }

  init {
    shader = IndicatorShader(colour, lineThickness)

    // initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    var bb = ByteBuffer.allocateDirect(SQUARE_COORDS.size * 4)
    bb.order(ByteOrder.nativeOrder())
    positionBuffer = bb.asFloatBuffer()
    positionBuffer.put(SQUARE_COORDS)
    positionBuffer.position(0)
    val uvs = floatArrayOf(
        0.0f, 0.0f,  // top left
        0.0f, 1.0f,  // bottom left
        1.0f, 1.0f,  // bottom right
        1.0f, 0.0f)
    bb = ByteBuffer.allocateDirect(uvs.size * 4)
    bb.order(ByteOrder.nativeOrder())
    texCoordBuffer = bb.asFloatBuffer()
    texCoordBuffer.put(uvs)
    texCoordBuffer.position(0)

    // initialize byte buffer for the draw list
    // (# of coordinate values * 2 bytes per short)
    bb = ByteBuffer.allocateDirect(SQUARE_INDICES.size * 2)
    bb.order(ByteOrder.nativeOrder())
    indexBuffer = bb.asShortBuffer()
    indexBuffer.put(SQUARE_INDICES)
    indexBuffer.position(0)
  }
}