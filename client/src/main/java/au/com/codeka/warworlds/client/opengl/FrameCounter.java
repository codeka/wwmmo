package au.com.codeka.warworlds.client.opengl;

/** Helper class for keep track of framerate and such. */
public class FrameCounter {
  /**
   * When counting framerate, count the number of frames over this many nanoseconds.
   */
  private static final long COUNT_INTERVAL_NANOS = 1 * 1000000000L;

  /**
   * A counter that's incremented each frame. After {@link #COUNT_INTERVAL_NANOS}, we use the
   * counter to calculate the average framerate over that period.
   */
  private int counter;

  /**
   * The time, in nanoseconds, that we last counted the framerate.
   */
  private long lastFrameCountTime;

  /**
   * Current count of frames per second (will be initially zero).
   */
  private float framesPerSecond;

  /** Called each frame to count one frame. */
  public void onFrame() {
    counter++;

    if (lastFrameCountTime == 0) {
      lastFrameCountTime = System.nanoTime();
      return;
    }

    long time = System.nanoTime();
    long dt = time - lastFrameCountTime;
    if (dt >= COUNT_INTERVAL_NANOS) {
      framesPerSecond = (float)(counter / (dt / 1000000000.0));
      lastFrameCountTime = time;
      counter = 0;
    }
  }

  public float getFramesPerSecond() {
    return framesPerSecond;
  }
}
