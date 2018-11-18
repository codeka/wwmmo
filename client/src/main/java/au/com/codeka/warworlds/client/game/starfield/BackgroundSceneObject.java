package au.com.codeka.warworlds.client.game.starfield;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import au.com.codeka.warworlds.client.opengl.Scene;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.client.opengl.Sprite;
import au.com.codeka.warworlds.client.opengl.SpriteTemplate;
import au.com.codeka.warworlds.common.Vector2;

public class BackgroundSceneObject extends SceneObject {
  private final long sectorX;
  private final long sectorY;
  private final Sprite starfield;
  private final List<Sprite> gases;
  private float zoomAmount;

  public BackgroundSceneObject(Scene scene, long sectorX, long sectorY) {
    super(scene.getDimensionResolver());
    this.sectorX = sectorX;
    this.sectorY = sectorY;

    starfield = scene.createSprite(new SpriteTemplate.Builder()
        .shader(scene.getSpriteShader())
        .texture(scene.getTextureManager().loadTexture("stars/starfield.png"))
        .build());
    starfield.setSize(1024.0f, 1024.0f);
    addChild(starfield);


    gases = new ArrayList<>();
    Random rand = new Random(sectorX ^ sectorY * 41378L + 728247L);
    for (int i = 0; i < 20; i++) {
      int x = rand.nextInt(4);
      int y = rand.nextInt(4);
      Sprite gasSprite = scene.createSprite(new SpriteTemplate.Builder()
          .shader(scene.getSpriteShader())
          .texture(scene.getTextureManager().loadTexture("stars/gas.png"))
          .uvTopLeft(new Vector2(0.25f * x, 0.25f * y))
          .uvBottomRight(new Vector2(0.25f * x + 0.25f, 0.25f * y + 0.25f))
          .build());
      gasSprite.translate((rand.nextFloat() - 0.5f) * 1024.0f, (rand.nextFloat() - 0.5f) * 1024.0f);
      float size = 300.0f + rand.nextFloat() * 200.0f;
      gasSprite.setSize(size, size);
      addChild(gasSprite);
      gases.add(gasSprite);
    }
  }

  public void setZoomAmount(float zoomAmount) {
    this.zoomAmount = zoomAmount;

    float bgAlpha = 1.0f;
    if (zoomAmount < 0.73) {
      bgAlpha = 0.0f;
    } else if (zoomAmount < 0.78) {
      bgAlpha = (zoomAmount - 0.73f) / 0.05f;
    }

    starfield.setAlpha(bgAlpha);
    for (Sprite gas : gases) {
      gas.setAlpha(bgAlpha);
    }
  }

}
