package au.com.codeka.warworlds.client.starfield;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;
import au.com.codeka.warworlds.client.opengl.Sprite;
import au.com.codeka.warworlds.client.opengl.SpriteShader;
import au.com.codeka.warworlds.client.opengl.SpriteTemplate;
import au.com.codeka.warworlds.client.opengl.Vector2;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;

/**
 * {@link StarfieldManager} manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that {@link StarfieldFragment} cares about) and the
 * move-fleet view, etc.
 */
public class StarfieldManager {
  private final Scene scene;
  private boolean initialized;

  public StarfieldManager(RenderSurfaceView renderSurfaceView) {
    this.scene = renderSurfaceView.createScene();
    renderSurfaceView.setScene(scene);
  }

  public void create() {
    App.i.getEventBus().register(eventListener);
    if (App.i.getServer().getCurrState().getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      // If we're already connected, then call onConnected now.
      onConnected();
    }
  }

  public void destroy() {
    App.i.getEventBus().unregister(eventListener);
  }

  /**
   * This is called after we're connected to the server. If this is our first time being called,
   * we'll initialize the scene at the default viewport.
   */
  public void onConnected() {
    if (initialized) {
      return;
    }

    Sprite sprite = scene.createSprite(new SpriteTemplate.Builder()
        .shader(new SpriteShader())
        .texture(scene.getTextureManager().loadTexture("stars/stars_small.png"))
        .uvTopLeft(new Vector2(0.25f, 0.5f))
        .uvBottomRight(new Vector2(0.5f, 0.75f))
        .build());
    sprite.setSizeDp(40.0f, 40.0f);
    scene.getRootObject().addChild(sprite);
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onServerStateEvent(ServerStateEvent event) {
      if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
        onConnected();
      }
    }
  };
}
