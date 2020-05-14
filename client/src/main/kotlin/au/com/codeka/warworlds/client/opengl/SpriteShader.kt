package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import android.text.TextUtils
import au.com.codeka.warworlds.common.Log

/**
 * [SpriteShader] manages the shader program used to render a sprite.
 */
class SpriteShader : Shader() {
  var positionHandle = 0
    private set
  var texCoordHandle = 0
    private set
  private var texSamplerHandle = 0
  var alphaHandle = 0
    private set

  override val vertexShaderCode: String?
    get() = TextUtils.join("\n", arrayOf(
        "uniform mat4 uMvpMatrix;",
        "attribute vec4 aPosition;",
        "attribute vec2 aTexCoord;",
        "varying vec2 vTexCoord;",
        "void main() {",
        "  vTexCoord = aTexCoord;",
        "  gl_Position = uMvpMatrix * aPosition;",
        "}"))

  override val fragmentShaderCode: String?
    get() = TextUtils.join("\n", arrayOf(
        "precision mediump float;",
        "uniform sampler2D uTexture;",
        "uniform float uAlpha;",
        "varying vec2 vTexCoord;",
        "void main() {",
        "  vec4 color = texture2D(uTexture, vTexCoord);",
        //"  color.rgb = vec3(1, 1, 1);",
        "  color.a = color.a * uAlpha;",
        "  gl_FragColor = color;",
        "}"))

  override fun onCompile() {
    positionHandle = getAttributeLocation("aPosition")
    texCoordHandle = getAttributeLocation("aTexCoord")
    texSamplerHandle = getUniformLocation("uTexture")
    alphaHandle = getUniformLocation("uAlpha")
  }

  override fun onBegin() {
    GLES20.glEnableVertexAttribArray(positionHandle)
    GLES20.glEnableVertexAttribArray(texCoordHandle)
    GLES20.glUniform1i(texSamplerHandle, 0)
  }

  override fun onEnd() {
    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(texCoordHandle)
  }

  companion object {
    private val log = Log("SpriteShader")
  }
}