package au.com.codeka.warworlds.client.concurrency;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * An enumeration of the thread types within War Worlds. Has some helper methods to ensure you run
 * on a particular thread.
 */
public enum Threads {
  UI_THREAD(true),

  /**
   * The OpenGL render thread. We assume there is only one of these in the whole process.
   */
  GL_THREAD(false);

  public static void checkOnThread(Threads thread) {
    Preconditions.checkState(thread.isCurrentThread(), "Unexpectedly not on " + thread);
  }

  public static void checkNotOnThread(Threads thread) {
    Preconditions.checkState(thread.isCurrentThread(), "Unexpectedly on " + thread);
  }

  private boolean isUiThread;
  @Nullable private Thread thread;

  Threads(boolean isUiThread) {
    this.isUiThread = isUiThread;
  }

  /** Set the thread we represent. */
  public void setThread(@NonNull Thread thread) {
    Preconditions.checkState(!isUiThread);
    Preconditions.checkState(this.thread == null);
    this.thread = thread;
  }

  public boolean isCurrentThread() {
    if (isUiThread) {
      return Looper.getMainLooper().getThread() == Thread.currentThread();
    } else if (this.thread != null) {
      return this.thread == Thread.currentThread();
    } else {
      // TODO: what else could it be?
      return false;
    }
  }
}
