package au.com.codeka.warworlds.client.starfield;

import com.google.common.base.Preconditions;

import java.util.Stack;

import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;

/**
 * {@link StarfieldManager} manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that {@link StarfieldFragment} cares about) and the
 * move-fleet view, etc.
 */
public class StarfieldManager {
  private final RenderSurfaceView renderSurfaceView;
  private final Stack<Scene> sceneStack = new Stack<>();

  public StarfieldManager(RenderSurfaceView renderSurfaceView) {
    this.renderSurfaceView = Preconditions.checkNotNull(renderSurfaceView);
  }

  public StarfieldSceneBuilder sceneBuilder() {
    return new StarfieldSceneBuilder(renderSurfaceView);
  }

  public void pushScene(Scene scene) {
    Threads.checkOnThread(Threads.UI_THREAD);

    sceneStack.push(scene);
    updateRendererScene();
  }

  public void popScene() {
    Threads.checkOnThread(Threads.UI_THREAD);

    sceneStack.pop();
    updateRendererScene();
  }

  /** Update the renderer with the current scene. */
  private void updateRendererScene() {
    if (sceneStack.isEmpty()) {
      renderSurfaceView.setScene(null);
    } else {
      renderSurfaceView.setScene(sceneStack.peek());
    }
  }
}
