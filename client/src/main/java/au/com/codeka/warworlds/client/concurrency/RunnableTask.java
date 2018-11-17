package au.com.codeka.warworlds.client.concurrency;

import au.com.codeka.warworlds.common.Log;

/**
 * A {@link Task} that encapsulates a {@link Runnable}, {@link RunnableP}, {@link RunnableR} or
 * {@link RunnablePR} that you want to run on a particular thread.
 *
 * @param <P> The parameter type. Depending on the type of runnable you're using, this may or may
 *           not be ignored.
 * @param <R> The result type. Depending on the type of runnable you're using, this may or may
 *           not be ignored.
 */
public class RunnableTask<P, R> extends Task<P, R> {
  private static final Log log = new Log("RunnableTask");

  /** A runnable that takes a parameter. */
  public interface RunnableP<P> {
    void run(P param);
  }

  /** A runnable that returns a value. */
  public interface RunnableR<R> {
    R run();
  }

  /** A runnable that takes a parameter and returns a value. */
  public interface RunnablePR<P, R> {
    R run(P param);
  }

  private final Runnable runnable;
  private final RunnableP<P> runnableP;
  private final RunnableR<R> runnableR;
  private final RunnablePR<P, R> runnablePR;
  private final Threads thread;

  public RunnableTask(TaskRunner taskRunner, Runnable runnable, Threads thread) {
    super(taskRunner);
    this.runnable = runnable;
    this.runnableP = null;
    this.runnableR = null;
    this.runnablePR = null;
    this.thread = thread;
  }

  public RunnableTask(TaskRunner taskRunner, RunnableP<P> runnable, Threads thread) {
    super(taskRunner);
    this.runnable = null;
    this.runnableP = runnable;
    this.runnableR = null;
    this.runnablePR = null;
    this.thread = thread;
  }

  public RunnableTask(TaskRunner taskRunner, RunnableR<R> runnable, Threads thread) {
    super(taskRunner);
    this.runnable = null;
    this.runnableP = null;
    this.runnableR = runnable;
    this.runnablePR = null;
    this.thread = thread;
  }

  public RunnableTask(TaskRunner taskRunner, RunnablePR<P, R> runnable, Threads thread) {
    super(taskRunner);
    this.runnable = null;
    this.runnableP = null;
    this.runnableR = null;
    this.runnablePR = runnable;
    this.thread = thread;
  }

  @Override
  public void run(P param) {
    thread.run(() -> {
      try {
        R result = null;
        if (runnable != null) {
          runnable.run();
        } else if (runnableP != null) {
          runnableP.run(param);
        } else if (runnableR != null) {
          result = runnableR.run();
        } else if (runnablePR != null) {
          result = runnablePR.run(param);
        }
        onComplete(result);
      } catch (Exception e) {
        log.error("Unexpected.", e);
        onError(e);
      }
    });
  }
}
