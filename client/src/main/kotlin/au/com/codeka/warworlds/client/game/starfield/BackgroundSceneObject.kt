package au.com.codeka.warworlds.client.game.starfield

import au.com.codeka.warworlds.client.opengl.Scene
import au.com.codeka.warworlds.client.opengl.SceneObject
import au.com.codeka.warworlds.client.opengl.Sprite
import au.com.codeka.warworlds.client.opengl.SpriteTemplate
import au.com.codeka.warworlds.common.Vector2
import java.util.*

class BackgroundSceneObject(scene: Scene, private val sectorX: Long, private val sectorY: Long)
    : SceneObject(scene.dimensionResolver, "Background:$sectorX,$sectorY") {
  private val starfield: Sprite = scene.createSprite(SpriteTemplate.Builder()
      .shader(scene.spriteShader)
      .texture(scene.textureManager.loadTexture("stars/starfield.png"))
      .build(), "Background:$sectorX,$sectorY")
  private val gases: MutableList<Sprite>
  private var zoomAmount = 0f

  init {
    starfield.setSize(1024.0f, 1024.0f)
    addChild(starfield)
    gases = ArrayList()
    val rand = Random(sectorX xor sectorY * 41378L + 728247L)
    for (i in 0..19) {
      val x = rand.nextInt(4)
      val y = rand.nextInt(4)
      val gasSprite = scene.createSprite(SpriteTemplate.Builder()
          .shader(scene.spriteShader)
          .texture(scene.textureManager.loadTexture("stars/gas.png"))
          .uvTopLeft(Vector2((0.25f * x).toDouble(), (0.25f * y).toDouble()))
          .uvBottomRight(Vector2((0.25f * x + 0.25f).toDouble(), (0.25f * y + 0.25f).toDouble()))
          .build(), "Gas:$x,$y")
      gasSprite.translate((rand.nextFloat() - 0.5f) * 1024.0f, (rand.nextFloat() - 0.5f) * 1024.0f)
      val size = 300.0f + rand.nextFloat() * 200.0f
      gasSprite.setSize(size, size)
      addChild(gasSprite)
      gases.add(gasSprite)
    }
  }

  fun setZoomAmount(zoomAmount: Float) {
    this.zoomAmount = zoomAmount
    var bgAlpha = 1.0f
    if (zoomAmount < 0.73) {
      bgAlpha = 0.0f
    } else if (zoomAmount < 0.78) {
      bgAlpha = (zoomAmount - 0.73f) / 0.05f
    }
    starfield.alpha = bgAlpha
    for (gas in gases) {
      gas.alpha = bgAlpha
    }
  }
}