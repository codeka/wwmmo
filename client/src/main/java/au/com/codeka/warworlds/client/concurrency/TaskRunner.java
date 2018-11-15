package au.com.codeka.warworlds.client.concurrency;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This is a class for running tasks on various threads. You can run a task on any thread defined
 * in {@link Threads}.
 */
public class TaskRunner {

  private Timer timer;

  public TaskRunner() {
    ThreadPool backgroundThreadPool = new ThreadPool(
        Threads.BACKGROUND,
        750 /* maxQueuedItems */,
        5 /* minThreads */,
        20 /* maxThreads */,
        1000 /* keepAliveMs */);
    Threads.BACKGROUND.setThreadPool(backgroundThreadPool);

    timer = new Timer("Timer");
  }

  /**
   * Run the given {@link Runnable} on the given {@link Threads}.
   *
   * @return A {@link Task} that you can use to chain further tasks after this one has finished.
   */
  public Task runTask(Runnable runnable, Threads thread) {
    return runTask(new RunnableTask<Void, Void>(this, runnable, thread), null);
  }

  /**
   * Run the given {@link RunnableTask.RunnableP} on the given {@link Threads}.
   *
   * @return A {@link Task} that you can use to chain further tasks after this one has finished.
   */
  public <P> Task runTask(RunnableTask.RunnableP<P> runnable, Threads thread) {
    return runTask(new RunnableTask<P, Void>(this, runnable, thread), null);
  }

  /**
   * Run the given {@link RunnableTask.RunnableR} on the given {@link Threads}.
   *
   * @return A {@link Task} that you can use to chain further tasks after this one has finished.
   */
  public <R> Task runTask(RunnableTask.RunnableR<R> runnable, Threads thread) {
    return runTask(new RunnableTask<Void, R>(this, runnable, thread), null);
  }

  /**
   * Run the given {@link RunnableTask.RunnablePR} on the given {@link Threads}.
   *
   * @return A {@link Task} that you can use to chain further tasks after this one has finished.
   */
  public <P, R> Task runTask(RunnableTask.RunnablePR<P, R> runnable, Threads thread) {
    return runTask(new RunnableTask<>(this, runnable, thread), null);
  }

  public <P> Task runTask(Task<P, ?> task, P param) {
    task.run(param);
    return task;
  }

  /**
   * Runs the given GmsCore {@link com.google.android.gms.tasks.Task}, and returns a {@link Task}
   * that you can then use to chain other tasks, etc.
   *
   * @param gmsTask The GmsCore task to run.
   * @param <R> The type of result to expect from the GmsCore task.
   * @return A {@link Task} that you can use to chain callbacks.
   */
  public <R> Task<Void, R> runTask(com.google.android.gms.tasks.Task<R> gmsTask) {
    return new GmsTask<>(this, gmsTask);
  }

  /** Run a task after the given delay. */
  public void runTask(final Runnable runnable, final Threads thread, long delayMs) {
    if (delayMs == 0) {
      runTask(runnable, thread);
    } else {
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          runTask(runnable, thread);
        }
      }, delayMs);
    }
  }
}
