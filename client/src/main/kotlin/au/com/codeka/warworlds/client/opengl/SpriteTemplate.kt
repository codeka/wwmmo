package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Vector2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * A [SpriteTemplate] describes a sprite's characteristics (texture, bounds within the texture
 * and so on). You would use it to add an actual [Sprite] to a [Scene].
 */
class SpriteTemplate(
    private val shader: SpriteShader, private val texture: BitmapTexture, uvTopLeft: Vector2,
    uvBottomRight: Vector2) {
  private val positionBuffer: FloatBuffer
  private val texCoordBuffer: FloatBuffer
  private val indexBuffer: ShortBuffer

  init {
    // initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    var bb = ByteBuffer.allocateDirect(SQUARE_COORDS.size * 4)
    bb.order(ByteOrder.nativeOrder())
    positionBuffer = bb.asFloatBuffer()
    positionBuffer.put(SQUARE_COORDS)
    positionBuffer.position(0)
    val uvs = floatArrayOf(
        uvTopLeft.x.toFloat(), uvTopLeft.y.toFloat(),  // top left
        uvTopLeft.x.toFloat(), uvBottomRight.y.toFloat(),  // bottom left
        uvBottomRight.x.toFloat(), uvBottomRight.y.toFloat(),  // bottom right
        uvBottomRight.x.toFloat(), uvTopLeft.y.toFloat())
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

  fun draw(mvpMatrix: FloatArray?, alpha: Float = 1.0f) {
    Threads.checkOnThread(Threads.GL)
    shader.begin()
    texture.bind()
    GLES20.glVertexAttribPointer(
        shader.positionHandle, 3, GLES20.GL_FLOAT, false, 0, positionBuffer)
    GLES20.glVertexAttribPointer(
        shader.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
    GLES20.glUniform1f(shader.alphaHandle, alpha)
    GLES20.glUniformMatrix4fv(shader.mvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES, SQUARE_INDICES.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    shader.end()
  }

  class Builder {
    private var shader: SpriteShader? = null
    private var texture: BitmapTexture? = null
    private var uvTopLeft: Vector2? = null
    private var uvBottomRight: Vector2? = null
    fun shader(shader: SpriteShader?): Builder {
      this.shader = shader
      return this
    }

    fun texture(texture: BitmapTexture?): Builder {
      this.texture = texture
      return this
    }

    fun uvTopLeft(uv: Vector2?): Builder {
      uvTopLeft = uv
      return this
    }

    fun uvBottomRight(uv: Vector2?): Builder {
      uvBottomRight = uv
      return this
    }

    fun build(): SpriteTemplate {
      if (uvTopLeft == null) {
        uvTopLeft = Vector2(0.0, 0.0)
      }
      if (uvBottomRight == null) {
        uvBottomRight = Vector2(1.0, 1.0)
      }

      return SpriteTemplate(shader!!, texture!!, uvTopLeft!!, uvBottomRight!!)
    }
  }

  companion object {
    private val SQUARE_COORDS = floatArrayOf(
        -0.5f, 0.5f, 0.0f,  // top left
        -0.5f, -0.5f, 0.0f,  // bottom left
        0.5f, -0.5f, 0.0f,  // bottom right
        0.5f, 0.5f, 0.0f)
    private val SQUARE_INDICES = shortArrayOf(0, 1, 2, 0, 2, 3)
  }

}