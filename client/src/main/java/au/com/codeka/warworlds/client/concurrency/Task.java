package au.com.codeka.warworlds.client.concurrency;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a "task", which allows chaining of following tasks (via {@link #then}) and handling
 * of errors (via {@link #error}).
 *
 * @param <P> the type of the input parameter to this task. Can be Void if you want nothing.
 * @param <R> the type of the return value from the task. Can be Void if you want nothing.
 */
public class Task<P, R> {
  private final TaskRunner taskRunner;
  private List<Task<R, ?>> thenTasks;
  private List<Task<Exception, Void>> errorTasks;
  private final Object lock = new Object();
  private boolean finished;
  private R result;
  private Exception error;

  Task(TaskRunner taskRunner) {
    this.taskRunner = taskRunner;
  }

  void run(P param) {
  }

  protected void onComplete(R result) {
    synchronized (lock) {
      finished = true;
      this.result = result;
      if (thenTasks != null) {
        for (Task<R, ?> task : thenTasks) {
          taskRunner.runTask(task, result);
        }
        thenTasks = null;
      }
    }
  }

  protected void onError(Exception error) {
    synchronized (lock) {
      finished = true;
      this.error = error;

      if (errorTasks != null) {
        for (Task<Exception, Void> task : errorTasks) {
          taskRunner.runTask(task, null);
        }
        errorTasks = null;
      }
    }
  }

  /**
   * Queues up the given {@link Task} to run after this task. It will be handed this task's result
   * as it's parameter.
   * @param task The task to queue after this task completes.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   *     after the other.
   */
  public <TR> Task<R, TR> then(Task<R, TR> task) {
    synchronized (lock) {
      if (finished) {
        if (error == null) {
          taskRunner.runTask(task, result);
        }
        return task;
      }
      if (this.thenTasks == null) {
        this.thenTasks = new ArrayList<>();
      }
      this.thenTasks.add(task);
    }

    return task;
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The {@link Threads} on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   *     after the other.
   */
  public Task<R, Void> then(Runnable runnable, Threads thread) {
    return then(new RunnableTask<R, Void>(taskRunner, runnable, thread));
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The {@link Threads} on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   *     after the other.
   */
  public Task<R, Void> then(RunnableTask.RunnableP<R> runnable, Threads thread) {
    return then(new RunnableTask<R, Void>(taskRunner, runnable, thread));
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The {@link Threads} on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   *     after the other.
   */
  public <RR> Task<R, RR> then(RunnableTask.RunnableR<RR> runnable, Threads thread) {
    return then(new RunnableTask<>(taskRunner, runnable, thread));
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The {@link Threads} on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   *     after the other.
   */
  public <RR> Task<R, RR> then(RunnableTask.RunnablePR<R, RR> runnable, Threads thread) {
    return then(new RunnableTask<>(taskRunner, runnable, thread));
  }

  /**
   * Queue a task to run in case there's an exception running the current task.
   *
   * @param task The task to run if there's an error.
   * @return The current task, so you can queue up calls like task.error().then() to handle both
   *     the error case and the 'next' case.
   */
  public Task<P, R> error(Task<Exception, Void> task) {
    synchronized (lock) {
      if (finished) {
        if (error != null) {
          taskRunner.runTask(task, error);
        }
        return this;
      }
      if (this.errorTasks == null) {
        this.errorTasks = new ArrayList<>();
      }
      this.errorTasks.add(task);
    }
    return this;
  }

  public Task<P, R> error(Runnable runnable, Threads thread) {
    return error(new RunnableTask<>(taskRunner, runnable, thread));
  }

  public Task<P, R> error(RunnableTask.RunnableP<Exception> runnable, Threads thread) {
    return error(new RunnableTask<>(taskRunner, runnable, thread));
  }
}
