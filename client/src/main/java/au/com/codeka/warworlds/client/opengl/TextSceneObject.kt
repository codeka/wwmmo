package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import au.com.codeka.warworlds.client.concurrency.Threads
import com.google.common.base.Preconditions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * A [SceneObject] that represents a piece of text.
 */
class TextSceneObject(
    dimensionResolver: DimensionResolver,
    shader: SpriteShader,
    textTexture: TextTexture,
    text: String) : SceneObject(dimensionResolver) {
  private val dimensionResolver: DimensionResolver
  private val shader: SpriteShader
  private val textTexture: TextTexture
  val text: String
  private var textWidth = 0f
  private var dirty: Boolean
  private var positionBuffer: FloatBuffer? = null
  private var texCoordBuffer: FloatBuffer? = null
  private var indexBuffer: ShortBuffer? = null
  fun setTextSize(dp: Float) {
    val px = dimensionResolver.dp2px(dp)
    val scale = px / textTexture.textHeight
    Matrix.scaleM(matrix, 0, scale, scale, 1.0f)
  }

  fun getTextWidth(): Float {
    if (dirty) {
      // If it's dirty, we'll have to measure the text ourselves... we can't create the buffers
      // since that must happen on the GL thread.
      textWidth = measureText()
    }
    return dimensionResolver.px2dp(textWidth)
  }

  override fun drawImpl(mvpMatrix: FloatArray?) {
    Threads.checkOnThread(Threads.GL)
    if (dirty) {
      createBuffers()
    }
    shader.begin()
    textTexture.bind()
    GLES20.glVertexAttribPointer(
        shader.positionHandle, 3, GLES20.GL_FLOAT, false, 0, positionBuffer)
    GLES20.glVertexAttribPointer(
        shader.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
    GLES20.glUniformMatrix4fv(shader.mvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, indexBuffer!!.limit(), GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    shader.end()
  }

  private fun measureText(): Float {
    var width = 0.0f
    for (i in 0 until text.length) {
      val bounds = textTexture.getCharBounds(text[i])
      width += bounds!!.width().toFloat()
    }
    return width
  }

  private fun createBuffers() {
    // # of chars * 4 verts per char * 3 components per vert
    val positions = FloatArray(text.length * 4 * 3)

    // # of chars * 4 verts per char * 2 components per vert
    val uvs = FloatArray(text.length * 4 * 2)

    // # of chars * 2 triangles * 3 verts per triangle
    val indices = ShortArray(text.length * 2 * 3)
    var offsetX = 0f
    for (i in 0 until text.length) {
      val ch = text[i]
      val bounds = textTexture.getCharBounds(ch)
      positions[i * 12] = offsetX
      positions[i * 12 + 1] = -bounds!!.height() / 2.0f
      positions[i * 12 + 2] = 0.0f
      positions[i * 12 + 3] = offsetX + bounds.width()
      positions[i * 12 + 4] = -bounds.height() / 2.0f
      positions[i * 12 + 5] = 0.0f
      positions[i * 12 + 6] = offsetX
      positions[i * 12 + 7] = bounds.height() / 2.0f
      positions[i * 12 + 8] = 0.0f
      positions[i * 12 + 9] = offsetX + bounds.width()
      positions[i * 12 + 10] = bounds.height() / 2.0f
      positions[i * 12 + 11] = 0.0f
      offsetX += bounds.width().toFloat()
      uvs[i * 8] = bounds.left.toFloat() / TextTexture.width
      uvs[i * 8 + 1] = bounds.bottom.toFloat() / TextTexture.height
      uvs[i * 8 + 2] = bounds.right.toFloat() / TextTexture.width
      uvs[i * 8 + 3] = bounds.bottom.toFloat() / TextTexture.height
      uvs[i * 8 + 4] = bounds.left.toFloat() / TextTexture.width
      uvs[i * 8 + 5] = bounds.top.toFloat() / TextTexture.height
      uvs[i * 8 + 6] = bounds.right.toFloat() / TextTexture.width
      uvs[i * 8 + 7] = bounds.top.toFloat() / TextTexture.height
      indices[i * 6] = (i * 4).toShort()
      indices[i * 6 + 1] = (i * 4 + 1).toShort()
      indices[i * 6 + 2] = (i * 4 + 2).toShort()
      indices[i * 6 + 3] = (i * 4 + 1).toShort()
      indices[i * 6 + 4] = (i * 4 + 3).toShort()
      indices[i * 6 + 5] = (i * 4 + 2).toShort()
    }
    textWidth = offsetX
    var bb = ByteBuffer.allocateDirect(positions.size * 4)
    bb.order(ByteOrder.nativeOrder())

    val posBuffer = bb.asFloatBuffer()
    posBuffer.put(positions)
    posBuffer.position(0)
    positionBuffer = posBuffer

    bb = ByteBuffer.allocateDirect(uvs.size * 4)
    bb.order(ByteOrder.nativeOrder())
    val tcBuffer = bb.asFloatBuffer()
    tcBuffer.put(uvs)
    tcBuffer.position(0)
    texCoordBuffer = tcBuffer

    bb = ByteBuffer.allocateDirect(indices.size * 2)
    bb.order(ByteOrder.nativeOrder())
    val iBuffer = bb.asShortBuffer()
    iBuffer.put(indices)
    iBuffer.position(0)
    indexBuffer = iBuffer

    dirty = false
  }

  init {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver)
    this.shader = Preconditions.checkNotNull(shader)
    this.textTexture = Preconditions.checkNotNull(textTexture)
    this.text = text
    dirty = true
  }
}