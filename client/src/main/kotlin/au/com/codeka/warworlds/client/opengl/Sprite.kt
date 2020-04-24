package au.com.codeka.warworlds.client.opengl

import com.google.common.base.Preconditions

/** A [Sprite] is basically a quad + texture.  */
class Sprite(dimensionResolver: DimensionResolver?, tmpl: SpriteTemplate) : SceneObject(dimensionResolver!!) {
  private val tmpl: SpriteTemplate
  var alpha = 1.0f

  override fun drawImpl(mvpMatrix: FloatArray?) {
    tmpl.draw(mvpMatrix, alpha)
  }

  init {
    this.tmpl = Preconditions.checkNotNull(tmpl)
  }
}