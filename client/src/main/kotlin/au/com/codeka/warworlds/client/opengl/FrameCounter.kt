package au.com.codeka.warworlds.client.opengl

/** Helper class for keep track of framerate and such.  */
class FrameCounter {
  /**
   * A counter that's incremented each frame. After [.COUNT_INTERVAL_NANOS], we use the counter to
   * calculate the average framerate over that period.
   */
  private var counter = 0

  /** The time, in nanoseconds, that we last counted the framerate. */
  private var lastFrameCountTime: Long = 0

  /** Current count of frames per second (will be initially zero). */
  var framesPerSecond = 0f
    private set

  /** Called each frame to count one frame.  */
  fun onFrame() {
    counter++
    if (lastFrameCountTime == 0L) {
      lastFrameCountTime = System.nanoTime()
      return
    }
    val time = System.nanoTime()
    val dt = time - lastFrameCountTime
    if (dt >= COUNT_INTERVAL_NANOS) {
      framesPerSecond = (counter / (dt / 1000000000.0)).toFloat()
      lastFrameCountTime = time
      counter = 0
    }
  }

  companion object {
    /** When counting framerate, count the number of frames over this many nanoseconds. */
    private const val COUNT_INTERVAL_NANOS = 1 * 1000000000L
  }
}