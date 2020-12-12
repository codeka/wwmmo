package au.com.codeka.warworlds.opengl;

/**
 * {@link DrawRunnable} can be added to a {@link SceneObject} and is called every frame during the
 * draw.
 */
public interface DrawRunnable {
  void run(float frameTime);
}
