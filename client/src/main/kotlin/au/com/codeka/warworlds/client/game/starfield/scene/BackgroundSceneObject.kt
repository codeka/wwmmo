package au.com.codeka.warworlds.client.game.starfield.scene

import au.com.codeka.warworlds.client.opengl.Scene
import au.com.codeka.warworlds.client.opengl.SceneObject
import au.com.codeka.warworlds.client.opengl.Sprite
import au.com.codeka.warworlds.client.opengl.SpriteTemplate
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.SectorCoord
import java.util.*

/**
 * BackgroundSceneObject is an object that adds the gas/starfield background to make the starfield
 * look a bit nicer. It also handles switching to the "heatmap" of ownership when you zoom out.
 */
class BackgroundSceneObject(private val scene: Scene, private val sectorX: Long,
                            private val sectorY: Long)
    : SceneObject(scene.dimensionResolver, "Background:$sectorX,$sectorY", drawLayer = -10) {
  private val starfield: Sprite = scene.createSprite(
      SpriteTemplate.Builder()
          .shader(scene.spriteShader)
          .texture(scene.textureManager.loadTexture("stars/starfield.png"))
          .build(),
      "Background:$sectorX,$sectorY")
  private val gases: MutableList<Sprite>

  // This is created on-demand the first time it should be displayed.
  private var tactical: Sprite? = null
  private var zoomAmount = 0f

  init {
    gases = ArrayList()
    val rand = Random(sectorX xor sectorY * 41378L + 728247L)
    for (i in 0..39) {
      val x = rand.nextInt(4)
      val y = rand.nextInt(4)
      val gasSprite = scene.createSprite(
          SpriteTemplate.Builder()
              .shader(scene.spriteShader)
              .texture(scene.textureManager.loadTexture("stars/gas.png"))
              .uvTopLeft(Vector2((0.25f * x).toDouble(), (0.25f * y).toDouble()))
              .uvBottomRight(
                  Vector2((0.25f * x + 0.25f).toDouble(), (0.25f * y + 0.25f).toDouble()))
              .build(),
          "Gas:$x,$y")
      gasSprite.translate((rand.nextFloat() - 0.5f) * 1024.0f, (rand.nextFloat() - 0.5f) * 1024.0f)
      val size = 300.0f + rand.nextFloat() * 200.0f
      gasSprite.setSize(size, size)
      addChild(gasSprite)
      gases.add(gasSprite)
    }

    starfield.setSize(1024.0f, 1024.0f)
    addChild(starfield)

  }

  fun setZoomAmount(zoomAmount: Float) {
    this.zoomAmount = zoomAmount
    var bgAlpha = 1.0f
    if (this.zoomAmount < 0.73) {
      bgAlpha = 0.0f
    } else if (this.zoomAmount < 0.78) {
      bgAlpha = (this.zoomAmount - 0.73f) / 0.05f
    }
    starfield.alpha = bgAlpha
    for (gas in gases) {
      gas.alpha = bgAlpha
    }

    if (tactical == null && bgAlpha < 1.0f) {
      ensureTacticalObject()
    }
    tactical?.alpha = 1.0f - bgAlpha
  }

  fun ensureTacticalObject() {
    val sprite = scene.createSprite(
        SpriteTemplate.Builder()
            .shader(scene.spriteShader)
            .texture(TacticalTexture.create(SectorCoord.Builder().x(sectorX).y(sectorY).build()))
            .build(),
        "Tactical:$sectorX,$sectorY")

    sprite.setSize(1024.0f, 1024.0f)
    addChild(sprite)
    tactical = sprite
  }
}